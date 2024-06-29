plugins {
    kotlin("multiplatform") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
}

group = "com.hsc"
version = "1.2.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm() // Pretty sure this has to be here
}