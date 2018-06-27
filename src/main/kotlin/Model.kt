import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.beans.property.ObjectProperty
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.util.concurrent.Callable


val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

var speed = 200.millis

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

    private val _edgeStartX = SimpleDoubleProperty(startCityProperty.get().x)
    private val _edgeStartY = SimpleDoubleProperty(startCityProperty.get().y)
    private val _edgeEndX = SimpleDoubleProperty(startCityProperty.get().x)
    private val _edgeEndY = SimpleDoubleProperty(startCityProperty.get().y)
    private val _distance = SimpleDoubleProperty(0.0)

    val distanceNow get() = CitiesAndDistances.distances[CityPair(startCity.id, endCity.id)]?:0.0

    init {
        startCityProperty.onChange {
            sequentialTransition += timeline(play = false) {
                keyframe(speed) {
                    keyvalue(_edgeStartX, it?.x ?: 0.0)
                    keyvalue(_edgeStartY, it?.y ?: 0.0)
                    keyvalue(_distance, distanceNow)
                }
            }
        }
        endCityProperty.onChange {
            sequentialTransition += timeline(play = false) {
                keyframe(speed) {
                    keyvalue(_edgeEndX, it?.x ?: 0.0)
                    keyvalue(_edgeEndY, it?.y ?: 0.0)
                    keyvalue(_distance, distanceNow)
                }
            }
        }
    }

    val edgeStartX: ReadOnlyDoubleProperty = _edgeStartX
    val edgeStartY: ReadOnlyDoubleProperty = _edgeStartY

    val edgeEndX: ReadOnlyDoubleProperty = _edgeEndX
    val edgeEndY: ReadOnlyDoubleProperty = _edgeEndY

    val distance: ReadOnlyDoubleProperty = _distance

    val nextEdge get() = Model.edges.firstOrNull { it.startCity == endCity } ?:
            Model.edges.firstOrNull { it.startCity == startCity }

    val intersectConflicts get() = Model.edges.asSequence()
            .filter { it != this }
            .filter { edge2 ->
                startCity !in edge2.let { setOf(it.startCity, it.endCity) } &&
                        endCity !in edge2.let { setOf(it.startCity, it.endCity) } &&
                    intersect(startPoint, endPoint, edge2.startPoint, edge2.endPoint)
            }

    fun executeSwap(otherEdge: Edge) {

        val e1 = this
        val e2 = otherEdge

        val startCity1 = startCity
        val endCity1 = endCity
        val startCity2 = otherEdge.startCity
        val endCity2 = otherEdge.endCity

        class Swap(val city1: City,
                   val city2: City,
                   val property1: ObjectProperty<City>,
                   val property2: ObjectProperty<City>
        ) {
            fun execute() {
                property1.set(city2)
                property2.set(city1)
            }
            fun reverse() {
                val p1 = property1.get()
                val p2 = property2.get()
                property1.set(p2)
                property2.set(p1)
            }
        }
        val distance = Model.edges.map { it.distanceNow }.sum()

        sequenceOf(
                Swap(startCity1, startCity2, e1.startCityProperty, e2.startCityProperty),
                Swap(endCity1, endCity2, e1.endCityProperty, e2.endCityProperty),

                Swap(startCity1, endCity2, e1.startCityProperty, e2.endCityProperty),
                Swap(endCity1, startCity2, e1.endCityProperty, e2.startCityProperty)

        ).map { swap ->
            swap.execute()
/*
            println(e1)
            println(e2)
            println()*/
            // Why is tour always getting broken?
            val result = Model.tourMaintained && Model.edges.map { it.distanceNow }.sum() < distance

            result to swap
        }.filter { it.first }
         .map { it.second }
        .forEach {
            it.execute()
        }
    }

    override fun toString() = "$startCity-$endCity"
}
object Model {


    val edges = CitiesAndDistances.cities.asSequence()
            .map { Edge(it) }
            .toList()

    val distancesProperty = Bindings.createDoubleBinding(
            Callable<Double> { edges.asSequence().map { it.distance.get() }.sum() },
            *edges.map { it.distance }.toTypedArray()
    )

    val tourMaintained: Boolean get() {
        val captured = mutableSetOf<Edge>()

        return generateSequence(edges.first()) {
            it.nextEdge?.takeIf { it !in captured }
        }.onEach { captured += it }
         .count() == edges.count()
    }

    val intersectConflicts get() = edges.asSequence()
            .flatMap { edge1 -> edge1.intersectConflicts.map { edge2 -> edge1 to edge2} }
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
        }
    },

    TWO_OPT {
        override fun execute() {

            speed = 50.millis

            sequentialTransition += timeline(play=false) {
                delay = 5.seconds
            }

            SearchStrategy.GREEDY.execute()

            speed = 100.millis

            (1..4).forEach {
                Model.intersectConflicts.forEach { (x, y) ->
                    x.executeSwap(y)
                }
            }
        }
    };

    abstract fun execute()
}