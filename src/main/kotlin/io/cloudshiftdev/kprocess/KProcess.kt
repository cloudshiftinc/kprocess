package io.cloudshiftdev.kprocess

import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

public suspend fun <O> exec(spec: (ExecSpec<O>.() -> Unit)): ExecResult<O> =
    execute(ExecSpecImpl<O>().apply(spec))

public suspend fun execToList(spec: (ExecSpec<List<String>>.() -> Unit)): ExecResult<List<String>> =
    exec {
        apply(spec)
        outputConsumer(OutputConsumer.lines { it.toList() })
    }

public suspend fun execToFile(file: File, spec: (ExecSpec<Unit>.() -> Unit)): ExecResult<Unit> =
    exec {
        apply(spec)
        outputConsumer(OutputConsumer.file(file))
    }

public class ExecResult<O>
internal constructor(
    private val executableName: String,
    public val exitCode: Int,
    public val output: O,
    public val error: List<String>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExecResult<*>

        if (executableName != other.executableName) return false
        if (exitCode != other.exitCode) return false
        if (output != other.output) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = executableName.hashCode()
        result = 31 * result + exitCode
        result = 31 * result + (output?.hashCode() ?: 0)
        result = 31 * result + error.hashCode()
        return result
    }

    override fun toString(): String {
        return "ExecResult(executableName='$executableName', exitCode=$exitCode, output=$output, error=$error)"
    }
}

private fun requireProcessStart(predicate: Boolean, lazyMessage: () -> String) {
    if (!predicate) throw ProcessStartException(lazyMessage())
}

private suspend fun <O> execute(spec: ExecSpecImpl<O>): ExecResult<O> {
    requireProcessStart(spec.commandLine.isNotEmpty()) { "Empty command line" }
    requireProcessStart(spec.workingDir?.exists() ?: true) {
        "Working directory '${spec.workingDir}' must exist"
    }
    requireProcessStart(spec.workingDir?.isDirectory ?: true) {
        "Working directory '${spec.workingDir}' must be a directory"
    }

    return withContext(Dispatchers.IO) {
        val process =
            try {
                ProcessBuilder(spec.commandLine)
                    .apply {
                        redirectInput(spec.inputProvider.toRedirect())
                        redirectOutput(spec.outputConsumer.toRedirect())
                        when {
                            spec.redirectErrorStream -> redirectErrorStream(true)
                            else -> redirectError(spec.errorConsumer.toRedirect())
                        }

                        environment().apply {
                            if (!spec.inheritEnvironment) clear()
                            putAll(spec.env)
                        }

                        spec.workingDir?.let { directory(it) }
                    }
                    .start()
            } catch (e: Exception) {
                requireProcessStart(e is IOException || e is UnsupportedOperationException) {
                    "Failed to start process: ${e.message}"
                }
                throw e
            }

        val inputJob = launch {
            when (val inputProvider = spec.inputProvider) {
                is InputProvider.Stream -> process.outputStream.use { inputProvider.handler(it) }
                else -> {}
            }
        }

        val deferredStandardOutput = async {
            spec.outputConsumer?.let { consumer ->
                when (consumer) {
                    is OutputConsumer.Stream<*> -> process.inputStream.use { consumer.handler(it) }
                    else -> Unit
                }
            } ?: Unit
        }

        val deferredErrorOutput = async {
            spec.errorConsumer
                ?.takeUnless { spec.redirectErrorStream }
                ?.let { consumer ->
                    when (consumer) {
                        is OutputConsumer.Stream<*> ->
                            process.errorStream.use { consumer.handler(it) }
                        else -> emptyList<String>()
                    }
                } ?: emptyList<String>()
        }

        try {
            // wait for background tasks

            // first wait for input to be finished
            inputJob.join()

            // then wait for output to be consumed
            val results = awaitAll(deferredStandardOutput, deferredErrorOutput)

            // then wait for process to exit
            val exitCode = runInterruptible { process.waitFor() }
            if (spec.failOnNonZeroExit && exitCode != 0) {
                throw ProcessFailedException(exitCode, results[1] as List<String>)
            }
            ExecResult(
                executableName = spec.commandLine[0],
                exitCode = exitCode,
                output = results[0] as O,
                error = results[1] as List<String>
            )
        } catch (e: CancellationException) {
            when {
                spec.destroyForcibly -> process.destroyForcibly()
                else -> process.destroy()
            }
            throw e
        }
    }
}

public abstract class KProcessException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

public class ProcessFailedException(
    public val exitCode: Int,
    public val errorOutput: List<String>
) : KProcessException("Process failed with exit code $exitCode; stderr=$errorOutput")

public class ProcessStartException(message: String) : KProcessException(message)
