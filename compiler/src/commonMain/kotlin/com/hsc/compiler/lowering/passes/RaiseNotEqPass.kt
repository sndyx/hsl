package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object RaiseNotEqPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Expr>()
        functions.forEach {
            RaiseNotEqVisitor.visitExpr(it)
        }
    }

}

private object RaiseNotEqVisitor : AstVisitor {

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                if (kind.kind == BinOpKind.Ne) {
                    kind.kind = BinOpKind.Eq
                    expr.kind = ExprKind.Paren(expr)
                }
            }
            else -> { }
        }
        super.visitExpr(expr)
    }

}