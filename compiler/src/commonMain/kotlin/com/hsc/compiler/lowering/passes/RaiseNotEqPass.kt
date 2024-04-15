package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object RaiseNotEqPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val exprs = ctx.query<Expr>()
        exprs.forEach { expr ->
            raiseNotEq(expr)
        }
    }

}

private fun raiseNotEq(expr: Expr) {
    when (val kind = expr.kind) {
        is ExprKind.Binary -> {
            if (kind.kind == BinOpKind.Ne) {
                kind.kind = BinOpKind.Eq
                expr.kind = ExprKind.Unary(UnaryOpKind.Not, expr)
            }
        }
        else -> {}
    }
}