package com.hsc.compiler.lowering

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.Fn
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Stmt

fun Ident.isTemp(ctx: LoweringCtx): Boolean =
    name.startsWith(ctx.sess.opts.tempPrefix)

fun firstAvailableTemp(ctx: LoweringCtx, fn: Fn, currentExpr: Expr): Ident {
    val visitor = TempVariablesInUseVisitor(ctx, currentExpr)
    visitor.visitBlock(fn.block)

    return fn.tempVariables
        .filterNot { it in visitor.variablesInUse }
        .firstOrNull() ?: createNewTempVariable(ctx, fn)
}

fun createNewTempVariable(ctx: LoweringCtx, fn: Fn): Ident {
    repeat(Int.MAX_VALUE) { i ->
        val ident = Ident.Player(ctx.sess.opts.tempPrefix + "temp" + i)
        if (!fn.tempVariables.contains(ident)) {
            fn.tempVariables.add(ident)
            return ident
        }
    }
    error("??? how ???")
}

private class TempVariablesInUseVisitor(val ctx: LoweringCtx, val currentExpr: Expr) : AstVisitor {

    private var lock = false
    val variablesInUse = mutableSetOf<Ident>()
    val variablesReassigned = mutableSetOf<Ident>()

    override fun visitStmt(stmt: Stmt) {
        stmt.assign()?.let {
            if (lock) variablesReassigned.add(it.ident)
        }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        super.visitExpr(expr)
        if (expr == currentExpr) {
            lock = true // we have reached the given query expression
            return
        }

        val variable = expr.variable() ?: return
        val ident = variable.ident

        if (!ident.isTemp(ctx)) return

        if (variable.isLastUsage) {
            if (!lock) variablesInUse.remove(ident)
        } else {
            if (ident !in variablesReassigned) variablesInUse.add(ident)
        }
    }

}