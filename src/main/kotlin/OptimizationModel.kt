import javafx.animation.Interpolator
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.property.ReadOnlyDoubleProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.paint.Color
import javafx.scene.shape.Line
import tornadofx.*
import java.util.concurrent.ThreadLocalRandom


val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

class Edge(city: City) {

    val startCityProperty = SimpleObjectProperty(city)
    var startCity by startCityProperty

    val endCityProperty = SimpleObjectProperty(city)
    var endCity by endCityProperty

    private val _edgeStartX = SimpleDoubleProperty(startCityProperty.get().x)
    private val _edgeStartY = SimpleDoubleProperty(startCityProperty.get().y)
    private val _edgeEndX = SimpleDoubleProperty(startCityProperty.get().x)
    private val _edgeEndY = SimpleDoubleProperty(startCityProperty.get().y)

    init {
        startCityProperty.onChange {
            sequentialTransition += timeline(play = false) {
                keyframe(300.millis) {
                    keyvalue(_edgeStartX,it?.x ?: 0.0)
                    keyvalue(_edgeStartY,it?.y ?: 0.0)
                }
            }
        }
        endCityProperty.onChange {
            sequentialTransition += timeline(play = false) {
                keyframe(300.millis) {
                    keyvalue(_edgeEndX,it?.x ?: 0.0)
                    keyvalue(_edgeEndY,it?.y ?: 0.0)
                }
            }
        }
    }

    val edgeStartX: ReadOnlyDoubleProperty = _edgeStartX
    val edgeStartY: ReadOnlyDoubleProperty = _edgeStartY

    val edgeEndX: ReadOnlyDoubleProperty = _edgeEndX
    val edgeEndY: ReadOnlyDoubleProperty = _edgeEndY

    val line = Line().apply {
        startXProperty().bind(edgeStartX)
        startYProperty().bind(edgeStartY)
        endXProperty().bind(edgeEndX)
        endYProperty().bind(edgeEndY)
        strokeWidth = 3.0
        stroke = Color.RED
    }

    fun swapEndPointWith(otherEdge: Edge) {
        timeline {
            keyframe(3.seconds) {
                val endCity1 = endCityProperty.get()
                val endCity2 = otherEdge.endCityProperty.get()

                keyvalue(endCityProperty, endCity2, Interpolator.EASE_BOTH)
                keyvalue(otherEdge.endCityProperty, endCity1, Interpolator.EASE_BOTH)
            }
            sequentialTransition += this
        }
    }
}
object OptimizationModel {

    val edges = CitiesAndDistances.cities.asSequence()
            .map { Edge(it) }
            .toList()

    val randomEdge get() = ThreadLocalRandom.current().nextInt(0, edges.size).let { edges[it] }

    fun greedySearch() {

        val capturedCities = mutableSetOf<Int>()

        var edge = edges.first()

        while(capturedCities.size < CitiesAndDistances.cities.size) {
            capturedCities += edge.startCity.id

             val closest = edges.asSequence().filter { it != edge && it.startCity.id !in capturedCities }
                     .minBy { CitiesAndDistances.distances[CityPair(edge.startCity.id, it.startCity.id)]?:10000.0 }?:edges.first()

            edge.endCityProperty.set(closest.startCity)
            edge = closest
        }
/*
        (0..10000).forEach {
            val x = OptimizationModel.randomEdge
            val y = OptimizationModel.randomEdge

            x.endCityProperty.set(y.startCityProperty.get())
        }*/
        sequentialTransition.play()
    }
}