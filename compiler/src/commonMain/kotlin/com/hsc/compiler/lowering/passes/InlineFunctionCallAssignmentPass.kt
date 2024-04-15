package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
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
        functions.forEach {
            InlineFunctionCallAssignmentVisitor.visitItem(it)
        }
        InlineFunctionCallAssignmentVisitor.functionsUsedAsExpressions
            .map { it.first }
            .distinct()
            .forEach { name ->
                checkHasReturn(ctx, name, functions)
            }
        functions.forEach {
            val fn = (it.kind as ItemKind.Fn).fn
            fn.sig.args = emptyList() // Args are no more!
        }
    }

}

private object InlineFunctionCallAssignmentVisitor : BlockAwareVisitor() {

    val functionsUsedAsExpressions = mutableListOf<Pair<String, Span>>()
    var inlined = 0

    override fun visitStmt(stmt: Stmt) {
        inlined = 0
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                val stmt = currentBlock.stmts[currentPosition]
                when (val stmtKind = stmt.kind) {
                    is StmtKind.Expr -> {
                        if (stmtKind.expr == expr) {
                            super.visitExpr(expr)
                            return
                        }
                    }
                    else -> {}
                }
                val offset = if (inlined < 2) inlined
                else (inlined - 1) * 2 + 1

                val newStmt = Stmt(expr.span, StmtKind.Expr(expr.deepCopy()))
                currentBlock.stmts.add(currentPosition - offset, newStmt)
                val returnKind = ExprKind.Var(Ident.Player("_return"))
                if (inlined == 0) {
                    functionsUsedAsExpressions.add(Pair(kind.ident.name, expr.span))
                    expr.kind = returnKind
                    added(1)
                } else {
                    val ident = Ident.Player("_temp${inlined - 1}")
                    val newStmt2 = Stmt(expr.span, StmtKind.Assign(ident, Expr(expr.span, returnKind)))
                    currentBlock.stmts.add(currentPosition - offset + 1, newStmt2)
                    expr.kind = ExprKind.Var(ident)
                    added(2)
                }
                inlined++
            }
            else -> {}
        }
        super.visitExpr(expr)
    }

}

private class FindReturnVisitor : AstVisitor {

    var found = false

    override fun visitIdent(ident: Ident) {
        found = found || (ident.name == "_return" && !ident.isGlobal)
    }

}

private fun checkHasReturn(
    ctx: LoweringCtx,
    name: String,
    functions: List<Item>,
) {
    functions.find { it.ident.name == name }?.let {
        val visitor = FindReturnVisitor()
        visitor.visitItem(it)
        if (!visitor.found) {
            InlineFunctionCallAssignmentVisitor.functionsUsedAsExpressions
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