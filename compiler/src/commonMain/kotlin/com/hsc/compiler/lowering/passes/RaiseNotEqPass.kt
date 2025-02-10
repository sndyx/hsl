package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getExprs

object RaiseNotEqPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.getExprs().forEach { expr ->
            val binary = expr.binary() ?: return@forEach
            if (binary.kind != BinOpKind.Ne) return@forEach

            binary.kind = BinOpKind.Eq
            expr.kind = ExprKind.Unary(UnaryOpKind.Not, Expr(expr.span, expr.kind))
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