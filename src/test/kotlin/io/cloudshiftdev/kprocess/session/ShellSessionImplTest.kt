package io.cloudshiftdev.kprocess.session

import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import org.junit.jupiter.api.Assertions.*

class ShellSessionImplTest : FunSpec() {
    init {
        test("shell session works") {
            val tmpDir = tempdir()
            shellSession(workingDir = tmpDir) {
                mkdir("test")
                cd("test")
                val dir = pwd()
                assertEquals(tmpDir.resolve("test"), dir)
            }
        }
    }
}
