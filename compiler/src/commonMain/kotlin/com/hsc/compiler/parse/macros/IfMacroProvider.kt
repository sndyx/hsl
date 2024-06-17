package com.hsc.compiler.parse.macros

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.lower
import com.hsc.compiler.parse.*
import com.hsc.compiler.span.Span

object IfMacroProvider : MacroProvider {

    override val name: String = "#if"

    override fun invoke(lexer: Lexer): CharProvider {
        return conditionalProvider(lexer, "#if")
    }

}

object ElifMacroProvider : MacroProvider {

    override val name: String = "#elif"

    override fun invoke(lexer: Lexer): CharProvider {
        return conditionalProvider(lexer, "#elif")
    }

}

object ElseMacroProvider : MacroProvider {

    override val name: String = "#else"

    override fun invoke(lexer: Lexer): CharProvider {
        return conditionalProvider(lexer, "#else")
    }

}

// It's unfortunate we have to do it this way.
private var previousConditionalTerminated = true

private fun conditionalProvider(lexer: Lexer, type: String): CharProvider {
    val stream = TokenStream(lexer.iterator())

    fun parseSrc(): Pair<String, Int> {
        lexer.eatWhitespace()
        val next = lexer.bump()
        return if (next == '{') {
            var depth = 1
            val pos = lexer.pos
            Pair(lexer.takeWhile {
                if (it == '{') depth++
                if (it == '}') depth--
                !(it == '}' && depth == 0) // exit
            }, pos).also { lexer.bump() }
        } else {
            val pos = lexer.pos
            Pair(next.toString() + lexer.takeWhile { it != '\n' }, pos)
        }
    }

    if (type != "#else") {
        val parser = Parser(stream, lexer.sess)

        parser.expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val expr = parser.parseExpr()
        parser.expect(TokenKind.CloseDelim(Delimiter.Parenthesis))

        val (src, offset) = parseSrc()

        val stmt = Stmt(Span.none, StmtKind.Expr(expr))
        val exprItem = Item(Span.none, Ident.Player("#if_lcx"), ItemKind.Artificial(stmt))
        val lcx = LoweringCtx(Ast(mutableListOf(exprItem)), lexer.sess)
        lower(lcx)

        val success = when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> lit.value == 1L
                    is Lit.Bool -> lit.value
                    else -> false
                }
            }
            else -> {
                val err = lexer.sess.dcx().err("#if condition is not a compile time constant")
                err.spanLabel(expr.span, "this condition")
                err.emit()
                false
            }
        }

        if (success) {
            if (previousConditionalTerminated && type == "#elif") {
                return EmptyCharProvider
            }
            previousConditionalTerminated = true
            return StringCharProvider(src, offset)
        }

        previousConditionalTerminated = false

        return EmptyCharProvider
    } else {
        // #else, parseSrc and return
        val (src, offset) = parseSrc()

        if (previousConditionalTerminated) {
            return EmptyCharProvider
        } else {
            previousConditionalTerminated = true
            return StringCharProvider(src, offset)
        }
    }
}