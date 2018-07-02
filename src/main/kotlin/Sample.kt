import java.util.concurrent.ThreadLocalRandom


/**
 * Samples a single random element `T` from a `List<T>`, and throws an error if no elements exist
 */
fun <T> List<T>.sample() = sampleOrNull()?: throw Exception("No elements found!")

/**
 * Samples a single random element `T` from a `List<T>`, and returns `null` if no elements exist
 */
fun <T> List<T>.sampleOrNull(): T? {
    if (size == 0) return null

    val random = ThreadLocalRandom.current().nextInt(0,size)

    return this[random]
}


/**
 * Samples a single random element `T` from a `Sequence<T>`, and throws an error if no elements exist
 */
fun <T> Sequence<T>.sample() = toList().sample()

/**
 * Samples a single random element `T` from a `Sequence<T>`, and returns `null` if no elements exist
 */
fun <T> Sequence<T>.sampleOrNull() = toList().sampleOrNull()


/**
 * Samples a single random element `T` from a `Sequence<T>`, and throws an error if no elements exist
 */
fun <T> Iterable<T>.sample() = toList().sample()

/**
 * Samples a single random element `T` from an `Iterable<T>`, and returns `null` if no elements exist
 */
fun <T> Iterable<T>.sampleOrNull() = toList().sampleOrNull()


/**
 * Samples a single random element `T` from an `Iterable<T>`, and returns `null` if no elements exist
 */
fun <T> List<T>.sampleDistinct(sampleSize: Int): List<T> {

    val cappedSampleSize = if (sampleSize > size) size else sampleSize

    return (0..Int.MAX_VALUE).asSequence().map {
        ThreadLocalRandom.current().nextInt(0,size)
    }.distinct()
    .take(cappedSampleSize)
    .map { this[it] }
    .toList()
}


fun <T> Sequence<T>.sampleDistinct(sampleSize: Int) = toList().sampleDistinct(sampleSize)