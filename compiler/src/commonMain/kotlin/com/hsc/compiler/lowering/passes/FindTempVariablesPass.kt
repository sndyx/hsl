package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.Fn
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.isTemp

object FindTempVariablesPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                val fn = (it.kind as ItemKind.Fn).fn
                val visitor = FindTempVariablesVisitor(ctx, fn)
                visitor.visitItem(it)
            }
    }

}

private class FindTempVariablesVisitor(val ctx: LoweringCtx, val fn: Fn) : AstVisitor {

    override fun visitStmt(stmt: Stmt) {
        super.visitStmt(stmt)
        val ident = stmt.assign()?.ident ?: stmt.assignOp()?.ident ?: return

        if (ident.isTemp(ctx)) {
            fn.tempVariables.add(ident)
        }
    }

    override fun visitExpr(expr: Expr) {
        expr.variable()?.let {
            if (it.ident.isTemp(ctx)) {
                fn.tempVariables.add(it.ident)
            }
        }
        super.visitExpr(expr)
    }

}