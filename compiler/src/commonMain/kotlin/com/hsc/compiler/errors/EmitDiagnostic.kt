package com.hsc.compiler.errors

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.bold
import com.github.ajalt.mordant.rendering.TextStyles.underline
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.span.SourceMap
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

fun Terminal.printDiagnostic(diagnostic: Diagnostic, sourceMap: SourceMap) {

    val (label, color) = style(diagnostic.level)

    println("${(bold + color)(label)}: ${bold(diagnostic.message)}")
    diagnostic.throwable?.stackTraceToString()
        ?.lines()
        ?.filter { it.contains("kfun:") }
        ?.drop(1)?.take(5)
        ?.map { bold(" -+> ") + it.split("kfun:").last() }
        ?.forEach { println(it) }
    diagnostic.spans.getOrNull(0)?.let { (span, _) ->
        sourceMap.files[span.fid]?.let { file ->

            val (line, col) = file.lookupPos(span.lo)
            val digits = log10(line.toDouble()).toInt() + 1

            print(" ".repeat(digits - 1))
            println(" ${bold("-->")} ${underline("${file.path}:$line:$col")}")
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
                print(" ".repeat(digits))
                if (line > 2) {
                    println(" ${bold(":")}")
                } else {
                    println(" ${bold("|")}")
                }
            } else if (line - prevLine > 2) {
                print(" ".repeat(digits))
                println(" ${bold(":")} ")
            }
            print(bold("$line"))
            print(" ".repeat(digits - curDigits))
            print(" ${bold("|")} ")
            println(
                text.replace(
                    Regex("\\s(else|enum|fn|for|if|in|match|while|break|continue|return|const|item)\\s"),
                    bold("\$0")
                ).trimEnd() // sneaky newline characters! :-(
            )
            print(" ".repeat(digits))
            print(" ${bold("|")} ")
            print(" ".repeat(max(0, col - 1)))
            if (!isReference) {
                print(color("^".repeat(max(1, hi - span.lo + 1))))
                msg?.let {
                    print(" ")
                    print(color(it))
                }
            } else {
                print(blue("-".repeat(max(1, hi - span.lo + 1))))
                msg?.let {
                    print(" ")
                    print(blue(it))
                }
            }
            println()
            idx++
            prevLine = line
        }
    }

    diagnostic.notes.forEach {
        val (l2, c2) = style(it.first)

        println("  ${c2(l2)}: ${bold(it.second)}")
    }
    println()
}

private fun style(level: Level) = when (level) {
    Level.Bug -> Pair("bug", red)
    Level.Error -> Pair("error", red)
    Level.Warning -> Pair("warning", yellow)
    Level.Hint -> Pair("hint", cyan)
    Level.Note -> Pair("note", white)
}