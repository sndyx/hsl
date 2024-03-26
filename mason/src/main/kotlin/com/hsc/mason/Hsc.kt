package com.hsc.mason

import kotlinx.coroutines.coroutineScope
import java.nio.file.Path

object Hsc {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            // process("hsc --version").await()
        }.isSuccess
    }

    suspend fun compile(src: List<Path>, mode: Mode) = coroutineScope {
        val modeString = when (mode) {
            Mode.Normal -> ""
            Mode.Strict -> "--mode=strict"
            Mode.Optimize -> "--mode=optimize"
        }

        val input = process("java -jar C:\\Users\\Sandy\\IdeaProjects\\hsc\\compiler\\build\\libs\\HSC-1.0.jar ${src.joinToString(" ")}").result

        var size = 0
        val buffer = ByteArray(1024)
        while ((input.read(buffer).also { size = it }) != -1) System.out.write(buffer, 0, size)
    }

}