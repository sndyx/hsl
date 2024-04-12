package com.hsc.compiler.lowering.passes

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object BlockFoldingPass : VolatilePass {

    override var changed: Boolean = false

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        val visitor = InlineBlockVisitor(ctx)
        functions.forEach {
            visitor.visitItem(it)
            if (visitor.changed) changed = true
        }
    }

}

private class InlineBlockVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var changed = false

    override fun visitStmt(stmt: Stmt) {
        when (val stmtKind = stmt.kind) {
            is StmtKind.Assign -> {
                when (val block = stmtKind.expr.kind) {
                    is ExprKind.Block -> {
                        val lastStmt = block.block.stmts.lastOrNull()
                            ?: throw ctx.dcx().err("empty block used as expression", block.block.span)
                        when (val kind = lastStmt.kind) {
                            is StmtKind.Expr -> {
                                currentBlock.stmts.addAll(currentPosition, block.block.stmts.dropLast(1))
                                stmtKind.expr = kind.expr
                                added(block.block.stmts.size - 1)
                                changed = true
                            }
                            else -> {
                                val err = ctx.dcx().err("expected value, found statement", lastStmt.span)
                                err.note(Level.Hint, "last statement of block should be expression")
                                throw err
                            }
                        }
                    }
                    else -> { }
                }
            }
            is StmtKind.AssignOp -> {
                when (val block = stmtKind.expr.kind) {
                    is ExprKind.Block -> {
                        val lastStmt = block.block.stmts.lastOrNull()
                            ?: throw ctx.dcx().err("empty block used as expression", block.block.span)
                        when (val kind = lastStmt.kind) {
                            is StmtKind.Expr -> {
                                currentBlock.stmts.addAll(currentPosition, block.block.stmts.dropLast(1))
                                stmtKind.expr = kind.expr
                                added(block.block.stmts.size - 1)
                                changed = true
                            }
                            else -> {
                                val err = ctx.dcx().err("expected value, found statement", lastStmt.span)
                                err.note(Level.Hint, "last statement of block should be expression")
                                throw err
                            }
                        }
                    }
                    else -> { }
                }
            }
            is StmtKind.Expr -> {
                when (val block = stmtKind.expr.kind) {
                    is ExprKind.Block -> {
                        currentBlock.stmts.removeAt(currentPosition)
                        currentBlock.stmts.addAll(currentPosition, block.block.stmts)
                        added(block.block.stmts.size - 1)
                        changed = true
                    }
                    else -> {}
                }
            }
            else -> { }
        }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Block -> {
                val block = kind.block
                if (block.stmts.size == 1 && block.stmts.single().kind is StmtKind.Expr) {
                    expr.kind = (block.stmts.single().kind as StmtKind.Expr).expr.kind
                    changed = true
                }
            }
            else -> super.visitExpr(expr)
        }
    }

}
