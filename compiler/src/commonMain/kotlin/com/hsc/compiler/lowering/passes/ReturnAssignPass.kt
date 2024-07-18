package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object ReturnAssignPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val visitor = ReturnAssignVisitor(ctx)
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                visitor.visitItem(it)
            }
    }

}

private class ReturnAssignVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var flagNextStmt = false
    var depth = 0

    override fun visitBlock(block: Block) {
        depth++
        super.visitBlock(block)
        depth--
        flagNextStmt = false
    }

    // Whole lotta of question marks in this function I've noticed! :-)
    override fun visitStmt(stmt: Stmt) {
        if (flagNextStmt) {
            val err = ctx.dcx().warn("unreachable statement")
            err.span(stmt.span)
            err.emit()
        }
        when (val kind = stmt.kind) {
            is StmtKind.Ret -> {
                // change _return to return value
                if (kind.expr != null) {
                    val assign = StmtKind.Assign(Ident.Player(ctx.sess.opts.tempPrefix + "return"), kind.expr!!)
                    stmt.kind = assign // I believe this is fine?
                    currentBlock.stmts = currentBlock.stmts.take(currentPosition + 1).toMutableList()
                }
                if (depth > 1) {
                    // We are inside a conditional body, I think?
                    val exit = StmtKind.Expr(Expr(
                        Span.none,
                        ExprKind.Call(Ident.Player("exit"), Args(Span.none, mutableListOf()))
                    ))
                    currentBlock.stmts = currentBlock.stmts.take(currentPosition + 1).toMutableList()
                    currentBlock.stmts.add(Stmt(Span.none, exit))
                }

                flagNextStmt = true
            }
            else -> super.visitStmt(stmt)
        }
    }

}