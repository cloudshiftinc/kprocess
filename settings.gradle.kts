/*
 * Copyright 2023-2024 the original author or authors.
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

rootProject.name = "kprocess"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    require(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17)) {
        "This build requires Gradle to be run with at least Java 17"
    }
    repositories { maven("https://cache-redirector.jetbrains.com/plugins.gradle.org") }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
    id("com.gradle.develocity") version "3.17.5"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2") }
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv("CI") != null }
        termsOfUseUrl = "https://gradle.com/terms-of-service"
        termsOfUseAgree = "yes"
    }
}

