package io.cloudshiftdev.kprocess.session

import io.cloudshiftdev.kprocess.ExecResult
import io.cloudshiftdev.kprocess.ExecSpec
import java.io.File

public interface ShellSession {
    public suspend fun changeDirectory(dir: File)

    public suspend fun cd(dir: File): Unit = changeDirectory(dir)

    public suspend fun cd(dir: String): Unit = changeDirectory(File(dir))

    public suspend fun currentDirectory(): File

    public suspend fun cwd(): File = currentDirectory()

    public suspend fun pwd(): File = currentDirectory()

    public suspend fun mkdir(vararg args: String)

    public suspend fun chmod(vararg args: String)

    public suspend fun <O> exec(
        vararg commandAndArgs: String,
        spec: (ExecSpec<O>.() -> Unit) = {}
    ): ExecResult<O>
}

public suspend fun <T> shellSession(
    workingDir: File? = null,
    environment: Map<String, String> = emptyMap(),
    session: suspend ShellSession.() -> T
): T {
    return ShellSessionImpl(workingDir, environment).session()
}

private class ShellSessionImpl(initialWorkingDir: File?, environment: Map<String, String>) :
    ShellSession {
    private var workingDir: File = initialWorkingDir ?: File(System.getProperty("user.dir"))
    private val environment: MutableMap<String, String> = environment.toMutableMap()

    override suspend fun changeDirectory(dir: File) {
        workingDir = resolveDir(dir)
    }

    override suspend fun currentDirectory(): File = workingDir

    override suspend fun mkdir(vararg args: String) {
        exec<Unit>("mkdir", *args) { failOnNonZeroExit(false) }
    }

    override suspend fun chmod(vararg args: String) {
        exec<Unit>("chmod", *args)
    }

    override suspend fun <O> exec(
        vararg commandAndArgs: String,
        spec: ExecSpec<O>.() -> Unit
    ): ExecResult<O> {
        return io.cloudshiftdev.kprocess.exec(*commandAndArgs) {
            workingDir(workingDir)
            environment(environment)
            apply(spec)
        }
    }

    private fun resolveDir(dir: File): File =
        if (dir.isAbsolute) dir else workingDir.resolve(dir).normalize()
}
