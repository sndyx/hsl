package com.hsc.compiler.lowering

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.Fn
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Stmt

fun LoweringCtx.isTemp(ident: Ident): Boolean =
    ident.name.startsWith(sess.opts.tempPrefix)

fun LoweringCtx.firstAvailableTemp(fn: Fn, currentExpr: Expr): Ident {
    val visitor = TempVariablesInUseVisitor(this, currentExpr)
    visitor.visitFn(fn)

    return fn.tempVariables
        .filterNot { it in visitor.variablesInUse }
        .firstOrNull() ?: createNewTempVariable(fn)
}

fun LoweringCtx.createNewTempVariable(fn: Fn): Ident {
    repeat(Int.MAX_VALUE) { i ->
        val ident = Ident.Player(sess.opts.tempPrefix + "temp" + i)
        if (!fn.tempVariables.contains(ident)) {
            fn.tempVariables.add(ident)
            return ident
        }
    }
    error("unreachable")
}

fun LoweringCtx.isTempInUse(ident: Ident, fn: Fn, currentExpr: Expr): Boolean {
    val visitor = TempVariablesInUseVisitor(this, currentExpr)
    visitor.visitFn(fn)

    return visitor.variablesInUse.contains(ident)
}

private class TempVariablesInUseVisitor(val ctx: LoweringCtx, val currentExpr: Expr) : AstVisitor {

    private var lock = false
    val variablesInUse = mutableSetOf<Ident>()
    val variablesReassigned = mutableSetOf<Ident>()

    override fun visitFn(fn: Fn) {
        variablesInUse.addAll(fn.sig.args.filter { ctx.isTemp(it) })
        super.visitFn(fn)
    }

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

        if (!ctx.isTemp(ident)) return

        if (variable.isLastUsage) {
            if (!lock) variablesInUse.remove(ident)
        } else {
            if (ident !in variablesReassigned) variablesInUse.add(ident)
        }
    }

}