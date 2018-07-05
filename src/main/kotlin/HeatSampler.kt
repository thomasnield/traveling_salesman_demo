import java.util.concurrent.ThreadLocalRandom


class WeightedBooleanRandom(val trueProbability: Double) {

    fun draw() = ThreadLocalRandom.current().nextDouble(0.0,1.0) <= trueProbability
}
class HeatSampler(val startingHeat: Int, val maxHeat: Int , val coolingStep: Int)  {

    init {
        if (startingHeat > maxHeat) throw Exception("startingHeat $startingHeat must be less than maxHeat $maxHeat")
    }

    private var _heat = startingHeat
    val heat get() = _heat

    val ratio get() = _heat.toDouble() / maxHeat.toDouble()


    fun draw() =  ThreadLocalRandom.current().nextInt(1,maxHeat+1)
            .let {
                if (it < _heat) Temperature.HOT else Temperature.COLD
            }

    fun cool(): Boolean {
        if (_heat == 0) return false
        _heat = if (_heat - coolingStep >= 0) _heat-coolingStep else 0
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

    val heatSampler = HeatSampler(800, 1000, 5)

    while (heatSampler.cool()) {
        (1..10000).map { heatSampler.draw() }
                .countBy { it }
                .let {
                    println("${it[HeatSampler.Temperature.HOT]},${it[HeatSampler.Temperature.COLD]}")
                }
    }
}*/
