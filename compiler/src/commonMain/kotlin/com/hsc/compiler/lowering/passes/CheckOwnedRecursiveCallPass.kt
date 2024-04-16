package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object CheckOwnedRecursiveCallPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            val span = Span(it.span.lo, (it.kind as ItemKind.Fn).fn.sig.span.hi, it.span.fid)
            OwnedRecursiveCallVisitor(ctx, it.ident, functions, span).visitItem(it)
        }
    }

}

private class OwnedRecursiveCallVisitor(
    val ctx: LoweringCtx,
    val ident: Ident,
    val functions: List<Item>,
    val itemSpan: Span,
) : AstVisitor {
    var pop = false

    override fun visitBlock(block: Block) {
        for (stmt in block.stmts) {
            if (pop) break
            visitStmt(stmt)
        }
        pop = false
    }

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.Action -> {
                if (kind.action is Action.PauseExecution) {
                    pop = true
                    return
                }
            }
            is StmtKind.Expr -> {
                when (val exprKind = kind.expr.kind) {
                    is ExprKind.Call -> {
                        if (exprKind.ident.isGlobal) return
                        if (exprKind.ident == ident) {
                            val warn = ctx.dcx().warn("recursive call without pause")
                            warn.reference(itemSpan, "for this function")
                            warn.spanLabel(stmt.span, "recursive call here")
                            warn.emit()
                            return
                        }
                        functions.find { it.ident == exprKind.ident }?.let {
                            visitItem(it)
                        }
                    }
                    else -> {}
                }
            }
            else -> {}
        }
    }
}