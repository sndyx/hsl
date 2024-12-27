package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.firstAvailableTemp
import com.hsc.compiler.lowering.getFunctionItems
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
            callee.processors?.list?.find { it.ident == "inline" } ?: return@walk

            val body = callee.block.deepCopy()

            object : AstVisitor {
                override fun visitStmt(stmt: Stmt) {
                    stmt.ret()?.let { ret ->
                        stmt.kind = StmtKind.Expr(ret.expr!!)
                    }
                }
            }.visitBlock(body)

            // backwards inline args
            callee.sig.args.forEachIndexed { index, arg ->
                // check if this arg is ever reassigned
                val isMutated = body.stmts
                    .map { it.assign()?.ident ?: it.assignOp()?.ident }
                    .any { it == arg }

                val param = call.args.args[index]

                val replacement = if (isMutated && param.variable() == null) {
                    // mutated, replace all instances with a temp variable
                    val ident = firstAvailableTemp(fn, expr)
                    body.stmts.add(0, Stmt(Span.none, StmtKind.Assign(ident, param)))
                    ExprKind.Var(ident)
                } else {
                    // not mutated, we can just insert the raw value
                    param.kind
                }

                object : AstVisitor {
                    override fun visitExpr(expr: Expr) {
                        expr.variable()?.let { variable ->
                            if (variable.ident == arg) expr.kind = replacement
                        }
                        super.visitExpr(expr)
                    }

                    override fun visitStmt(stmt: Stmt) {
                        stmt.assign()?.let { assign ->
                            if (assign.ident == arg) assign.ident = (replacement as ExprKind.Var).ident
                        }
                        stmt.assignOp()?.let { assignOp ->
                            if (assignOp.ident == arg) assignOp.ident = (replacement as ExprKind.Var).ident
                        }
                        super.visitStmt(stmt)
                    }
                }.visitBlock(body)
            }

            expr.kind = ExprKind.Block(body)
        }
    }

    getFunctionItems().forEach { (item, fn) ->
        if (fn.processors?.list?.any { it.ident == "inline" } == true) {
            ctx.ast.items.remove(item)
        }
    }

    // for legacy passes, unfortunately
    clearQuery<Expr>()
    clearQuery<Stmt>()
    clearQuery<Block>()
    clearQuery<Item>()
    clearQuery<Fn>()
    clearQuery<Lit>()
}