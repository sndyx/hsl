package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.Fn
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.isTemp

object CheckTempVariablesAssignedBeforeUsePass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                val fn = (it.kind as ItemKind.Fn).fn
                val visitor = CheckTempVariablesAssignedBeforeUseVisitor(ctx, fn)
                visitor.visitItem(it)
            }
    }

}

private class CheckTempVariablesAssignedBeforeUseVisitor(val ctx: LoweringCtx, val fn: Fn) : AstVisitor {

    val variablesDefined = fn.sig.args.toMutableSet()

    override fun visitStmt(stmt: Stmt) {
        stmt.assign()?.let { variablesDefined.add(it.ident) }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        expr.variable()?.let {
            if (ctx.isTemp(it.ident) && !it.ident.name.endsWith("return") && it.ident !in variablesDefined) {
                ctx.dcx().warn("temp variable used before assigned", expr.span).emit()
            }
        }
        super.visitExpr(expr)
    }

}