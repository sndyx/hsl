package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

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
object FlattenComplexExpressionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            val visitor = FlattenComplexExpressionsVisitor()
            do {
                visitor.changes = 0
                visitor.visitItem(it)
                visitor.passes++
            } while (visitor.changes != 0)
        }
    }

}

private class FlattenComplexExpressionsVisitor : BlockAwareVisitor() {

    var changes = 0
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
                            NodeId.from(currentBlock.id),
                            Span.none,
                            StmtKind.Assign(cident ?: Ident(false, "_temp$passes"), kind.a)
                        )
                        val assignOp = Stmt(
                            NodeId.from(currentBlock.id),
                            Span.none,
                            StmtKind.AssignOp(kind.kind, cident ?: Ident(false, "_temp$passes"), kind.b)
                        )
                        currentBlock.stmts.add(currentPosition, assignTemp)
                        currentBlock.stmts.add(currentPosition + 1, assignOp)
                        if (cident == null) {
                            expr.kind = ExprKind.Var(Ident(false, "_temp$passes"))
                        } else {
                            currentBlock.stmts.removeAt(currentPosition + 2)
                        }
                        added(if (cident != null) 1 else 2)
                        changes++
                    }
                    else -> super.visitExpr(expr) // Do(n't) flatten conditions
                }
            }
            else -> super.visitExpr(expr)
        }
    }

}


