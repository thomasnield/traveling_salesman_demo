import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.util.concurrent.Callable
import kotlin.math.exp


val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

var defaultSpeed = 200.millis
var speed = defaultSpeed
var defaultAnimationOn = true

data class Point(val x: Double, val y: Double)

fun ccw(a: Point, b: Point, c: Point) =
        (c.y - a.y) * (b.x - a.x) > (b.y - a.y) * (c.x - a.x)

fun intersect(a: Point, b: Point, c: Point, d: Point) =
        ccw(a,c,d) != ccw(b,c,d) && ccw(a,b,c) != ccw(a,b,d)


class Edge(city: City) {

    val startCityProperty = SimpleObjectProperty(city)
    var startCity by startCityProperty
    val startPoint get() = startCity.let { Point(it.x,it.y) }

    val endCityProperty = SimpleObjectProperty(city)
    var endCity by endCityProperty
    val endPoint get() = endCity.let { Point(it.x, it.y) }

    // animated properties
    val edgeStartX = SimpleDoubleProperty(startCityProperty.get().x)
    val edgeStartY = SimpleDoubleProperty(startCityProperty.get().y)
    val edgeEndX = SimpleDoubleProperty(startCityProperty.get().x)
    val edgeEndY = SimpleDoubleProperty(startCityProperty.get().y)
    val distance = SimpleDoubleProperty(0.0)

    val distanceNow get() = CitiesAndDistances.distances[CityPair(startCity.id, endCity.id)]?:0.0


    init {
        startCityProperty.onChange {
            if (defaultAnimationOn)
                sequentialTransition += timeline(play = false) {
                    keyframe(speed) {
                        keyvalue(edgeStartX, it?.x ?: 0.0)
                        keyvalue(edgeStartY, it?.y ?: 0.0)
                        keyvalue(distance, distanceNow)
                    }
                }
        }
        endCityProperty.onChange {
            if (defaultAnimationOn)
                sequentialTransition += timeline(play = false) {
                    keyframe(speed) {
                        keyvalue(edgeEndX, it?.x ?: 0.0)
                        keyvalue(edgeEndY, it?.y ?: 0.0)
                        keyvalue(distance, distanceNow)
                    }
                }
        }
    }

    fun animateChange() {
        sequentialTransition += timeline(play = false) {
            keyframe(speed) {
                keyvalue(edgeStartX, startCity?.x ?: 0.0)
                keyvalue(edgeStartY, startCity?.y ?: 0.0)
                keyvalue(edgeEndX, endCity?.x ?: 0.0)
                keyvalue(edgeEndY, endCity?.y ?: 0.0)
                keyvalue(distance, distanceNow)
            }
        }
    }

    val nextEdge get() = (Model.edges.firstOrNull { it != this && it.startCity == endCity }) ?:
        (Model.edges.firstOrNull { it != this && it.endCity == endCity }?.also { it.flip() })

    private fun flip() {
        val city1 = startCity
        val city2 = endCity
        startCity = city2
        endCity = city1
    }

    val intersectConflicts get() = Model.edges.asSequence()
            .filter { it != this }
            .filter { edge2 ->
                startCity !in edge2.let { setOf(it.startCity, it.endCity) } &&
                        endCity !in edge2.let { setOf(it.startCity, it.endCity) } &&
                    intersect(startPoint, endPoint, edge2.startPoint, edge2.endPoint)
            }


    class TwoSwap(val city1: City,
                  val city2: City,
                  val edge1: Edge,
                  val edge2: Edge
    ) {

        fun execute() {
            edge1.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city1 }.set(city2)
            edge2.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city2 }.set(city1)
        }
        fun reverse() {
            edge1.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city2 }.set(city1)
            edge2.let { sequenceOf(it.startCityProperty, it.endCityProperty) }.first { it.get() == city1 }.set(city2)
        }


        fun animate() {
            sequentialTransition += timeline(play = false) {
                keyframe(speed) {
                    sequenceOf(edge1,edge2).forEach {
                        keyvalue(it.edgeStartX, it.startCity?.x ?: 0.0)
                        keyvalue(it.edgeStartY, it.startCity?.y ?: 0.0)
                        keyvalue(it.edgeEndX, it.endCity?.x ?: 0.0)
                        keyvalue(it.edgeEndY, it.endCity?.y ?: 0.0)
                    }
                }
                keyframe(1.millis) {
                    sequenceOf(edge1,edge2).forEach {
                        keyvalue(it.distance, it.distanceNow)
                    }
                }
            }
        }

        override fun toString() = "$city1-$city2 ($edge1)-($edge2)"
    }
    fun attemptTwoSwap(otherEdge: Edge): TwoSwap? {

        val e1 = this
        val e2 = otherEdge

        val startCity1 = startCity
        val endCity1 = endCity
        val startCity2 = otherEdge.startCity
        val endCity2 = otherEdge.endCity

        return sequenceOf(
                TwoSwap(startCity1, startCity2, e1, e2),
                TwoSwap(endCity1, endCity2, e1, e2),

                TwoSwap(startCity1, endCity2, e1, e2),
                TwoSwap(endCity1, startCity2, e1, e2)

        ).filter {
            it.edge1.startCity !in it.edge2.let { setOf(it.startCity, it.endCity) } &&
                    it.edge1.endCity !in it.edge2.let { setOf(it.startCity, it.endCity) }
        }
        .firstOrNull { swap ->
            swap.execute()
            val result = Model.tourMaintained
            if (!result) {
                swap.reverse()
            }
            result
        }
    }

    override fun toString() = "$startCity-$endCity"
}
object Model {


    val edges = CitiesAndDistances.cities.asSequence()
            .map { Edge(it) }
            .toList()

    val animationDistanceProperty = Bindings.createDoubleBinding(
            Callable<Double> { edges.asSequence().map { it.distance.get() }.sum() },
            *edges.map { it.distance }.toTypedArray()
    )

    val totalDistance get() = Model.edges.map { it.distanceNow }.sum()

    val traverseTour: Sequence<Edge> get() {
        val captured = mutableSetOf<Edge>()

        return generateSequence(edges.first()) {
            it.nextEdge?.takeIf { it !in captured }
        }.onEach { captured += it }
    }

    val tourMaintained get() = traverseTour.count() == edges.count()

    val intersectConflicts get() = edges.asSequence()
            .map { edge1 -> edge1.intersectConflicts.map { edge2 -> edge1 to edge2}.sampleOrNull() }
            .filterNotNull()


    fun toConfiguration() = traverseTour.map { it.startCity to it.endCity }.toList().toTypedArray()
    fun applyConfiguration(configuration: Array<Pair<City,City>>) = edges.zip(configuration).forEach { (e,c) ->
        e.startCity = c.first
        e.endCity = c.second
    }
    val heatProperty = SimpleDoubleProperty(0.0)
    val heat by heatProperty

    fun reset() {
        speed = 100.millis

        CitiesAndDistances.cities.zip(edges).forEach { (c,e) ->
            e.startCity = c
            e.endCity = c
            e.animateChange()
        }

        speed = defaultSpeed
    }
}
enum class SearchStrategy {

    RANDOM {
        override fun execute() {
            val capturedCities = mutableSetOf<Int>()

            val startingEdge = Model.edges.sample()
            var edge = startingEdge

            while(capturedCities.size < CitiesAndDistances.cities.size) {
                capturedCities += edge.startCity.id

                val nextRandom = Model.edges.asSequence()
                        .filter { it.startCity.id !in capturedCities }
                        .sampleOrNull()?:startingEdge

                edge.endCity = nextRandom.startCity
                edge = nextRandom
            }

            if (!Model.tourMaintained) throw Exception("Tour broken in RANDOM SearchStrategy \r\n${Model.edges.joinToString("\r\n")}")
        }
    },

    GREEDY {
        override fun execute() {
            val capturedCities = mutableSetOf<Int>()

            var edge = Model.edges.first()

            while(capturedCities.size < CitiesAndDistances.cities.size) {
                capturedCities += edge.startCity.id

                val closest = Model.edges.asSequence().filter { it.startCity.id !in capturedCities }
                        .minBy { CitiesAndDistances.distances[CityPair(edge.startCity.id, it.startCity.id)]?:10000.0 }?:Model.edges.first()

                edge.endCity = closest.startCity
                edge = closest
            }
            if (!Model.tourMaintained) throw Exception("Tour broken in GREEDY SearchStrategy \r\n${Model.edges.joinToString("\r\n")}")
        }
    },

    REMOVE_OVERLAPS {
        override fun execute() {

            SearchStrategy.RANDOM.execute()
            defaultAnimationOn = false

            (1..10).forEach {
                Model.intersectConflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)?.animate()
                }
            }

        }
    },
    TWO_OPT {
        override fun execute() {

            SearchStrategy.RANDOM.execute()
            defaultAnimationOn = false

            (1..2000).forEach { iteration ->
                Model.edges.sampleDistinct(2).toList()
                        .let { it.first() to it.last() }
                        .also { (e1,e2) ->

                            val oldDistance = Model.totalDistance
                            e1.attemptTwoSwap(e2)?.also {
                                when {
                                    oldDistance <= Model.totalDistance -> it.reverse()
                                    oldDistance > Model.totalDistance -> it.animate()
                                }
                            }
                        }
            }
/*
            (1..4).forEach {
                Model.intersectConflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)?.animate()
                }
            }*/
            if (!Model.tourMaintained) throw Exception("Tour broken in TWO_OPT SearchStrategy \r\n${Model.edges.joinToString("\r\n")}")
        }
    },

    SIMULATED_ANNEALING {
        override fun execute() {
            SearchStrategy.RANDOM.execute()
            defaultAnimationOn = false


            var bestDistance = Model.totalDistance
            var bestSolution = Model.toConfiguration()

            val tempSchedule = sequenceOf(
                        generateSequence(80.0) { (it - .005).takeIf { it >= 50 } },
                        generateSequence(50.0) { (it + .005).takeIf { it <= 120 } },
                        generateSequence(120.0) { (it - .005).takeIf { it >= 60 } }
                    ).flatMap { it }
                     .toList().toTypedArray().toDoubleArray().let {
                        TempSchedule(120, it)
                    }

            /* Regarding bestDistance check, I'm pretty sure 17509 is the global optimum, so cheating here to shorten demo for talk :) */
            while(tempSchedule.next() && bestDistance > 17510.0) {

                Model.edges.sampleDistinct(2)
                        .toList()
                        .let { it.first() to it.last() }
                        .also { (e1,e2) ->

                            val oldDistance = Model.totalDistance

                            e1.attemptTwoSwap(e2)?.also { swap ->

                                val neighborDistance = Model.totalDistance

                                when {
                                    oldDistance == neighborDistance -> swap.reverse()
                                    neighborDistance == bestDistance -> swap.reverse()
                                    oldDistance > neighborDistance -> {

                                        if (bestDistance > neighborDistance) {
                                            bestDistance = neighborDistance
                                            println("${tempSchedule.heat}: $bestDistance->$neighborDistance")
                                            bestSolution = Model.toConfiguration()
                                        }
                                        swap.animate()
                                    }
                                    oldDistance < neighborDistance -> {

                                        // Desmos graph for intuition: https://www.desmos.com/calculator/fpuii1qjff
                                        if (weightedCoinFlip(
                                                        exp((-(neighborDistance - bestDistance)) / tempSchedule.heat)
                                                )
                                        ) {
                                            swap.animate()
                                            println("${tempSchedule.heat} accepting degrading solution: $bestDistance -> $neighborDistance")

                                        } else {
                                            swap.reverse()
                                        }
                                    }
                                }
                            }
                        }
                sequentialTransition += timeline(play = false) {
                    keyframe(1.millis) {
                        keyvalue(Model.heatProperty, tempSchedule.ratio)
                    }
                }
            }

            // reset temperature
            sequentialTransition += timeline(play = false) {
                keyframe(1.seconds) {
                    keyvalue(Model.heatProperty, 0)
                }
            }

            // apply best found model
            if (Model.totalDistance > bestDistance) {
                Model.applyConfiguration(bestSolution)
                Model.edges.forEach { it.animateChange() }
            }
            println("$bestDistance<==>${Model.totalDistance}")
            defaultAnimationOn = true

        }
    };

    abstract fun execute()
}