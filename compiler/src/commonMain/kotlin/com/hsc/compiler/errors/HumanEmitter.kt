package com.hsc.compiler.errors

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.span.SourceMap
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration

class HumanEmitter(
    private val t: Terminal,
    private val sourceMap: SourceMap
) : Emitter() {

    override fun print(diagnostic: Diagnostic) {

        val (label, color) = style(diagnostic.level)

        t.println("${(bold + color)(label)}: ${bold(italicizeBackticks(diagnostic.message))}")
        diagnostic.throwable?.stackTraceToString()
            ?.lines()
            ?.drop(1)?.take(10)
            ?.map { it.replace("\tat", bold(" -+>")) }
            ?.forEach { t.println(it) }
        diagnostic.spans.getOrNull(0)?.let { (span, _) ->
            sourceMap.files[span.fid]?.let { file ->

                val (line, col) = file.lookupPos(span.lo)
                val digits = log10(line.toDouble()).toInt() + 1

                t.print(" ".repeat(digits - 1))
                t.println(" ${bold("-->")} ${underline("${file.path}:$line:$col")}")
            }
        }

        var idx = 0
        var prevLine = 0

        val positions = diagnostic.spans.map { (span, _, _) ->
            sourceMap.files[span.fid]?.lookupPos(span.lo) ?: Pair(0, 0)
        }

        val digits = log10((positions.maxOfOrNull { it.first } ?: 0).toDouble()).toInt() + 1

        diagnostic.spans.forEach { (span, isReference, msg) ->
            sourceMap.files[span.fid]?.let { file ->

                val (line, col) = positions[idx]

                val text = file.getLine(line)
                // Recomputed span hi that ends when line ends
                val hi = min(span.hi, span.lo + text.length - col)

                val curDigits = log10(line.toDouble()).toInt() + 1

                if (idx == 0) {
                    t.print(" ".repeat(digits))
                    if (line > 2) {
                        t.println(" ${bold(":")}")
                    } else {
                        t.println(" ${bold("|")}")
                    }
                } else if (line - prevLine > 2) {
                    t.print(" ".repeat(digits))
                    t.println(" ${bold(":")} ")
                }
                t.print(bold("$line"))
                t.print(" ".repeat(digits - curDigits))
                t.print(" ${bold("|")} ")
                t.println(
                    text.replace(
                        Regex("\\s(else|enum|fn|for|if|in|match|while|break|continue|return|const)\\s"),
                        bold("\$0")
                    ).trimEnd() // sneaky newline characters! :-(
                )
                t.print(" ".repeat(digits))
                t.print(" ${bold("|")} ")
                t.print(" ".repeat(max(0, col - 1)))
                if (!isReference) {
                    t.print(color("^".repeat(max(1, hi - span.lo + 1))))
                    msg?.let {
                        t.print(" ")
                        t.print(color(it))
                    }
                } else {
                    t.print(blue("-".repeat(max(1, hi - span.lo + 1))))
                    msg?.let {
                        t.print(" ")
                        t.print(blue(italicizeBackticks(it)))
                    }
                }
                t.println()
                idx++
                prevLine = line
            }
        }

        diagnostic.notes.forEach {
            val (l2, c2) = style(it.first)

            t.println("  ${c2(l2)}: ${bold(italicizeBackticks(it.second))}")
        }
        t.println()
    }

    private fun italicizeBackticks(str: String): String {
        return str
    }

    override fun start(name: String) {
        t.println("${(green + bold)("compiling:")} ${bold(name)}")
    }

    override fun complete(name: String, time: Duration) {
        t.println("${(green + bold)("complete:")} ${bold(name)} successfully compiled in ${italic("${time.inWholeMilliseconds}ms")}")
    }

    override fun failed(name: String) {
        t.println("${(red + bold)("failed:")} ${bold("could not compile $name")}")
    }

    override fun pass(name: String, time: Duration) {
        t.println("${(white + bold)("pass:")} ${bold(name)} completed in ${italic("${time.inWholeMilliseconds}ms")}")
    }

    override fun close() { }

    private fun style(level: Level) = when (level) {
        Level.Bug -> Pair("bug", red)
        Level.Error -> Pair("error", red)
        Level.Warning -> Pair("warning", yellow)
        Level.Hint -> Pair("hint", cyan)
        Level.Note -> Pair("note", white)
    }

}