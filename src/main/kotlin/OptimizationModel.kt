import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.util.concurrent.Callable
import java.util.concurrent.ThreadLocalRandom


val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

var speed = 200.millis

class Edge(city: City) {

    val startCityProperty = SimpleObjectProperty(city)
    var startCity by startCityProperty

    val endCityProperty = SimpleObjectProperty(city)
    var endCity by endCityProperty

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

    fun swapEndPointWith(otherEdge: Edge) {
        val endCity1 = endCity
        val endCity2 = otherEdge.endCity

        if (startCity == endCity2) {
            throw Exception("Help!")
        }
        endCity = endCity2
        otherEdge.endCity = endCity1
    }
}
object OptimizationModel {

    val edges = CitiesAndDistances.cities.asSequence()
            .map { Edge(it) }
            .toList()

    val distancesProperty = Bindings.createDoubleBinding(
            Callable<Double> { edges.asSequence().map { it.distance.get() }.sum() },
            *edges.map { it.distance }.toTypedArray()
    )
    fun randomEdge(except: Edge? = null): Edge = ThreadLocalRandom.current().nextInt(0, edges.size)
            .let { edges[it] }
            .takeIf { it.startCity != except?.startCity }?: randomEdge(except)

    fun randomSearch() {

    }
    fun greedySearch() {

        val capturedCities = mutableSetOf<Int>()

        var edge = edges.first()

        while(capturedCities.size < CitiesAndDistances.cities.size) {
            capturedCities += edge.startCity.id

             val closest = edges.asSequence().filter { it != edge && it.startCity.id !in capturedCities }
                     .minBy { CitiesAndDistances.distances[CityPair(edge.startCity.id, it.startCity.id)]?:10000.0 }?:edges.first()

            edge.endCity = closest.startCity
            edge = closest
        }
    }
    fun kOptSearch(){
        speed = 500.millis

        sequentialTransition += timeline(play=false) {
            delay = 5.seconds
        }
        greedySearch()

        (0..1000).forEach {
            val x = OptimizationModel.randomEdge()
            val y = OptimizationModel.randomEdge(x)

            if (x.endCity != y.endCity &&
                    x.startCity != y.startCity &&
                    x.startCity != y.endCity &&
                    y.startCity != x.endCity) {

                val currentValue = edges.asSequence().map { it.distanceNow }.sum()

                x.swapEndPointWith(y)

                val newValue = edges.asSequence().map { it.distanceNow }.sum()

                if (currentValue < newValue) {
                    x.swapEndPointWith(y)
                }
            }
        }
    }
}