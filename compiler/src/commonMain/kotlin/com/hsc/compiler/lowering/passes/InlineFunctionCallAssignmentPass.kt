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
                functions.find { it.ident.name == name }?.let {
                    val visitor = FindReturnVisitor()
                    visitor.visitItem(it)
                    if (!visitor.found) {
                        InlineFunctionCallAssignmentVisitor.functionsUsedAsExpressions
                            .filter { it.first == name }
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
        functions.forEach {
            val fn = (it.kind as ItemKind.Fn).fn
            fn.sig.args = emptyList() // Args are no more!
        }
    }

}

private object InlineFunctionCallAssignmentVisitor : BlockAwareVisitor() {

    val functionsUsedAsExpressions = mutableListOf<Pair<String, Span>>()

    override fun visitStmt(stmt: Stmt) {
        when (val assign = stmt.kind) {
            is StmtKind.Assign -> {
                when (val call = assign.expr.kind) {
                    is ExprKind.Call -> {
                        // insert call function before assign and change assign expr kind to var (_return)
                        val newStmt = Stmt(Span.none, StmtKind.Expr(assign.expr.deepCopy()))
                        currentBlock.stmts.add(currentPosition, newStmt)
                        functionsUsedAsExpressions.add(Pair(call.ident.name, assign.expr.span))
                        assign.expr.kind = ExprKind.Var(Ident(false, "_return"))
                        added(1)
                    }
                    else -> { }
                }
            }
            is StmtKind.AssignOp -> {
                // TOD0(NT)!!!!: MAKE THIS (NOT) WORK!!!! IT DOES(--)NT(--) FUCKIGN WORK! (HOORAY!!!)
                when (val call = assign.expr.kind) {
                    is ExprKind.Call -> {
                        val newStmt = Stmt(Span.none, StmtKind.Expr(assign.expr.copy()))
                        currentBlock.stmts.add(currentPosition, newStmt)
                        functionsUsedAsExpressions.add(Pair(call.ident.name, assign.expr.span))
                        assign.expr.kind = ExprKind.Var(Ident(false, "_return"))

                        println(currentBlock)
                        added(1)
                    }
                    else -> { }
                }
            }
            else -> { }
        }
        super.visitStmt(stmt) // Always do t
    }

}

private class FindReturnVisitor : AstVisitor {

    var found = false

    override fun visitIdent(ident: Ident) {
        found = found || (ident.name == "_return" && !ident.global)
    }

}
