package com.hsc.mason

import kotlinx.coroutines.coroutineScope
import okio.Path

object Hsc {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            process("hsc --version")
        }.isSuccess
    }

    suspend fun compile(src: List<Path>, mode: Mode) = coroutineScope {
        val modeString = when (mode) {
            Mode.Normal -> ""
            Mode.Strict -> "--mode=strict"
            Mode.Optimize -> "--mode=optimize"
        }

        printProcess("hsc ${src.joinToString(" ")}")
    }

}