import javafx.application.Application
import javafx.beans.binding.Bindings
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import tornadofx.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.Callable


fun main(args: Array<String>) = Application.launch(TSPApp::class.java, *args)

class TSPApp: App(TSPView::class)




class TSPView: View() {

    private val backingList = FXCollections.observableArrayList<Edge>()

    val selectedEdge = SimpleObjectProperty<Edge>()

    override val root = borderpane {

        left = form {

            title = "Traveling Salesman Problem"

            fieldset {
                field("ROUTE") {
                    listview(backingList) {
                        selectedEdge.bind(selectionModel.selectedItemProperty())
                    }
                }
            }
            fieldset {
                field("DISTANCE") {
                    textfield {
                        Model.distanceProperty.onChange {
                            text = BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toString()
                        }
                    }
                }
            }
            fieldset {
                field("ALGORITHM") {
                    vbox {
                        SearchStrategy.values().forEach { ss ->

                            borderpane {
                                val disablePlayButton = SimpleBooleanProperty(true)

                                center = button(ss.name.replace("_", " ")) {
                                    useMaxWidth = true

                                    setOnAction {
                                        Model.reset()
                                        ss.execute()
                                        backingList.clear()
                                        backingList.setAll(Model.edges)
                                        disablePlayButton.set(false)
                                    }
                                }
                                right = button("\u25B6") {
                                    textFill = Color.GREEN
                                    disableProperty().bind(disablePlayButton)

                                    setOnAction { _ ->
                                        backingList.clear()
                                        Model.reset()
                                        ss.animationQueue.play()
                                        ss.savedEdges
                                                .map { Edge(it.startCity).apply { startCity = it.startCity; endCity = it.endCity } }
                                                .forEach {
                                                    backingList += it
                                                }
                                    }
                                }
                            }
                        }
                    }
                }

                field("TEMP") {
                    stackpane {
                        progressbar(Model.heatRatioProperty) {
                            useMaxWidth = true
                            style {
                                accentColor = Color.RED
                            }
                        }
                        label {
                            textFill = Color.BLACK

                            Model.heatProperty.onChange {
                                text = it.toBigDecimal().setScale(3, RoundingMode.HALF_UP).toString()
                            }
                        }
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
                            stroke = if (it != null && it.startCity == edge.startCity && it.endCity == edge.endCity) Color.BLUE else Color.RED
                        }
                    }
                }
            }
        }
    }
}
