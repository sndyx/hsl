package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.firstAvailableTemp
import com.hsc.compiler.lowering.getFunctions
import com.hsc.compiler.lowering.walk
import com.hsc.compiler.span.Span

/**
 * A pass that inlines functions marked with the special #inline processor.
 *
 * eg:
 * ```
 * #inline
 * fn test() {
 *   message("Hello, world!")
 * }
 *
 * ...
 * message("Calling function")
 * test()
 * ```
 * becomes:
 * ```
 * message("Calling function")
 * message("Hello, world")
 * ```
 */
fun inlineFunctions(ctx: LoweringCtx) = with(ctx) {
    getFunctions().forEach { fn ->
        walk(fn) { expr ->
            val call = expr.call() ?: return@walk

            // find a function associated with this call, or return
            val callee = (ctx.query<Item>()
                .find { it.ident == call.ident }
                ?.kind as? ItemKind.Fn)?.fn ?: return@walk

            // make sure this is an inline function
            callee.processors?.list?.find { it == "inline" } ?: return@walk

            val body = callee.block.deepCopy()

            // backwards inline args
            callee.sig.args.forEachIndexed { index, arg ->
                // check if this arg is ever reassigned
                val isMutated = body.stmts
                    .map { it.assign()?.ident ?: it.assignOp()?.ident }
                    .any { it == arg }

                val param = call.args.args[index]

                val replacement = if (isMutated) {
                    // mutated, replace all instances with a temp variable
                    val ident = firstAvailableTemp(fn, expr)
                    body.stmts.add(0, Stmt(Span.none, StmtKind.Assign(ident, param)))
                    ExprKind.Var(ident)
                } else {
                    // not mutated, we can just insert the raw value
                    param.kind
                }

                walk(body) { expr ->
                    if (expr.variable()?.ident == arg) {
                        expr.kind = replacement
                    }
                }
            }

            expr.kind = ExprKind.Block(body)
        }
    }
}