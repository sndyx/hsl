package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object InlineFunctionPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            val visitor = InlineFunctionVisitor(ctx)
            do {
                visitor.changes = 0
                visitor.visitItem(it)
            } while (visitor.changes != 0)
        }
        functions.forEach {
            if ((it.kind as ItemKind.Fn).fn.processors?.list?.contains("inline") == true) {
                ctx.ast.items.remove(it)
            }
        }
        // CLEAR QUERY
        ctx.clearQuery<Expr>()
        ctx.clearQuery<Stmt>()
        ctx.clearQuery<Block>()
        ctx.clearQuery<Item>()
        ctx.clearQuery<Fn>()
        ctx.clearQuery<Lit>()
    }

}

private class InlineFunctionVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var changes = 0
    var cident: Ident? = null

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.Assign -> {
                cident = kind.ident
            }
            is StmtKind.AssignOp -> {
                // cident = Ident(false, "_temp0")
                // this might conflict with the complex expressions pass...
            }
            else -> {
                cident = null
            }
        }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                val calleeName = kind.ident.name
                val callee = ctx.query<Item>().find { it.kind is ItemKind.Fn && it.ident.name == calleeName }
                if (callee != null) {
                    val fn = (callee.kind as ItemKind.Fn).fn.deepCopy()
                    if (fn.processors?.list?.contains("inline") == true) {

                        // deepCopy() to remove ref! Will otherwise link multiple parts badly
                        val inlineBlock = fn.block.deepCopy()

                        val assignments = mutableListOf<Pair<Ident, Expr>>()

                        // backwards-inline args
                        fn.sig.args.forEachIndexed { idx, ident ->
                            val visitor = InlinedFunctionTransformerVisitor(ident, kind.args.args[idx].deepCopy())
                            visitor.visitBlock(inlineBlock)
                            if (visitor.stopEarly) assignments += Pair(ident, kind.args.args[idx].deepCopy())
                        }
                        kind.args.args.clear()

                        expr.kind = ExprKind.Var(Ident.Player("_return"))
                        // currentBlock.stmts.removeAt(currentPosition)

                        currentBlock.stmts.addAll(currentPosition, inlineBlock.stmts)

                        val assignStmts = assignments.map { (ident, expr) ->
                            Stmt(Span.none, StmtKind.Assign(ident, expr.deepCopy()))
                        }

                        currentBlock.stmts.addAll(currentPosition, assignStmts)

                        added(inlineBlock.stmts.size)
                        added(assignStmts.size)

                        changes++
                    }
                }
            }
            else -> super.visitExpr(expr)
        }
    }

}

private class InlinedFunctionTransformerVisitor(val old: Ident, val new: Expr) : AstVisitor {

    var ident: Ident? = null
    var stopEarly = false

    init {
        if (new.kind is ExprKind.Var) ident = (new.kind as ExprKind.Var).ident
    }

    override fun visitExpr(expr: Expr) {
        if (stopEarly) return
        when (val kind = expr.kind) {
            is ExprKind.Var -> {
                if (kind.ident == old) {
                    expr.kind = new.kind.deepCopy()
                    expr.span = new.span
                }
            } else -> super.visitExpr(expr)
        }
    }

    override fun visitStmt(stmt: Stmt) {
        if (stopEarly) return
        when (val kind = stmt.kind) {
            is StmtKind.Assign -> {
                if (kind.ident == old) {
                    if (ident == null) {
                        stopEarly = true
                    } else {
                        kind.ident = ident!!
                    }
                }
            }
            is StmtKind.AssignOp -> {
                if (kind.ident == old) {
                    if (ident == null) {
                        stopEarly = true
                    } else {
                        kind.ident = ident!!
                    }
                }
            }
            else -> {}
        }
        super.visitStmt(stmt)
    }

}

