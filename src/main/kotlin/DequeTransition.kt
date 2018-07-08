

import javafx.animation.Timeline
import java.util.concurrent.ConcurrentLinkedDeque

class DequeTransition {
    private var _queue = ConcurrentLinkedDeque<Timeline>()


    operator fun plusAssign(timeline: Timeline) {

        _queue.add(
                timeline.apply {
                    setOnFinished {
                        _queue.poll()?.play()
                    }

                    if (_queue.size == 0) play()
                }
        )
    }
}


