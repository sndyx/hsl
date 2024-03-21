package com.hsc.compiler.parse

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.errors.CompileException
import com.hsc.compiler.errors.Level
import com.hsc.compiler.span.Span

class Preprocessor(private val sess: CompileSess, private val srcp: SourceProvider) {

    private val src = srcp.src
    private var pos = 0

    fun run() {
        takeWhile {
            if (it == '\n') srcp.addLine(pos + 1)
            it.isWhitespace()
        }
        while (take(8) == "#define ") {
            pos += 8 // bump
            eatSpaces()
            val ident = takeWhile { it.isLetterOrDigit() || it == '_' }
            if (ident.isEmpty()) expectedIdent()

            val args = mutableListOf<String>()
            if (bump() == '(') {
                eatSpaces()
                // while next character is not closing paren (accounting for EOF)
                while (src.getOrNull(pos)?.let { it != ')' } == true) {
                    val arg = takeWhile { it.isLetterOrDigit() || it == '_' }
                    if (arg.isEmpty()) expectedIdent()
                    args.add(arg)
                    eatSpaces()
                    if (src[pos] != ',') break // No comma, exit loop
                    else bump()
                    eatSpaces()
                }
                if (bump() != ')') expectedClosingParen()
            }
            eatSpaces()

            val src = if (src.getOrNull(pos) == '`') {
                // backtick delimited macro
                bump()
                takeWhile {
                    if (it == '\n') srcp.addLine(pos + 1)
                    it != '`'
                }.also { bump() }
            } else {
                takeWhile { it != '\n' }
            }

            // Done parsing this macro, add to source provider
            val provider = MacroProvider(ident, src, args)
            srcp.addMacro(provider)


            takeWhile {
                if (it == '\n') srcp.addLine(pos + 1)
                it.isWhitespace()
            }
        }
        // Offset source provider up to what we've already parsed to avoid parse errors.
        // The parser unfortunately does not handle/skip macros at this time which is quite annoying, but
        // maybe that can be implemented later to allow for macros anywhere in the file.
        srcp.pos = pos
    }

    private fun bump(): Char? {
        return src.getOrNull(pos++)
    }

    private fun take(n: Int): String? {
        return if (src.length > pos + n) {
            src.substring(pos, pos + n)
        } else null
    }

    private fun takeWhile(predicate: (Char) -> Boolean): String {
        val sb = StringBuilder()
        while (!isEof() && predicate(src[pos])) {
            sb.append(bump())
        }
        return sb.toString()
    }

    private fun eatSpaces() {
        takeWhile { it == ' ' }
    }

    private fun isEof(): Boolean = src.length <= pos

    private fun expectedIdent() {
        val err = sess.dcx().err("expected ident")
        err.span(Span.single(pos, srcp.fid))
        err.note(Level.Hint, "macros are declared `#define NAME(args...) <value>`")
        throw CompileException(err)
    }

    private fun expectedClosingParen() {
        // Hesitant to get any more descriptive due to the nature of this error
        // (it will usually be the result of missing a comma, I believe)
        val err = sess.dcx().err("expected )")
        err.span(Span.single(pos, srcp.fid))
        throw CompileException(err)
    }

}