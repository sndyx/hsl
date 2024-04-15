package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

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
                val visitor = FlattenComplexExpressionsVisitor()
                do {
                    visitor.changed = false
                    visitor.visitItem(it)
                    visitor.passes++
                    if (visitor.changed) changed = true
                } while (visitor.changed)
            }
        if (changed) ctx.clearQuery<Stmt>()
    }

}

private class FlattenComplexExpressionsVisitor : BlockAwareVisitor() {

    var changed = false
    var passes = 0
    var cident: Ident? = null

    override fun visitStmt(stmt: Stmt) {
        cident = when (val kind = stmt.kind) {
            is StmtKind.Assign -> {
                kind.ident
            }
            else -> {
                null
            }
        }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                when (kind.kind) {
                    BinOpKind.Add, BinOpKind.Sub, BinOpKind.Mul, BinOpKind.Div, BinOpKind.Rem -> {
                        val assignTemp = Stmt(
                            kind.a.span,
                            StmtKind.Assign(cident ?: Ident.Player("_temp$passes"), kind.a)
                        )
                        val assignOp = Stmt(
                            kind.b.span,
                            StmtKind.AssignOp(kind.kind, cident ?: Ident.Player("_temp$passes"), kind.b)
                        )
                        currentBlock.stmts.add(currentPosition, assignTemp)
                        currentBlock.stmts.add(currentPosition + 1, assignOp)
                        if (cident == null) {
                            expr.kind = ExprKind.Var(Ident.Player("_temp$passes"))
                        } else {
                            currentBlock.stmts.removeAt(currentPosition + 2)
                        }
                        added(if (cident != null) 1 else 2)
                        changed = true
                    }
                    else -> super.visitExpr(expr) // Do(n't) flatten conditions
                }
            }
            else -> super.visitExpr(expr)
        }
    }

}


