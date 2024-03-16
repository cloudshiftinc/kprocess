
![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/cloudshiftinc/kprocess/build.yaml?branch=main&style=plastic&label=Build%20-%20Main&cacheSeconds=300)
![Maven Central Version](https://img.shields.io/maven-central/v/io.cloudshiftdev.kprocess/kprocess?style=plastic&label=latest%20release&cacheSeconds=300)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.cloudshiftdev.kprocess/kprocess?server=https%3A%2F%2Fs01.oss.sonatype.org&style=plastic&label=latest%20snapshot&cacheSeconds=300)

# KProcess

Co-routine, DSL-friendly process launching from Kotlin.

## Usage

* Executing a process and capturing the results to a `List<String>`
```kotlin
    val result = execToList { commandLine("git", "version") }
    result.output.forEach { println(it) }
```

* Executing a process and processing standard output as a `Sequence<String>`
````kotlin
    val result = exec {
        commandLine("git", "version")
        consumeLineSequence { it.sumOf { it.length } }
    }
````
* Executing a process and writing standard output to a file
```kotlin
    execToFile(File("git.version")) { commandLine("git", "version") }
```

* Executing a process and process the standard output stream
```kotlin
    val result = exec {
        commandLine("git", "version")
        consumeStream { inputStream ->
            val file = File.createTempFile("git.", ".version")
            GZIPOutputStream(file.outputStream()).use { inputStream.copyTo(it) }
            file
        }
    }
```

## Key Features

* Non-zero exit values are considered a failure by default (configure `failOnNonZeroExit`) and throw an exception;
* Standard error is, by default, captured to a `List<String>`

## Getting Started

Gradle: 

![Maven Central Version](https://img.shields.io/maven-central/v/io.cloudshiftdev.kprocess/kprocess?style=plastic&label=latest%20release&cacheSeconds=300)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/io.cloudshiftdev.kprocess/kprocess?server=https%3A%2F%2Fs01.oss.sonatype.org&style=plastic&label=latest%20snapshot&cacheSeconds=300)
```kotlin

dependencies {
    implementation("io.cloudshiftdev.kprocess:kprocess:<version>")
}
```
