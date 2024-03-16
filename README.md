
![Maven Central](https://img.shields.io/maven-central/v/io.cloudshiftdev.kprocess/kprocess)
![GitHub](https://img.shields.io/github/license/cloudshiftinc/kprocess)

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
```kotlin

dependencies {
    implementation("io.cloudshiftdev.kprocess:kprocess:<version>")
}
```
