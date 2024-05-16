package io.cloudshiftdev.kprocess

import java.io.File
import java.io.IOException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext

public suspend fun <O> exec(
    vararg commandAndArgs: String,
    spec: (ExecSpec<O>.() -> Unit)
): ExecResult<O> =
    ExecSpecImpl<O>()
        .apply {
            commandLine(commandAndArgs.toList())
            spec()
        }
        .let { execute(it) }

public suspend fun execToList(
    vararg commandAndArgs: String,
    spec: (ExecSpec<List<String>>.() -> Unit)
): ExecResult<List<String>> =
    exec(*commandAndArgs) {
        apply(spec)
        outputConsumer(OutputConsumer.lines { it.toList() })
    }

public suspend fun execToFile(
    file: File,
    vararg commandAndArgs: String,
    spec: (ExecSpec<Unit>.() -> Unit)
): ExecResult<Unit> =
    exec(*commandAndArgs) {
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

public class ProcessSpec(
    public val commandLine: List<String>,
    public val workingDir: File?,
    public val env: Map<String, String>,
    public val inheritEnvironment: Boolean,
    public val redirectErrorStream: Boolean,
    public val destroyForcibly: Boolean,
    public val failOnNonZeroExit: Boolean
) {
    override fun toString(): String {
        return "ProcessSpec(workingDir=$workingDir, commandLine=$commandLine, env=$env, inheritEnvironment=$inheritEnvironment, redirectErrorStream=$redirectErrorStream, destroyForcibly=$destroyForcibly, failOnNonZeroExit=$failOnNonZeroExit)"
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
                val builder = ProcessBuilder(spec.commandLine)
                val environment =
                    builder.environment().apply {
                        if (!spec.inheritEnvironment) clear()
                        putAll(spec.env)
                    }
                val processSpec =
                    ProcessSpec(
                        commandLine = spec.commandLine,
                        workingDir = spec.workingDir,
                        env = environment.toMap(),
                        inheritEnvironment = spec.inheritEnvironment,
                        redirectErrorStream = spec.redirectErrorStream,
                        destroyForcibly = spec.destroyForcibly,
                        failOnNonZeroExit = spec.failOnNonZeroExit
                    )

                spec.launchHandlers.forEach { it(processSpec) }

                builder
                    .apply {
                        redirectInput(spec.inputProvider.toRedirect())
                        redirectOutput(spec.outputConsumer.toRedirect())
                        when {
                            spec.redirectErrorStream -> redirectErrorStream(true)
                            else -> redirectError(spec.errorConsumer.toRedirect())
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

        val deferredStandardInput: Deferred<ProcessIOResult> = async {
            try {
                when (val inputProvider = spec.inputProvider) {
                    is InputProvider.Stream ->
                        process.outputStream.use { inputProvider.handler(it) }
                    else -> {}
                }
                ProcessIOResult.NoResult
            } catch (e: Exception) {
                ProcessIOResult.Failure(e)
            }
        }

        val deferredStandardOutput: Deferred<ProcessIOResult> = async {
            try {
                when (val consumer = spec.outputConsumer) {
                    is OutputConsumer.Stream<*> ->
                        ProcessIOResult.Success(process.inputStream.use { consumer.handler(it) })
                    else -> ProcessIOResult.NoResult
                }
            } catch (e: Exception) {
                ProcessIOResult.Failure(e)
            }
        }

        val deferredStandardError: Deferred<ProcessIOResult> = async {
            try {
                val consumer = spec.errorConsumer?.takeUnless { spec.redirectErrorStream }
                when (consumer) {
                    is OutputConsumer.Stream<*> ->
                        ProcessIOResult.Success(process.errorStream.use { consumer.handler(it) })
                    else -> ProcessIOResult.NoResult
                }
            } catch (e: Exception) {
                ProcessIOResult.Failure(e)
            }
        }

        try {
            val inputResult = deferredStandardInput.await()
            val outputResult = deferredStandardOutput.await()
            val errorResult = deferredStandardError.await()
            val results = listOf(inputResult, outputResult, errorResult)
            val failures = results.filterIsInstance<ProcessIOResult.Failure>()
            val errorOutput =
                when (errorResult) {
                    is ProcessIOResult.Success<*> -> errorResult.value as List<String>
                    else -> emptyList()
                }
            val stdout =
                when (outputResult) {
                    is ProcessIOResult.Success<*> -> outputResult.value
                    else -> null
                }

            // wait for process to exit
            val exitCode = runInterruptible { process.waitFor() }
            if (spec.failOnNonZeroExit && exitCode != 0) {
                val ex = ProcessFailedException(exitCode, errorOutput)
                failures.forEach { ex.addSuppressed(it.thrown) }
                throw ex
            }

            if (failures.isNotEmpty()) {
                throw ProcessIOException(failures.first().thrown)
            }

            ExecResult(
                executableName = spec.commandLine[0],
                exitCode = exitCode,
                output = stdout as O,
                error = errorOutput
            )
        } catch (e: Exception) {
            when {
                spec.destroyForcibly -> process.destroyForcibly()
                else -> process.destroy()
            }
            throw e
        }
    }
}

private sealed class ProcessIOResult {
    data object NoResult : ProcessIOResult()

    data class Success<T>(val value: T) : ProcessIOResult()

    data class Failure(val thrown: Throwable) : ProcessIOResult()
}

public abstract class KProcessException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

public class ProcessFailedException(
    public val exitCode: Int,
    public val errorOutput: List<String>
) : KProcessException("Process failed with exit code $exitCode; stderr=$errorOutput")

public class ProcessIOException(cause: Throwable) :
    KProcessException("Process input/output failure", cause)

public class ProcessStartException(message: String) : KProcessException(message)
