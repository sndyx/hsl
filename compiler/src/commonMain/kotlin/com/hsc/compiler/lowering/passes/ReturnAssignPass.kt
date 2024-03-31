package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object ReturnAssignPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            ReturnAssignVisitor(ctx).visitItem(it)
        }
    }

}

private class ReturnAssignVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var flagNextStmt = false

    override fun visitBlock(block: Block) {
        super.visitBlock(block)
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
                val blockOwner = ctx.node<Any>(currentBlock.id.ownerId)

                // change _return to return value
                if (kind.expr != null) {
                    val assign = StmtKind.Assign(Ident(false, "_return"), kind.expr!!)
                    stmt.kind = assign // I believe this is fine?
                    currentBlock.stmts = currentBlock.stmts.take(currentPosition + 1).toMutableList()
                }
                if (blockOwner !is Item) {
                    // We are inside a conditional body, I think?
                    val exit = StmtKind.Expr(Expr(
                        NodeId.from(currentBlock.id),
                        Span.none,
                        ExprKind.Call(Ident(false, "exit"), Args(Span.none, mutableListOf()))
                    ))
                    currentBlock.stmts = currentBlock.stmts.take(currentPosition + 1).toMutableList()
                    currentBlock.stmts.add(Stmt(NodeId.from(currentBlock.id), Span.none, exit))
                }

                flagNextStmt = true
            }
            else -> super.visitStmt(stmt)
        }
    }

}