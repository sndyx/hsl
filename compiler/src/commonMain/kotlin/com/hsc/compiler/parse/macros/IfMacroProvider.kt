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
        val stream = TokenStream(lexer.iterator())
        val parser = Parser(stream, lexer.sess)

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

        val conditions = with(parser) {
            buildList {
                var i = 0
                do {
                    expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
                    val expr = parseExpr()
                    expect(TokenKind.CloseDelim(Delimiter.Parenthesis))

                    val (src, offset) = parseSrc()
                    add(Triple(expr, src, offset))
                    i++
                } while (eat(TokenKind.Ident("#elif")))
                if (parser.eat(TokenKind.Ident("#else"))) {
                    val (src, offset) = parseSrc()
                    add(Triple(Expr(Span.none, ExprKind.Lit(Lit.Bool(true))), src, offset))
                }
            }
        }

        for ((expr, src, srcOffset) in conditions) {
            val stmt = Stmt(Span.none, StmtKind.Expr(expr))
            val exprItem = Item(Span.none, Ident.Player("#if_lcx"), ItemKind.Artificial(stmt))
            val lcx = LoweringCtx(Ast(mutableListOf(exprItem)), lexer.sess)
            lower(lcx)

            val success = when (val kind = expr.kind) {
                is ExprKind.Lit -> {
                    when (val lit = kind.lit) {
                        is Lit.I64 -> {
                            lit.value == 1L
                        }
                        is Lit.Bool -> {
                            lit.value
                        }
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
                return StringCharProvider(src, srcOffset)
            }
        }

        return EmptyCharProvider
    }

}