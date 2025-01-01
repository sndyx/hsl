package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getExprs
import com.hsc.compiler.parse.Lexer
import com.hsc.compiler.parse.Parser
import com.hsc.compiler.parse.SourceProvider
import com.hsc.compiler.parse.TokenStream
import com.hsc.compiler.span.SourceFile
import okio.Path.Companion.toPath

fun convertStrings(ctx: LoweringCtx) = with(ctx) {
    getExprs().forEach { expr ->
        val string = (expr.lit()?.lit as? Lit.Str)?.value ?: return@forEach

        expr.kind = parse(ctx, expr, string)
    }
}

fun parse(ctx: LoweringCtx, expr: Expr, raw: String): ExprKind {
    val parts = mutableListOf<Expr>() // Collect all parts (literals or expressions)
    val builder = StringBuilder()
    var i = 0

    fun flushBuilder() {
        parts.add(Expr(expr.span, ExprKind.Lit(Lit.Str(builder.toString()))))
        builder.clear()
    }

    flushBuilder()

    while (i < raw.length) {
        when {
            // Handle escaped templating: \${...}
            raw.substring(i).startsWith("\\\${", i) -> {
                builder.append("\${") // Append escaped sequence
                i += 3
            }

            // Handle templating: ${...}
            raw.substring(i).startsWith("\${", i) -> {
                flushBuilder() // Add any previous string as a literal
                i += 2
                val start = i
                var braceCount = 1 // Track nested braces

                while (i < raw.length && braceCount > 0) {
                    if (raw[i] == '{') braceCount++
                    else if (raw[i] == '}') braceCount--
                    i++
                }

                val code = raw.substring(start, i - 1)
                val sourceFile = SourceFile("".toPath(), code)
                val templateProvider = SourceProvider(sourceFile)
                val lexer = Lexer(ctx.sess, templateProvider)
                val stream = TokenStream(lexer.iterator())
                val parser = Parser(stream, ctx.sess)

                parts.add(parser.parseExpr())
            }

            // Handle normal characters
            else -> {
                builder.append(raw[i])
                i++
            }
        }
    }

    flushBuilder() // Add remaining text in the builder

    // Combine all parts into a binary addition chain
    return parts.reduce { acc, part ->
        Expr(expr.span, ExprKind.Binary(BinOpKind.Add, acc, part))
    }.kind
}