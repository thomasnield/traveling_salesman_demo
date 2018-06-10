import javafx.animation.Interpolator
import javafx.application.Application
import javafx.scene.paint.Color
import tornadofx.*

fun main(args: Array<String>) = Application.launch(TSPApp::class.java, *args)

class TSPApp: App(TSPView::class)

class TSPView: View() {
    override val root = vbox {
        line {
            fill = Color.BLACK
            startX = 0.0
            startY = 0.0
            endX = 100.0
            endY = 100.0

            timeline {
                keyframe(3.seconds) {
                    keyvalue(endXProperty(), 300.0, Interpolator.EASE_BOTH)
                    keyvalue(endYProperty(),200.0, Interpolator.EASE_BOTH)
                }
                isAutoReverse = true
                cycleCount = 100
            }
        }
    }
}