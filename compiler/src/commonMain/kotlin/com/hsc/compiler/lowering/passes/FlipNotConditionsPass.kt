package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object FlipNotConditionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Expr>().forEach { expr ->
            flipNotConditions(expr)
        }
    }

}

private fun flipNotConditions(expr: Expr) {
    when (val kind = expr.kind) {
        is ExprKind.Unary -> {
            when (val inner = kind.expr.kind) {
                is ExprKind.Binary -> {
                    when (inner.kind) {
                        BinOpKind.Lt -> BinOpKind.Ge
                        BinOpKind.Le -> BinOpKind.Gt
                        BinOpKind.Gt -> BinOpKind.Le
                        BinOpKind.Ge -> BinOpKind.Lt
                        else -> null
                    }?.let {
                        inner.kind = it
                        expr.kind = inner
                    }
                }
                else -> {}
            }
        }
        else -> {}
    }
}