plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.hsc.mason"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    // implementation(project(":compiler"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    implementation("com.github.ajalt.clikt:clikt:4.2.2")
    implementation("com.github.ajalt.mordant:mordant:2.4.0")
    // ktoml has too many bugs... god bless peanuuutz
    implementation("net.peanuuutz.tomlkt:tomlkt:0.3.7")
}

tasks {
    jar {
        archiveBaseName = "Mason"
        manifest {
            attributes["Main-Class"] = "com.hsc.mason.MainKt"
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) })
    }
}