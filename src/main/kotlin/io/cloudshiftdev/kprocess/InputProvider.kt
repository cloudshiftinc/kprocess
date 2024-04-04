package io.cloudshiftdev.kprocess

import java.io.File
import java.io.OutputStream

public sealed class InputProvider {
    internal data object None : InputProvider()

    internal data object Inherit : InputProvider()

    internal data class Stream(internal val handler: suspend (OutputStream) -> Unit) :
        InputProvider()

    public companion object {
        public fun none(): InputProvider = None

        public fun inherit(): InputProvider = Inherit

        public fun stream(handler: suspend (OutputStream) -> Unit): InputProvider = Stream(handler)

        public fun string(value: String): InputProvider = Stream { it.write(value.toByteArray()) }

        public fun file(file: File): InputProvider = Stream { file.inputStream().copyTo(it) }
    }
}

internal fun InputProvider?.toRedirect(): ProcessBuilder.Redirect {
    return when (this) {
        is InputProvider.None -> ProcessBuilder.Redirect.PIPE
        is InputProvider.Inherit -> ProcessBuilder.Redirect.INHERIT
        is InputProvider.Stream -> ProcessBuilder.Redirect.PIPE
        null -> ProcessBuilder.Redirect.PIPE
    }
}
