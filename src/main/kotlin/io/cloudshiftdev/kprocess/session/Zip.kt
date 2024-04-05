package io.cloudshiftdev.kprocess.session

public suspend fun ShellSession.zip(vararg args: String) {
    exec<Unit>("zip", *args)
}

public suspend fun ShellSession.unzip(vararg args: String) {
    exec<Unit>("unzip", *args)
}
