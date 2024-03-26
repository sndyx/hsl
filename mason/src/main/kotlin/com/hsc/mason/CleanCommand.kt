package com.hsc.mason

import com.github.ajalt.clikt.core.CliktCommand
import kotlin.io.path.*

class CleanCommand : CliktCommand() {

    @OptIn(ExperimentalPathApi::class)
    override fun run() {
        if (Path("House.toml").exists()) {
            Path("build").deleteRecursively()
        }
        Path("House.lock").deleteIfExists()
    }

}