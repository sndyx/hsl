package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.firstAvailableTemp
import com.hsc.compiler.span.Span

/**
 * A pass that should receive flattened assign statements with no arguments.
 *
 * eg:
 * ```
 * x = function()
 * ```
 * becomes:
 * ```
 * function()
 * x = _return
 * ```
 */
object InlineFunctionCallAssignmentPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        val visitor = InlineFunctionCallAssignmentVisitor(ctx)
        functions.forEach {
            visitor.visitItem(it)
        }
        visitor.functionsUsedAsExpressions
            .map { it.first }
            .distinct()
            .forEach { name ->
                checkHasReturn(ctx, name, functions, visitor.functionsUsedAsExpressions)
            }

        functions.forEach {
            val fn = (it.kind as ItemKind.Fn).fn
            fn.sig.args = emptyList() // Args are no more!
        }
    }

}

private class InlineFunctionCallAssignmentVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    val functionsUsedAsExpressions = mutableListOf<Pair<String, Span>>()
    var inlined = 0

    var currentFn: Fn? = null

    override fun visitFn(fn: Fn) {
        currentFn = fn
        super.visitFn(fn)
    }

    override fun visitStmt(stmt: Stmt) {
        inlined = 0
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        val callExpr = expr.call() ?: return super.visitExpr(expr)

        val stmt = currentBlock.stmts[currentPosition]
        if (stmt.expr()?.expr == expr) {
            return super.visitExpr(expr)
        }

        val offset = if (inlined < 2) inlined
        else (inlined - 1) * 2 + 1

        val newStmt = Stmt(expr.span, StmtKind.Expr(expr.deepCopy()))
        currentBlock.stmts.add(currentPosition - offset, newStmt)
        val returnKind = ExprKind.Var(Ident.Player(ctx.sess.opts.tempPrefix + "return"))
        if (inlined == 0) {
            functionsUsedAsExpressions.add(Pair(callExpr.ident.name, expr.span))
            expr.kind = returnKind
            added(1)
        } else {
            val ident = firstAvailableTemp(ctx, currentFn!!, expr)
            val newStmt2 = Stmt(expr.span, StmtKind.Assign(ident, Expr(expr.span, returnKind)))
            currentBlock.stmts.add(currentPosition - offset + 1, newStmt2)
            expr.kind = ExprKind.Var(ident)
            added(2)
        }
        inlined++
    }

}

private class FindReturnVisitor(val ctx: LoweringCtx) : AstVisitor {

    var found = false

    override fun visitIdent(ident: Ident) {
        found = found || (ident.name == (ctx.sess.opts.tempPrefix + "return") && !ident.isGlobal)
    }

}

private fun checkHasReturn(
    ctx: LoweringCtx,
    name: String,
    functions: List<Item>,
    functionsUsedAsExpressions: List<Pair<String, Span>>
) {
    functions.find { it.ident.name == name }?.let {
        val visitor = FindReturnVisitor(ctx)
        visitor.visitItem(it)
        if (!visitor.found) {
            functionsUsedAsExpressions
                .filter { fn -> fn.first == name }
                .forEach { pair ->
                    val err = ctx.dcx().err("function with no `return` used as expression")
                    err.spanLabel(pair.second, "called here")
                    // Guys we did the funny span creation again
                    err.reference(
                        Span(it.span.lo, (it.kind as ItemKind.Fn).fn.sig.span.hi, it.span.fid),
                        "function declared here"
                    )
                    err.emit()
                }
        }
    }
}