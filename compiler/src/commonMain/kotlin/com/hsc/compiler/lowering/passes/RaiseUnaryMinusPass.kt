package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object RaiseUnaryMinusPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val exprs = ctx.query<Expr>()
        exprs.forEach { expr ->
            raiseUnaryMinus(expr)
        }
    }

}

private fun raiseUnaryMinus(expr: Expr) {
    when (val kind = expr.kind) {
        is ExprKind.Unary -> {
            expr.kind = ExprKind.Binary(BinOpKind.Mul, kind.expr, Expr(kind.expr.span, ExprKind.Lit(Lit.I64(-1))))
        }
        else -> {}
    }
}