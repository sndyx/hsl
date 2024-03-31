package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object RemoveParenPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            RemoveParenVisitor.visitItem(it)
        }
    }

}

private object RemoveParenVisitor : AstVisitor {

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Paren -> {
                expr.kind = kind.expr.kind
                expr.span = kind.expr.span
            }
            else -> { }
        }
        super.visitExpr(expr)
    }

}