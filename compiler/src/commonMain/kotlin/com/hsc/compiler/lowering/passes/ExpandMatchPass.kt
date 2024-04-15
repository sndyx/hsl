package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object ExpandMatchPass : AstPass {

    private var changed = false

    override fun run(ctx: LoweringCtx) {
        ctx.query<Expr>().forEach { expr ->
            changed = changed || expandMatch(expr)
        }
        if (changed) {
            ctx.clearQuery<Expr>()
            ctx.clearQuery<Block>()
        }
    }

}

private fun expandMatch(expr: Expr): Boolean {
    when (val kind = expr.kind) {
        is ExprKind.Match -> {
            val ident = when (val innerKind = kind.expr.kind) {
                is ExprKind.Var -> innerKind.ident
                else -> Ident.Player("_temp")
            }

            val exprs = kind.arms.map { arm ->
                val query = arm.query.map {
                    Expr(Span.none, ExprKind.Binary(BinOpKind.Eq, Expr(Span.none, ExprKind.Var(ident)), it))
                }.reduce { acc, other -> Expr(Span.none, ExprKind.Binary(BinOpKind.Or, acc, other)) }
                val block = Block(arm.value.span, mutableListOf(Stmt(arm.value.span, StmtKind.Expr(arm.value))))
                val ifExpr = Expr(Span.none, ExprKind.If(query, block, null))
                Stmt(Span.none, StmtKind.Expr(ifExpr))
            }

            val list = mutableListOf<Stmt>()
            if (ident.name == "_temp") {
                val temp = Stmt(kind.expr.span, StmtKind.Assign(Ident.Player("_temp"), kind.expr))
                list.add(temp)
            }
            list.addAll(exprs)
            expr.kind = ExprKind.Block(Block(expr.span, list))
            return true
        }
        else -> {}
    }
    return false
}