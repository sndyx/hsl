package com.hsc.mason.proc

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.mason.House
import com.hsc.mason.Mode
import com.hsc.mason.Target
import kotlinx.coroutines.coroutineScope
import okio.Path

object Hsc {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            process("hsc --version")
        }.isSuccess
    }

    suspend fun compile(src: List<Path>, out: Path, config: House, run: Boolean) = coroutineScope {
        val t = Terminal()

        val modeString = when (config.pkg.mode ?: Mode.Normal) {
            Mode.Normal -> ""
            Mode.Strict -> "--mode=strict"
            Mode.Optimize -> "--mode=optimize"
        }

        val targetString = when (config.pkg.target ?: Target.Json) {
            Target.Json -> ""
            Target.Htsl -> "--target=htsl"
        }

        printProcess(
            "hsc ${src.joinToString(" ")}" +
                    " $modeString" +
                    " --house-name=\"${sanitize(config.pkg.name)}\"" +
                    " --color=always" +
                    " $targetString" +
                    " --output $out" +
                    if (run) " --driver=interpreter " else "" +
                    " " + (config.pkg.flags?.joinToString(" ") { sanitizeStrict(it) } ?: "")
        )
    }

}