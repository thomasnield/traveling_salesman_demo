import java.util.concurrent.ThreadLocalRandom


class WeightedBooleanRandom(val trueProbability: Double) {

    fun draw() = ThreadLocalRandom.current().nextDouble(0.0,1.0) <= trueProbability
}

class TempSchedule(val maxTemp: Int, val temperatureSequence: IntArray)  {

    private var index = -1

    val heat get() = temperatureSequence[index]
    val ratio get() = temperatureSequence[index].toDouble() / maxTemp.toDouble()

    fun cool(): Boolean {
        if (index == (temperatureSequence.size-1)) return false
        index++
        return true
    }
}

/**
 * Hot = Non-greedy, random-tolerant
 *
 * Cold = Greedy, minimum-seeking
 */
enum class Temperature {
    HOT,
    COLD
}

/*
fun main(args: Array<String>) {

    val heatSampler = TempSchedule(800, 1000, 5)

    while (heatSampler.cool()) {
        (1..10000).map { heatSampler.draw() }
                .countBy { it }
                .let {
                    println("${it[TempSchedule.Temperature.HOT]},${it[TempSchedule.Temperature.COLD]}")
                }
    }
}*/
