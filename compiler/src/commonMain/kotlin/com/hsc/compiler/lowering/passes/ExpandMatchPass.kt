package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object ExpandMatchPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val exprs = ctx.query<Expr>()
        val visitor = ExpandMatchVisitor(ctx)
        exprs.forEach {
            visitor.visitExpr(it)
        }
    }

}

private class ExpandMatchVisitor(val ctx: LoweringCtx) : AstVisitor {

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Match -> {
                val ident = when (val innerKind = kind.expr.kind) {
                    is ExprKind.Var -> innerKind.ident
                    else -> Ident(false, "_temp")
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
                    val temp = Stmt(kind.expr.span, StmtKind.Assign(Ident(false, "_temp"), kind.expr))
                    list.add(temp)
                }
                list.addAll(exprs)
                expr.kind = ExprKind.Block(Block(expr.span, list))
            }
            else -> {}
        }
    }

}