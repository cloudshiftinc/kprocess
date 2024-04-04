package io.cloudshiftdev.kprocess

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.File
import java.io.IOException

class KProcessTest : FunSpec() {
    init {
        test("execToList works") {
            val result = execToList { commandLine("git", "version") }

            result.output.shouldHaveSize(1)
            result.output[0].shouldStartWith("git version ")
            result.error.shouldBeEmpty()
        }

        test("exec using sequence works") {
            val result = exec {
                commandLine("git", "version")
                consumeLineSequence { it.toList() }
            }

            result.output.shouldHaveSize(1)
            result.output[0].shouldStartWith("git version ")
            result.error.shouldBeEmpty()
        }

        test("exec to stream works") {
            val result = exec {
                commandLine("git", "version")
                consumeStream { inputStream ->
                    val file = tempfile()
                    file.outputStream().use { inputStream.copyTo(it) }
                    file
                }
            }

            result.output.shouldBeInstanceOf<File>()
            result.output.readText().shouldStartWith("git version ")
        }

        test("bad command throws exception") {
            val e = shouldThrow<IOException> { execToList { commandLine("xxxgit", "version") } }

            e.message.shouldBe("Cannot run program \"xxxgit\": error=2, No such file or directory")
        }

        test("error output captured") {
            val e =
                shouldThrow<ProcessFailedException> { execToList { commandLine("git", "--xxx") } }

            e.message.shouldStartWith(
                "Process failed with exit code 129; stderr=[unknown option: --xxx,"
            )
        }

        test("launch handler works") {
            var processSpec: ProcessSpec? = null
            execToList {
                commandLine("git", "version")
                launchHandler {
                    processSpec = it
                    println(it) }
            }

            processSpec.shouldNotBeNull()
        }
    }
}
