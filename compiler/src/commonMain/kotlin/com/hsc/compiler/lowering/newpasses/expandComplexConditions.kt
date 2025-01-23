package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.*
import com.hsc.compiler.span.Span

fun expandComplexConditions(ctx: LoweringCtx) {
    fun decompose(fn: Fn, expr: Expr, isOwning: Boolean = false): Expr {
        val binary = expr.binary() ?: return expr
        if (binary.kind != BinOpKind.And && binary.kind != BinOpKind.Or) return expr
        if (isMonomorphic(expr) && isOwning) return expr // this expr is fine

        val lhs = decompose(fn, binary.a)
        val rhs = decompose(fn, binary.b)

        val temp = ctx.firstAvailableTemp(fn, expr)

        val trueExpr = Expr(Span.none, ExprKind.Lit(Lit.Bool(true)))
        val falseExpr = Expr(Span.none, ExprKind.Lit(Lit.Bool(false)))

        val ifActions = wrap(Stmt(expr.span, StmtKind.Assign(temp, trueExpr)))
        val elseActions = wrap(Stmt(expr.span, StmtKind.Assign(temp, falseExpr)))

        val cond = Expr(expr.span, ExprKind.Binary(binary.kind, lhs, rhs))

        if (isOwning) return cond

        val ifStmt = Stmt(expr.span, StmtKind.Expr(
            Expr(expr.span, ExprKind.If(cond, ifActions, elseActions))
        ))
        val tempStmt = Stmt(expr.span, StmtKind.Expr(Expr(
            expr.span, ExprKind.Binary(BinOpKind.Eq,
                Expr(expr.span, ExprKind.Var(temp)),
                Expr(expr.span, ExprKind.Lit(Lit.Bool(true)))
            )
        )))

        return Expr(expr.span, ExprKind.Block(wrap(listOf(ifStmt, tempStmt))))
    }

    ctx.getFunctions().forEach { fn ->
        walk(fn) { expr ->
            val cond = expr.conditional() ?: return@walk
            val dec = decompose(fn, cond.expr, true)
            cond.expr.kind = dec.kind
        }
    }
}

private fun isMonomorphic(expr: Expr): Boolean {
    val kind = expr.binary()!!.kind

    var matches = true
    walk(expr) {
        val op = it.binary()?.kind ?: return@walk
        if (op != BinOpKind.And && op != BinOpKind.Or) return@walk

        if (op != kind) matches = false
    }

    return matches
}