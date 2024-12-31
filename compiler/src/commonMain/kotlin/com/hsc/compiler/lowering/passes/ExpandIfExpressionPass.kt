package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getFunctions

object ExpandIfExpressionPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Stmt>()
            .filter { it.kind is StmtKind.Assign || it.kind is StmtKind.AssignOp }
            .forEach {
                var binOp: BinOpKind? = null
                val (ident, expr) =
                    if (it.kind is StmtKind.Assign) (it.kind as StmtKind.Assign).let { Pair(it.ident, it.expr) }
                    else (it.kind as StmtKind.AssignOp).let { binOp = it.kind; Pair(it.ident, it.expr) }

                if (expr.kind !is ExprKind.If) return@forEach
                val ifKind = (expr.kind as ExprKind.If)

                if (ifKind.other == null) {
                    throw ctx.dcx().err("if used as expression without else", it.span)
                }

                setAssign(ctx, ifKind.block, ident, binOp)
                setAssign(ctx, ifKind.other!!, ident, binOp)

                it.kind = StmtKind.Expr(expr)
            }

        ctx.clearQuery<Stmt>()
        ctx.clearQuery<Expr>()
    }

}

private fun setAssign(ctx: LoweringCtx, block: Block, ident: Ident, binOp: BinOpKind?) {
    val lastStmt = block.stmts.lastOrNull()
    if (lastStmt == null || lastStmt.kind !is StmtKind.Expr) {
        val err = ctx.dcx().err("expected value")
        err.spanLabel(block.span, "block used as expression must end with one")
        throw err
    }

    val expr = (lastStmt.kind as StmtKind.Expr).expr

    if (binOp == null) {
        lastStmt.kind = StmtKind.Assign(ident, expr)
    } else {
        lastStmt.kind = StmtKind.AssignOp(binOp, ident, expr)
    }
}
