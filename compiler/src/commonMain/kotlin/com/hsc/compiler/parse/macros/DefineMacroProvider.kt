package com.hsc.compiler.parse.macros

import com.hsc.compiler.errors.Level
import com.hsc.compiler.parse.*
import com.hsc.compiler.span.Span

object DefineMacroProvider : MacroProvider {

    override val name: String = "#define"

    override fun invoke(lexer: Lexer): CharProvider = with(lexer) {

        val ident = floating {
            expect { isIdStart(it) } + takeWhile { isIdContinue(it) }
        }

        val args = mutableListOf<String>()
        if (eat('(')) {
            eatSpaces()
            // while next character is not closing paren (accounting for EOF)
            eatWhile { char ->
                if (char == ')') return@eatWhile false
                val arg = expect { isIdStart(it) } + takeWhile { isIdContinue(it) }
                args.add(arg)
                eatSpaces()
                if (!eat(',')) return@eatWhile false // No comma, exit loop
                eatSpaces()
                true
            }
            expect(')')
        }
        eatSpaces()

        val startPos = pos
        val src = if (eat('`')) {
            // backtick delimited macro
            takeWhile {
                if (it == '\n') srcp.addLine(pos + 1)
                it != '`'
            }.also { bump() }
        } else {
            takeWhile { it != '\n' }
        }
        println(src)

        srcp.addMacro(DefinedMacroProvider(ident, src, startPos, args))
        EmptyCharProvider
    }

    private fun Lexer.eatSpaces(): Unit = eatWhile { it.isWhitespace() && it != '\n' }

}

private class DefinedMacroProvider(
    override val name: String,
    private val src: String,
    private val srcOffset: Int,
    private val args: List<String>,
) : MacroProvider {
    override fun invoke(lexer: Lexer): CharProvider = with(lexer) {
        val params = mutableListOf<String>()
        if (first() == '(') { // It's fine to use parens even with 0 args
            val argStart = pos
            bump()
            eatWhitespace()
            var level = 1

            while (!(first() == ')' && level == 1) && !isEof()) {
                val arg = StringBuilder()
                while (first() != ',' && !isEof() && !(first() == ')' && level == 1)) {
                    if (first() == '(') level++
                    else if (first() == ')') level--
                    arg.append(bump())
                }

                if (arg.isEmpty()) {
                    val err = sess.dcx().err("unexpected token") // Best way to describe it I think...
                    err.span(Span.single(pos, fid))
                    throw err
                }

                params.add(arg.toString())
                if (first() == ',') bump() // Bump ,

                eatWhitespace()
            }
            bump() // Bump )

            if (params.size != args.size) {
                // Fuck grammar!!!
                val s1 = if (args.size == 1) "" else "s"
                val s2 = if (params.size == 1) "" else "s"
                val was = if (params.size == 1) "was" else "were"

                val err = sess.dcx().err("this macro takes ${args.size} parameter$s1 but ${params.size} parameter$s2 $was supplied")
                err.span(Span(argStart, pos - 1, fid))
                throw err
            }
        } else {
            if (args.isNotEmpty()) { // No invocation & args expected
                val err = sess.dcx().err("expected (")
                err.span(Span.single(pos - 1, fid))
                err.note(Level.Error, "expecting macro invocation")
                throw err
            }
        }

        val replaced = args.zip(params).fold(src) { src, (arg, value) ->
            src.replace("\${$arg}", value).replace(Regex("\\\$$arg\\b"), value)
        }

        StringCharProvider(replaced, srcOffset)
    }
}

private object EmptyCharProvider : CharProvider {
    override val srcOffset: Int = 0
    override var pos: Int = 0
    override fun next(): Char = error("")
    override fun hasNext(): Boolean = false
    override fun lookahead(count: Int): Char? = null
}