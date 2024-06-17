package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object CheckEmptyBlockPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        val visitor = CheckEmptyBlockVisitor(ctx)
        functions.forEach {
            visitor.visitItem(it)
        }
    }

}

private class CheckEmptyBlockVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    override fun visitExpr(expr: Expr) {
        if (expr.kind is ExprKind.If) {
            val ifStmt = (expr.kind as ExprKind.If)
            if (ifStmt.block.stmts.isEmpty() && ifStmt.other?.stmts?.isEmpty() != false) {
                ctx.dcx().warn("empty block", ifStmt.block.span).emit()
            }
        }
    }

    override fun visitFn(fn: Fn) {
        if (fn.block.stmts.isEmpty()) {
            ctx.dcx().warn("empty block", fn.block.span).emit()
        }
    }

}