import javafx.animation.Interpolator
import javafx.animation.SequentialTransition
import javafx.animation.Timeline
import javafx.beans.property.ReadOnlyDoubleWrapper
import javafx.beans.property.SimpleObjectProperty
import tornadofx.*


val sequentialTransition = SequentialTransition()
operator fun SequentialTransition.plusAssign(timeline: Timeline) { children += timeline }

class Edge(city: City) {

    val startCity = SimpleObjectProperty(city)
    val endCity = SimpleObjectProperty(city)

    val startX = startCity.select { ReadOnlyDoubleWrapper(it.x) }
    val startY = startCity.select { ReadOnlyDoubleWrapper(it.y) }

    val endX = endCity.select { ReadOnlyDoubleWrapper(it.x) }
    val endY = endCity.select { ReadOnlyDoubleWrapper(it.y) }

    fun swapEndPointWith(otherEdge: Edge) {
        timeline {
            keyframe(3.seconds) {
                keyvalue(endCity, otherEdge.endCity.value, Interpolator.EASE_BOTH)
                keyvalue(otherEdge.endCity, endCity.value, Interpolator.EASE_BOTH)
            }
            sequentialTransition += this
        }
    }
}
object OptimizationModel {
}