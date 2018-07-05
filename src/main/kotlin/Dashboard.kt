import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.ReadOnlyIntegerWrapper
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.image.Image
import javafx.scene.paint.Color
import tornadofx.*
import java.util.concurrent.Callable


fun main(args: Array<String>) = Application.launch(TSPApp::class.java, *args)

class TSPApp: App(TSPView::class)




class TSPView: View() {

    private val backingList = FXCollections.observableArrayList<Edge>()

    val selectedEdge = SimpleObjectProperty<Edge>()

    override val root = borderpane {

        left = form {
            fieldset {
                field("ROUTE") {
                    listview(backingList) {

                        selectedEdge.bind(selectionModel.selectedItemProperty())

                        cellFormat {
                            textProperty().bind(
                                    Bindings.createStringBinding(Callable { "${it.startCity}→${it.endCity}" }, it.startCityProperty, it.endCityProperty)
                            )
                        }

                    }
                }
            }
            fieldset {
                field("DISTANCE") {
                    textfield(Model.animationDistanceProperty.select { ReadOnlyIntegerWrapper(it.toInt()) })
                }
            }
            fieldset {
                field("ALGORITHM") {
                    vbox {
                        SearchStrategy.values().forEach { ss ->
                            button(ss.name.replace("_", " ")) {
                                useMaxWidth = true

                                setOnAction {
                                    defaultAnimationOn = true

                                    sequentialTransition.children.clear()
                                    Model.reset()
                                    ss.execute()

                                    backingList.setAll(
                                            Model.traverseTour.toList().observable()
                                    )
                                    sequentialTransition.play()
                                }
                            }
                        }
                    }
                }

                field("HEAT") {
                    stackpane {
                        progressbar(Model.heatProperty) {
                            useMaxWidth = true
                            style {
                                accentColor = Color.RED
                            }
                        }
                        label(Model.heatProperty)
                    }

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

                Model.edges.forEach { edge ->
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
            }
        }
    }
}