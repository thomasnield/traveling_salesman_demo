import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import kotlin.math.exp

var defaultSpeed = 200.millis
var speed = defaultSpeed


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

    val distance get() = CitiesAndDistances.distances[CityPair(startCity.id, endCity.id)]?:0.0

    // animated properties
    val edgeStartX = SimpleDoubleProperty(startCityProperty.get().x)
    val edgeStartY = SimpleDoubleProperty(startCityProperty.get().y)
    val edgeEndX = SimpleDoubleProperty(startCityProperty.get().x)
    val edgeEndY = SimpleDoubleProperty(startCityProperty.get().y)

    fun animateChange() = timeline(play = false) {
            keyframe(speed) {
                keyvalue(edgeStartX, startCity?.x ?: 0.0)
                keyvalue(edgeStartY, startCity?.y ?: 0.0)
                keyvalue(edgeEndX, endCity?.x ?: 0.0)
                keyvalue(edgeEndY, endCity?.y ?: 0.0)
                keyvalue(Model.distanceProperty, Model.totalDistance)
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


        fun animate() = timeline(play = false) {
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
                        keyvalue(Model.distanceProperty, Model.totalDistance)
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

    val distanceProperty = SimpleDoubleProperty(0.0)
    val bestDistanceProperty = SimpleDoubleProperty(0.0)

    val totalDistance get() = Model.edges.map { it.distance }.sum()

    val traverseTour: Sequence<Edge> get() {
        val captured = mutableSetOf<Edge>()

        return generateSequence(edges.first()) { edge ->
            edge.nextEdge?.takeIf { it !in captured }
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

    val heatRatioProperty = SimpleDoubleProperty(0.0)
    var heatRatio by heatRatioProperty

    val heatProperty = SimpleDoubleProperty(0.0)
    var heat by heatProperty

    fun reset() {
        speed = 100.millis

        CitiesAndDistances.cities.zip(edges).forEach { (c,e) ->
            e.startCity = c
            e.endCity = c
            e.animateChange().play()
        }

        speed = defaultSpeed
    }
}
enum class SearchStrategy {

    RANDOM {
        override fun execute() {
            animationQueue.clear()
            val capturedCities = mutableSetOf<Int>()

            val startingEdge = Model.edges.sample()
            var edge = startingEdge

            while(capturedCities.size < CitiesAndDistances.cities.size) {
                capturedCities += edge.startCity.id

                val nextRandom = Model.edges.asSequence()
                        .filter { it.startCity.id !in capturedCities }
                        .sampleOrNull()?:startingEdge

                edge.endCity = nextRandom.startCity
                animationQueue += edge.animateChange()
                edge = nextRandom
            }
            Model.bestDistanceProperty.set(Model.totalDistance)

            if (!Model.tourMaintained) throw Exception("Tour broken in RANDOM SearchStrategy \r\n${Model.edges.joinToString("\r\n")}")
        }
    },

    GREEDY {
        override fun execute() {
            animationQueue.clear()
            val capturedCities = mutableSetOf<Int>()

            var edge = Model.edges.first()

            while(capturedCities.size < CitiesAndDistances.cities.size) {
                capturedCities += edge.startCity.id

                val closest = Model.edges.asSequence().filter { it.startCity.id !in capturedCities }
                        .minBy { CitiesAndDistances.distances[CityPair(edge.startCity.id, it.startCity.id)]?:10000.0 }?: Model.edges.first()

                edge.endCity = closest.startCity
                animationQueue += edge.animateChange()
                edge = closest
            }

            Model.distanceProperty.set(Model.totalDistance)
            Model.bestDistanceProperty.set(Model.totalDistance)
            if (!Model.tourMaintained) throw Exception("Tour broken in GREEDY SearchStrategy \r\n${Model.edges.joinToString("\r\n")}")
        }
    },

    REMOVE_OVERLAPS {
        override fun execute() {
            animationQueue.clear()
            SearchStrategy.RANDOM.execute()
            animationQueue += SearchStrategy.RANDOM.animationQueue

            (1..10).forEach {
                Model.intersectConflicts.forEach { (x, y) ->
                    x.attemptTwoSwap(y)?.animate()?.also {
                        animationQueue += it
                    }
                }
            }
            Model.distanceProperty.set(Model.totalDistance)
            Model.bestDistanceProperty.set(Model.totalDistance)
        }
    },
    TWO_OPT {
        override fun execute() {
            animationQueue.clear()
            SearchStrategy.RANDOM.execute()
            animationQueue += SearchStrategy.RANDOM.animationQueue

            (1..2000).forEach { iteration ->
                Model.edges.sampleDistinct(2).toList()
                        .let { it.first() to it.last() }
                        .also { (e1,e2) ->

                            val oldDistance = Model.totalDistance
                            e1.attemptTwoSwap(e2)?.also {
                                when {
                                    oldDistance <= Model.totalDistance -> it.reverse()
                                    oldDistance > Model.totalDistance -> it.animate().also { animationQueue += it }
                                }
                            }
                        }
            }
            Model.distanceProperty.set(Model.totalDistance)
            Model.bestDistanceProperty.set(Model.totalDistance)
            if (!Model.tourMaintained) throw Exception("Tour broken in TWO_OPT SearchStrategy \r\n${Model.edges.joinToString("\r\n")}")
        }
    },

    SIMULATED_ANNEALING {
        override fun execute() {
            animationQueue.clear()
            SearchStrategy.RANDOM.execute()
            animationQueue += SearchStrategy.RANDOM.animationQueue

            var bestDistance = Model.totalDistance
            var bestSolution = Model.toConfiguration()

            val tempSchedule = sequenceOf(
                        generateSequence(80.0) { (it - .005).takeIf { it >= 50 } },
                        generateSequence(50.0) { (it + .005).takeIf { it <= 120 } },
                        generateSequence(120.0) { (it - .005).takeIf { it >= 60 } }
                    ).flatMap { it }

            /* Regarding bestDistance check, I'm pretty sure 17509 is the global optimum, so cheating here to shorten demo for talk :) */
            tempSchedule.forEach { temperature ->

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
                                            //println("${tempSchedule.heat}: $bestDistance->$neighborDistance")
                                            bestSolution = Model.toConfiguration()
                                            Model.bestDistanceProperty.set(bestDistance)
                                        }
                                        animationQueue += swap.animate()
                                    }
                                    oldDistance < neighborDistance -> {

                                        // Desmos graph for intuition: https://www.desmos.com/calculator/rpfpfiq7ce
                                        if (weightedCoinFlip(
                                                        exp((-(neighborDistance - bestDistance)) / temperature)
                                                )
                                        ) {
                                            animationQueue += swap.animate()
                                            //println("${tempSchedule.heat} accepting degrading solution: $bestDistance -> $neighborDistance")

                                        } else {
                                            swap.reverse()
                                        }
                                    }
                                }
                            }
                        }
                animationQueue += timeline(play = false) {
                    keyframe(1.millis) {
                        keyvalue(Model.heatRatioProperty, temperature / 120.0)
                        keyvalue(Model.heatProperty, temperature)
                    }
                }
            }

            // reset temperature
            animationQueue += timeline(play = false) {
                keyframe(1.seconds) {
                    keyvalue(Model.heatRatioProperty, 0)
                    keyvalue(Model.heatProperty, 0)
                }
            }

            // apply best found model
            if (Model.totalDistance > bestDistance) {
                Model.applyConfiguration(bestSolution)
                Model.edges.forEach {
                    animationQueue += it.animateChange()
                }
            }
            println("$bestDistance<==>${Model.totalDistance}")
        }
    };/*,

    // This is painfully slow to run
    // Branch-and-bound ojAlgo solver is probably not the best approach

    INTEGER {
        override fun execute() {

            val solver = ExpressionsBasedModel()

            val cities = CitiesAndDistances.cities

            val cityDummies = cities.map { it to solver.variable(isInteger = true, lower = 1, upper = cities.size) }.toMap()

            data class Segment(val city1: City, val city2: City) {
                val selected = solver.variable(isBinary = true)
                val distance get() = city1.distanceTo(city2)

                val u_i = cityDummies[city1]!!
                val u_j = cityDummies[city2]!!

                init {
                    solver.expression {
                        set(u_i, 1)
                        set(u_j, -1)
                        set(selected, cities.size)
                        lower(2)
                        upper(cities.size -1)
                    }
                }
                operator fun contains(city: City) = city == city1 || city == city2
            }

            // create segments
            val segments = cities.flatMap { city1 ->
                cities.filter { it != city1 }
                        .map { city2 -> if (city1.name > city2.name) city2 to city1 else city1 to city2 }
            }.distinct()
            .map { Segment(it.first, it.second) }
            .toList()

            solver.apply {

                // constrain each city to have two connections
                cities.forEach { city ->
                        expression(lower=2, upper=2) {
                            segments.filter { city in it }.forEach { set(it.selected, 1) }
                        }
                }

                // minimize distance objective
                expression(weight = 1) {
                    segments.forEach {
                        set(it.selected, it.distance)
                    }
                }

                // prevent sub-tours

            }

            // execute and plot
            val result = solver.minimise().also(::println)

            segments.filter { it.selected.value.toInt() == 1 }
                    .zip(Model.edges)
                    .forEach { (selectedSegment, edge) ->
                        edge.startCity = selectedSegment.city1
                        edge.endCity = selectedSegment.city2
                        animationQueue += edge.animateChange()
                    }

            Model.distanceProperty.set(result.value)
        }
    };*/

    val animationQueue = SequentialTransition()

    abstract fun execute()
}


operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }
fun SequentialTransition.clear() = children.clear()
operator fun SequentialTransition.plusAssign(other: SequentialTransition) { children.addAll(SearchStrategy.RANDOM.animationQueue.children) }