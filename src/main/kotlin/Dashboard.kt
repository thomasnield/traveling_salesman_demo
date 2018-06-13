import javafx.animation.Interpolator
import javafx.animation.SequentialTransition
import javafx.application.Application
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.text.FontWeight
import tornadofx.*


fun main(args: Array<String>) = Application.launch(TSPApp::class.java, *args)

class TSPApp: App(TSPView::class)




class TSPView: View() {

    val selectedCity = SimpleObjectProperty<City>()

    override val root = borderpane {

        left = vbox {
            label("CITIES") {
                textFill = Color.RED
                style { fontWeight = FontWeight.BOLD }
            }
            listview(CitiesAndDistances.cities.sortedBy { it.city }.observable()) {
                selectedCity.bind(selectionModel.selectedItemProperty())
            }
        }

        center = pane {

            imageview(Image("europe.png")) {
                fitHeight = 1000.0
                fitWidth = 1000.0

                CitiesAndDistances.cities.forEach { city ->
                    circle(city.x,city.y,10.0) {
                        fill = Color.RED
                        selectedCity.onChange {
                            fill = if (it == city) Color.BLUE else Color.RED
                        }
                    }
                }

                CitiesAndDistances.cities.zipWithNext { a, b ->
                        st.children += timeline(play=false) {
                                line {
                                    stroke = Color.RED
                                    strokeWidth = 5.0
                                    startX = a.x
                                    startY = a.y
                                    endX = a.x
                                    endY = a.y
                                    keyframe(300.millis) {
                                        keyvalue(endXProperty(), b.x, Interpolator.EASE_BOTH)
                                        keyvalue(endYProperty(), b.y, Interpolator.EASE_BOTH)
                                    }
                                }
                            }
                        }

                st.play()
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