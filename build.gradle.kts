plugins {
    kotlin("multiplatform") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

group = "com.hsc"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm() // Pretty sure this has to be here
}