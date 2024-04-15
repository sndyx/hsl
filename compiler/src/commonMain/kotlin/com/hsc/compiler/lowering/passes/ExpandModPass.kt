package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object ExpandModPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val visitor = ExpandModVisitor()
        ctx.query<Block>().forEach {
            visitor.visitBlock(it)
        }
        if (visitor.changed) ctx.clearQuery<Stmt>()
    }
}

private class ExpandModVisitor : BlockAwareVisitor() {

    var changed = false

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.AssignOp -> {
                if (kind.kind != BinOpKind.Rem) return
                val divStmt = Stmt(stmt.span, StmtKind.AssignOp(BinOpKind.Div, kind.ident, kind.expr))
                currentBlock.stmts.add(currentPosition, divStmt)
                kind.kind = BinOpKind.Mul
                added(1)
                changed = true
            }
            else -> {}
        }
        pass() // instead of visitStmt
    }

}