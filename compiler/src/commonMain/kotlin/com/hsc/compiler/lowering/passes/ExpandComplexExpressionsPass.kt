package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.firstAvailableTemp

/**
 * A pass that should flatten complex expressions.
 *
 * Should *always* run after `InlineFunctionParametersPass`!
 *
 * eg:
 * ```
 * x = 1 + 2 * 3
 * ```
 * becomes:
 * ```
 * _temp = 2
 * _temp *= 3
 * _temp += 1
 * x = temp0
 * ```
 */
object ExpandComplexExpressionsPass : AstPass {

    private var changed = false

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                val fn = (it.kind as ItemKind.Fn).fn
                val visitor = FlattenComplexExpressionsVisitor(ctx, fn)
                do {
                    visitor.changed = false
                    visitor.visitItem(it)
                    if (visitor.changed) changed = true
                } while (visitor.changed)
            }
        if (changed) ctx.clearQuery<Stmt>()
    }

}

private class FlattenComplexExpressionsVisitor(val ctx: LoweringCtx, val fn: Fn) : BlockAwareVisitor() {

    var changed = false
    var assignIdent: Ident? = null

    override fun visitStmt(stmt: Stmt) {
        assignIdent = stmt.assign()?.ident
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        val binExpr = expr.binary() ?: return

        if (binExpr.kind !in
            listOf(BinOpKind.Add, BinOpKind.Sub, BinOpKind.Mul, BinOpKind.Div, BinOpKind.Rem)
        ) return super.visitExpr(expr)

        val tempIdent = binExpr.a.variable()
            ?.takeIf { it.isLastUsage }?.ident
            ?: ctx.firstAvailableTemp(fn, expr)

        val a = Stmt(binExpr.a.span, StmtKind.Assign(assignIdent ?: tempIdent, binExpr.a))
        val b = Stmt(binExpr.b.span, StmtKind.AssignOp(binExpr.kind, assignIdent ?: tempIdent, binExpr.b))

        currentBlock.stmts.add(currentPosition, a)
        currentBlock.stmts.add(currentPosition + 1, b)

        if (assignIdent == null) {
            expr.kind = ExprKind.Var(tempIdent, true)
        } else {
            currentBlock.stmts.removeAt(currentPosition + 2)
        }
        offset(if (assignIdent != null) 1 else 2)
        changed = true
    }

}


