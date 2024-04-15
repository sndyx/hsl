package com.hsc.mason.proc

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.mason.Mode
import kotlinx.coroutines.coroutineScope
import okio.Path

object Hsc {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            process("hsc --version")
        }.isSuccess
    }

    suspend fun compile(src: List<Path>, out: Path, mode: Mode) = coroutineScope {
        val t = Terminal()

        val modeString = when (mode) {
            Mode.Normal -> ""
            Mode.Strict -> "--mode=strict"
            Mode.Optimize -> "--mode=optimize"
        }

        val colorString = when (t.info.ansiLevel) {
            AnsiLevel.NONE -> ""
            else -> "--force-color"
        }

        printProcess("hsc ${src.joinToString(" ")} $modeString --color=always --parallel --output $out")
    }

}