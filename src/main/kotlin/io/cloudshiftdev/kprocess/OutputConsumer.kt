package io.cloudshiftdev.kprocess

import java.io.File
import java.io.InputStream

public sealed class OutputConsumer {
    internal data object Discard : OutputConsumer()

    internal data object Inherit : OutputConsumer()

    internal data class Stream<T>(val handler: suspend (InputStream) -> T) : OutputConsumer()

    public companion object {
        public fun discard(): OutputConsumer = Discard

        public fun inherit(): OutputConsumer = Inherit

        public fun <T> stream(consumer: suspend (InputStream) -> T): OutputConsumer =
            Stream(consumer)

        public fun <T> lines(consumer: suspend (Sequence<String>) -> T): OutputConsumer =
            Stream { input ->
                input.bufferedReader().useLines { consumer(it) }
            }

        public fun file(file: File): OutputConsumer = Stream { input ->
            input.copyTo(file.outputStream())
        }
    }
}

internal fun OutputConsumer?.toRedirect(): ProcessBuilder.Redirect {
    return when (this) {
        is OutputConsumer.Discard -> ProcessBuilder.Redirect.DISCARD
        is OutputConsumer.Inherit -> ProcessBuilder.Redirect.INHERIT
        is OutputConsumer.Stream<*> -> ProcessBuilder.Redirect.PIPE
        null -> ProcessBuilder.Redirect.DISCARD
    }
}
