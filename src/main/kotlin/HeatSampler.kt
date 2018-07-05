import java.util.concurrent.ThreadLocalRandom


class WeightedBooleanRandom(val trueProbability: Double) {

    fun draw() = ThreadLocalRandom.current().nextDouble(0.0,1.0) <= trueProbability
}
class HeatSampler(val startingHeat: Int, val maxHeat: Int , val coolingStep: Int)  {

    init {
        if (startingHeat > maxHeat) throw Exception("startingHeat $startingHeat must be less than maxHeat $maxHeat")
    }

    var heat = startingHeat

    val ratio get() = heat.toDouble() / maxHeat.toDouble()


    fun draw() =  ThreadLocalRandom.current().nextInt(1,maxHeat+1)
            .let {
                if (it < heat) Temperature.HOT else Temperature.COLD
            }

    fun cool(): Boolean {
        if (heat == 0) return false
        heat = if (heat - coolingStep >= 0) heat-coolingStep else 0
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
