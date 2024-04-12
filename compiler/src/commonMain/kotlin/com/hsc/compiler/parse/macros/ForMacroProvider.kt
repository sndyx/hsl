package com.hsc.compiler.parse.macros

import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import com.hsc.compiler.parse.*
import com.hsc.compiler.span.Span

object ForMacroProvider : MacroProvider {

    override val name: String = "#for"

    override fun invoke(lexer: Lexer): CharProvider = with(lexer) {
        floating { expect('(') }

        val ident = floating {
            expect { isIdStart(it) } + takeWhile { isIdContinue(it) }
        }

        floating {
            val lo = pos
            val kw = takeWhile { isIdContinue(it) }
            if (kw != "in") {
                throw sess.dcx().err("expected in keyword, found $kw", Span(lo, pos, fid))
            }
        }

        val range = floating {
            val lo = pos + 1
            try {
                val a = expectNumber()
                eatWhitespace()
                if (!(eat('.') && eat('.'))) {
                    throw sess.dcx().err("expected range", Span(lo, pos - 1, fid))
                }
                eatWhitespace()
                val b = expectNumber()
                a..b
            } catch (err: Diagnostic) {
                err.note(Level.Hint, "ranges are declared `<low>..<high>`")
                throw err
            }
        }

        floating { expect(')') }

        eatWhitespace()
        expect('{')
        val srcOffset = pos

        var depth = 1
        val src = takeWhile {
            if (it == '{') depth++
            if (it == '}') depth--
            !(it == '}' && depth == 0) // exit
        }
        bump() // Final closing }, macro should be fully parsed

        return ForCharProvider(srcOffset, range, ident, src)
    }

}

private class ForCharProvider(
    override val srcOffset: Int,
    range: LongRange,
    val ident: String,
    val src: String,
) : CharProvider {

    val values = range.toMutableList()
    var currentSrc: String? = null
    var nextSrc: String? = null
    init { bump(); bump() }

    override var pos: Int = 0

    override fun next(): Char {
        if (currentSrc?.length == pos) bump()
        return currentSrc!![pos++]
    }

    override fun hasNext(): Boolean = lookahead(0) != null

    override fun lookahead(count: Int): Char? {
        return currentSrc?.let {
            it.getOrNull(pos + count)
                ?: nextSrc?.getOrNull(count + (pos - it.length))
        }
    }

    fun bump() {
        currentSrc = nextSrc
        nextSrc = values.removeFirstOrNull()?.let {
            src.replace("\${$ident}", "$it")
                .replace(Regex("\\\$$ident\\b"), "$it")
        }
        pos = 0
    }

}