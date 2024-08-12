package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.lowering.LoweringCtx

object FindLastVariableUsagePass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                FindVariableLastUsageVisitor.visitItem(it)
            }
    }

}

private object FindVariableLastUsageVisitor : AstVisitor {

    val variables = mutableMapOf<Ident, ExprKind.Var>()

    override fun visitItem(item: Item) {
        variables.clear()
    }

    override fun visitStmt(stmt: Stmt) {
        stmt.assign()?.let { variables[it.ident]?.isLastUsage = true }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        expr.variable()?.let { variables[it.ident] = it }
        super.visitExpr(expr)
    }

}