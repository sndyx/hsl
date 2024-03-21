plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.21"
}

group = "com.hsc.compiler"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.github.ajalt.mordant:mordant:2.4.0")
}

tasks {
    jar {
        archiveBaseName = "HSC"
        manifest {
            attributes["Main-Class"] = "com.hsc.compiler.MainKt"
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) })
    }
}