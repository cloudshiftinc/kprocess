package io.cloudshiftdev.kprocess

import java.io.File
import java.io.InputStream

public interface ExecSpec<O> {
    public fun commandLine(vararg args: String) {
        commandLine(args.toList())
    }

    public fun commandLine(args: Iterable<String>)

    public fun environment(env: Map<String, String>)

    public fun environmentVariable(key: String, value: String)

    public fun workingDir(dir: File)

    public fun outputConsumer(consumer: OutputConsumer)

    public fun consumeStream(consumer: suspend (InputStream) -> O) {
        outputConsumer(OutputConsumer.stream(consumer))
    }

    public fun consumeLineSequence(consumer: suspend (Sequence<String>) -> O) {
        outputConsumer(OutputConsumer.lines(consumer))
    }

    public fun discardOutput() {
        outputConsumer(OutputConsumer.discard())
    }

    public fun discardError() {
        errorConsumer(OutputConsumer.discard())
    }

    public fun inheritOutput() {
        outputConsumer(OutputConsumer.discard())
    }

    public fun inheritError() {
        errorConsumer(OutputConsumer.discard())
    }

    public fun errorConsumer(consumer: OutputConsumer)

    public fun inputProvider(provider: InputProvider)

    public fun inheritEnvironment(value: Boolean = true)

    public fun redirectErrorStream(value: Boolean = true)

    public fun destroyForcibly(value: Boolean = true)

    public fun failOnNonZeroExit(value: Boolean = true)
}

internal class ExecSpecImpl<O> : ExecSpec<O> {
    val commandLine: MutableList<String> = mutableListOf()
    val env: MutableMap<String, String> = mutableMapOf()
    var workingDir: File? = null
    var inputProvider: InputProvider = InputProvider.none()
    var outputConsumer: OutputConsumer? = null
    var errorConsumer: OutputConsumer? = null
    var inheritEnvironment: Boolean = true
    var redirectErrorStream: Boolean = false
    var destroyForcibly: Boolean = false
    var failOnNonZeroExit: Boolean = true

    override fun commandLine(args: Iterable<String>) {
        commandLine.addAll(args)
    }

    override fun environment(env: Map<String, String>) {
        this.env.putAll(env)
    }

    override fun environmentVariable(key: String, value: String) {
        env[key] = value
    }

    override fun workingDir(dir: File) {
        workingDir = dir
    }

    override fun outputConsumer(consumer: OutputConsumer) {
        require(outputConsumer == null) { "Output consumer already set" }
        outputConsumer = consumer
    }

    override fun errorConsumer(consumer: OutputConsumer) {
        require(errorConsumer == null) { "Error consumer already set" }
        errorConsumer = consumer
    }

    override fun inputProvider(provider: InputProvider) {
        inputProvider = provider
    }

    override fun inheritEnvironment(value: Boolean) {
        inheritEnvironment = value
    }

    override fun redirectErrorStream(value: Boolean) {
        redirectErrorStream = value
    }

    override fun destroyForcibly(value: Boolean) {
        destroyForcibly = value
    }

    override fun failOnNonZeroExit(value: Boolean) {
        failOnNonZeroExit = value
    }
}
