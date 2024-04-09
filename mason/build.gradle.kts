import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

group = "com.hsc.mason"
version = "1.0"

repositories {
    mavenCentral()
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
    jvm {
        withJava()
        mainRun {
            args("build", "C:\\Users\\Sandy\\IdeaProjects\\hsc\\examples\\simple")
            mainClass.set("com.hsc.mason.MainKt")
        }
    }
    mingwX64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "mason"
                entryPoint = "com.hsc.mason.main"
            }
        }
    }
    linuxX64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "mason"
                entryPoint = "com.hsc.mason.main"
            }
        }
    }
    macosX64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "mason"
                entryPoint = "com.hsc.mason.main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable(listOf(NativeBuildType.RELEASE)) {
                baseName = "mason"
                entryPoint = "com.hsc.mason.main"
            }
        }
    }

    sourceSets {
        val commonMain by sourceSets.getting {
            dependencies {
                implementation("com.squareup.okio:okio:3.9.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                implementation("com.github.ajalt.clikt:clikt:4.2.2")
                implementation("com.github.ajalt.mordant:mordant:2.4.0")
                // ktoml has too many bugs... god bless peanuuutz
                implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
            }
        }
        val jvmMain by sourceSets.getting {
            dependencies {
                runtimeOnly(project(":compiler"))
            }
        }

        val posixMain by sourceSets.creating {
            dependsOn(commonMain)
        }
        val mingwX64Main by getting {
            // cannot depend on posixMain thanks to _popen! :-)
        }
        val linuxX64Main by getting {
            dependsOn(posixMain)
        }
        val macosX64Main by getting {
            dependsOn(posixMain)
        }
        val macosArm64Main by getting {
            dependsOn(posixMain)
        }
    }

}