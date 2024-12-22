package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.BlockAwareVisitor
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.firstAvailableTemp
import com.hsc.compiler.lowering.getFunctions

fun expandComplexExpressions(ctx: LoweringCtx) = with(ctx) {
    getFunctions().forEach { fn ->
        val visitor = object : BlockAwareVisitor() {
            var changed = false
            var assignIdent: Ident? = null

            override fun visitStmt(stmt: Stmt) {
                assignIdent = stmt.assign()?.ident
                super.visitStmt(stmt)
            }

            override fun visitExpr(expr: Expr) {
                val binExpr = expr.binary() ?: return super.visitExpr(expr)

                if (binExpr.kind !in
                    listOf(BinOpKind.Add, BinOpKind.Sub, BinOpKind.Mul, BinOpKind.Div, BinOpKind.Rem)
                ) return super.visitExpr(expr)

                if (currentStmt.assign() != null) {
                    // we are in an assign, so we can just set this to the first term in the binary
                    // and assignop the next term.
                    val assignIdent = currentStmt.assign()!!.ident

                    currentStmt.kind = StmtKind.Assign(assignIdent, binExpr.a)
                    val b = Stmt(binExpr.b.span, StmtKind.AssignOp(binExpr.kind, assignIdent, binExpr.b))
                    currentBlock.stmts.add(currentPosition + 1, b)
                    offset(1)
                } else {
                    val tempIdent = binExpr.a.variable()
                        ?.takeIf { it.isLastUsage }?.ident
                        ?: ctx.firstAvailableTemp(fn, expr)

                    val a = Stmt(binExpr.a.span, StmtKind.Assign(tempIdent, binExpr.a))
                    val b = Stmt(binExpr.b.span, StmtKind.AssignOp(binExpr.kind, tempIdent, binExpr.b))
                    currentBlock.stmts.add(currentPosition, b)
                    currentBlock.stmts.add(currentPosition, a)
                    expr.kind = ExprKind.Var(tempIdent)
                    offset(2)
                }

                changed = true
            }
        }

        do {
            visitor.changed = false
            visitor.visitFn(fn)
        } while (visitor.changed)
    }
}