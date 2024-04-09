package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object FlipNotConditionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Expr>()
        functions.forEach {
            FlipNotConditionsVisitor.visitExpr(it)
        }
    }

}

private object FlipNotConditionsVisitor : AstVisitor {

    override fun visitExpr(expr: Expr) {
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
                    else -> { }
                }
            }
            else -> { }
        }
        super.visitExpr(expr)
    }

}