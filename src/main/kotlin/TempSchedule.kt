import java.util.concurrent.ThreadLocalRandom

/**
 * Simulates a weighted TRUE/FALSE coin flip, with a percentage of probability towards TRUE
 */
fun weightedCoinFlip(trueProbability: Double) =
        ThreadLocalRandom.current().nextDouble(0.0,1.0) <= trueProbability



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