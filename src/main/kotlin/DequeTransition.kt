import javafx.animation.Timeline
import java.util.*

class DequeTransition {
    private var _queue = Collections.synchronizedList(mutableListOf<Timeline>())

    operator fun plusAssign(timeline: Timeline) {

        _queue.add(
                timeline.apply {
                    setOnFinished {
                        _queue.remove(this)
                        _queue.firstOrNull()?.play()
                    }

                    if (_queue.isEmpty()) play()
                }
        )
    }
}