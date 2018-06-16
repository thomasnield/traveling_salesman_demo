import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import javafx.scene.paint.Color
import tornadofx.*
import java.util.concurrent.Callable


fun main(args: Array<String>) = Application.launch(TSPApp::class.java, *args)

class TSPApp: App(TSPView::class)




class TSPView: View() {

    val selectedEdge = SimpleObjectProperty<Edge>()

    override val root = borderpane {

        left = form {
            fieldset {
                field("CITIES") {
                    listview(OptimizationModel.edges.observable()) {

                        selectedEdge.bind(selectionModel.selectedItemProperty())

                        cellFormat {
                            textProperty().bind(
                                    Bindings.createStringBinding(Callable { "${it.startCity}->${it.endCity}" }, it.startCityProperty, it.endCityProperty)
                            )
                        }
                    }
                }
            }
            fieldset {
                field("DISTANCE") {
                    textfield(OptimizationModel.distancesProperty.select { ReadOnlyIntegerWrapper(it.toInt()) })
                }
            }
        }

        center = pane {

            imageview(Image("europe.png")) {
                fitHeight = 1000.0
                fitWidth = 1000.0

                CitiesAndDistances.cities.forEach { city ->
                    circle(city.x,city.y,10.0) {
                        fill = Color.RED
                        selectedEdge.onChange {
                            fill = if (city in setOf(it?.startCity, it?.endCity)) Color.BLUE else Color.RED
                        }
                    }
                }

                OptimizationModel.edges.forEach { edge ->
                    line {
                        startXProperty().bind(edge.edgeStartX)
                        startYProperty().bind(edge.edgeStartY)
                        endXProperty().bind(edge.edgeEndX)
                        endYProperty().bind(edge.edgeEndY)
                        strokeWidth = 3.0
                        stroke = Color.RED
                        selectedEdge.onChange {
                            stroke = if (it == edge) Color.BLUE else Color.RED
                        }
                    }
                }

                OptimizationModel.twoOptSearch()
                sequentialTransition.play()
            }
        }
    }
}

//COOL INITIALIZE EFFECT!
/*
timeline {
    keyframe(1.seconds) {
        keyvalue(endXProperty(), b.x, Interpolator.EASE_BOTH)
        keyvalue(endYProperty(), b.y, Interpolator.EASE_BOTH)
    }
}
 */

/*
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
 */

/*
addEventHandler(MouseEvent.MOUSE_CLICKED) {
            circle(it.x,it.y,10.0) {
                fill = Color.RED
            }

            val choices = CitiesAndDistances.cities

            val dialog = ChoiceDialog<City>(choices[cityIncrementer++], choices)
            val result = dialog.showAndWait()
            if (result.isPresent) {
                println("${result.get().id},${it.x},${it.y}")
            }
        }
 */