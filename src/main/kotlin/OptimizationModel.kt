import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*
import java.util.concurrent.Callable


val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

var speed = 200.millis

class Edge(city: City) {

    val startCityProperty = ReadOnlyObjectWrapper(city)
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

    val nextEdge get() = OptimizationModel.edges.firstOrNull { it.startCity == endCity } ?:
            OptimizationModel.edges.firstOrNull { it.startCity == startCity }

    fun executeSwaps1(otherEdge: Edge) {

        val startCity1 = startCity
        val startCity2 = otherEdge.startCity
        val endCity1 = endCity
        val endCity2 = otherEdge.endCity

        // combo attempt 1
        endCity = startCity2
        otherEdge.startCity = endCity1
    }

    fun exceuteSwaps2(otherEdge: Edge) {

        val startCity1 = startCity
        val startCity2 = otherEdge.startCity
        val endCity1 = endCity
        val endCity2 = otherEdge.endCity

        // combo attempt 2
        otherEdge.endCity = startCity1
        startCity = endCity2
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

    val tourMaintained: Boolean get() {
        val captured = mutableSetOf<Edge>()

        return generateSequence(edges.first()) {
            it.nextEdge?.takeIf { it !in captured }
        }.onEach { captured += it }
         .count() == edges.count()
    }


    fun randomSearch() {
        val capturedCities = mutableSetOf<Int>()

        val startingEdge = edges.sample()
        var edge = startingEdge

        while(capturedCities.size < CitiesAndDistances.cities.size) {
            capturedCities += edge.startCity.id

           val nextRandom = edges.asSequence().filter { it.startCity.id !in capturedCities }.sampleOrNull()?:startingEdge

            edge.endCity = nextRandom.startCity
            edge = nextRandom
        }
    }
    fun greedySearch() {

        val capturedCities = mutableSetOf<Int>()

        var edge = edges.first()

        while(capturedCities.size < CitiesAndDistances.cities.size) {
            capturedCities += edge.startCity.id

             val closest = edges.asSequence().filter { it.startCity.id !in capturedCities }
                     .minBy { CitiesAndDistances.distances[CityPair(edge.startCity.id, it.startCity.id)]?:10000.0 }?:edges.first()

            edge.endCity = closest.startCity
            edge = closest
        }
    }
    fun twoOptSearch(){
        speed = 1.millis

        sequentialTransition += timeline(play=false) {
            delay = 5.seconds
        }
        randomSearch()

        (0..1000).forEach {
            val sample = OptimizationModel.edges.sample(2)

            val x = sample.first()
            val y = sample.last()

            val currentValue1 = edges.asSequence().map { it.distanceNow }.sum()

            x.executeSwaps1(y)

            val newValue1 = edges.asSequence().map { it.distanceNow }.sum()

            if (currentValue1 < newValue1 || !tourMaintained) {
                x.executeSwaps1(y)
            }

            x.exceuteSwaps2(y)

            val currentValue2 = edges.asSequence().map { it.distanceNow }.sum()
            val newValue2 = edges.asSequence().map { it.distanceNow }.sum()

            if (currentValue2 < newValue2 || !tourMaintained) {
                x.exceuteSwaps2(y)
            }
        }
    }
}




