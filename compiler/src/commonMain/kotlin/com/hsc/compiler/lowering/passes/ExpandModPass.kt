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

    // x % y ->
    // x - x / y * y
    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                if (kind.kind != BinOpKind.Rem) return

                kind.kind = BinOpKind.Sub
                val x = kind.a.kind
                val y = kind.b.kind
                kind.b.kind = ExprKind.Binary(BinOpKind.Mul,
                    Expr(expr.span, ExprKind.Binary(
                        BinOpKind.Div, Expr(expr.span, x.deepCopy()), Expr(expr.span, y.deepCopy())
                    )), Expr(expr.span, y.deepCopy())
                )
            }
            else -> {}
        }
        pass()
    }

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.AssignOp -> {
                if (kind.kind != BinOpKind.Rem) return

                stmt.kind = StmtKind.Assign(kind.ident, Expr(stmt.span,
                    ExprKind.Binary(BinOpKind.Rem, Expr(stmt.span, ExprKind.Var(kind.ident)), kind.expr)
                ))
            }
            else -> {}
        }
        super.visitStmt(stmt)
    }

}