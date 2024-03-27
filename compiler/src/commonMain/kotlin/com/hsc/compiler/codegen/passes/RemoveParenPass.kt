package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.*

object RemoveParenPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
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