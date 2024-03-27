package com.hsc.mason

import com.github.ajalt.clikt.core.CliktCommand
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

class CleanCommand : CliktCommand() {

    override fun run() {
        if (FileSystem.SYSTEM.exists("build".toPath())) {
            FileSystem.SYSTEM.deleteRecursively("build".toPath())
        }
        if (FileSystem.SYSTEM.exists("House.lock".toPath())) {
            FileSystem.SYSTEM.delete("House.lock".toPath())
        }
    }

}