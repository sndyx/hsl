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
    if (expr.unary()?.kind?.equals(UnaryOpKind.Neg) == true) {
        expr.kind = ExprKind.Binary(BinOpKind.Mul, expr.unary()!!.expr, Expr(expr.unary()!!.expr.span, ExprKind.Lit(Lit.I64(-1))))
    }
}