import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "com.hsc.compiler"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {

    jvm {
        withJava()
    }
    mingwX64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "hsc"
                entryPoint = "com.hsc.compiler.main"
            }
        }
    }
    linuxX64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "hsc"
                entryPoint = "com.hsc.compiler.main"
            }
        }
    }
    macosX64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "hsc"
                entryPoint = "com.hsc.compiler.main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "hsc"
                entryPoint = "com.hsc.compiler.main"
            }
        }
    }

    sourceSets {
        // Kotlin Multiplatform? More like: Kotlin other-people-Multi and I freeload platform!!!!
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0-RC.2")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.2")
            implementation("com.github.ajalt.clikt:clikt:4.2.2")
            implementation("com.github.ajalt.mordant:mordant:2.4.0")
        }
    }

    tasks.withType<Jar> {
        doFirst {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            val main by kotlin.jvm().compilations.getting
            manifest {
                attributes(
                    "Main-Class" to "com.hsc.compiler.MainKt",
                )
            }
            from({
                main.runtimeDependencyFiles.files.filter { it.name.endsWith("jar") }.map { zipTree(it) }
            })
        }
    }


}