fun main(args: Array<String>) {
    CitiesAndDistances.distances.forEach(::println)
}


data class CityPair(val city1: Int, val city2: Int)

data class City(val id: Int, val city: String, val x: Double, val y: Double) {
    override fun toString() = city
}

object CitiesAndDistances {

    val citiesById = CitiesAndDistances::class.java.getResource("cities.csv").readText().lines()
            .asSequence()
            .map { it.split(",") }
            .map { City(it[0].toInt(), it[1], it[2].toDouble(), it[3].toDouble()) }
            .map { it.id to it }
            .toMap()

    val citiesByString = citiesById.entries.asSequence()
            .map { it.value.city to it.value }
            .toMap()

    val cities = citiesById.values.toList()

    val distances = CitiesAndDistances::class.java.getResource("distances.csv").readText().lines()
            .asSequence()
            .map { it.split(",") }
            .map { CityPair(it[0].toInt(), it[1].toInt()) to it[2].toDouble() }
            .toMap()
}

operator fun Map<CityPair,String>.get(city1: Int, city2: Int) = get(CityPair(city1,city2))
operator fun Map<CityPair,String>.get(city1: City, city2: City) = get(CityPair(city1.id,city2.id))
