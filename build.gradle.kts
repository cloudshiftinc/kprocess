import com.gradle.scan.agent.serialization.scan.serializer.kryo.ja
import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.jetbrains.binaryCompatibilityValidator)
    alias(libs.plugins.vanniktech.maven.publish)
}

dependencies {
    implementation(libs.kotlinx.coroutines)
}

ktfmt {
    kotlinLangStyle()
}

java {
    consistentResolution { useCompileClasspathVersions() }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01, true)
    signAllPublications()

    pom {
        name = "KProcess"
        description = "Kotlin Process Launcher"
        inceptionYear = "2023"
        url = "https://github.com/cloudshiftinc/kprocess"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "cloudshiftchris"
                name = "Chris Lee"
                email = "chris@cloudshiftconsulting.com"
            }
        }
        scm {
            connection = "scm:git:git://github.com/cloudshiftinc/kprocess.git"
            developerConnection = "scm:git:https://github.com/cloudshiftinc/kprocess.git"
            url = "https://github.com/cloudshiftinc/kprocess"
        }
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(platform(libs.kotest.bom))
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.runner.junit5)
            }
            targets {
                all {
                    testTask.configure {
                        outputs.upToDateWhen { false }
                        testLogging {
                            events =
                                setOf(
                                    TestLogEvent.FAILED,
                                    TestLogEvent.PASSED,
                                    TestLogEvent.SKIPPED,
                                    TestLogEvent.STANDARD_OUT,
                                    TestLogEvent.STANDARD_ERROR
                                )
                            exceptionFormat = TestExceptionFormat.FULL
                            showExceptions = true
                            showCauses = true
                            showStackTraces = true
                        }
                    }
                }
            }
        }
    }
}


val ciBuild = System.getenv("CI") != null

val precommit = tasks.register("precommit") {
    group = "verification"
    dependsOn("check", "ktfmtFormat", "apiDump")
}

// only check formatting for CI builds
tasks.withType<KtfmtCheckTask>().configureEach {
    enabled = ciBuild
}

// always format for non-CI builds
tasks.withType<KtfmtFormatTask>().configureEach {
    enabled = !ciBuild
}

tasks.named("apiCheck") {
    mustRunAfter("apiDump")
}
