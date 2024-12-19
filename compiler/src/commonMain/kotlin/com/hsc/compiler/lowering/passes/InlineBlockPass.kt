package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object InlineBlockPass : VolatilePass {

    override var changed: Boolean = false

    override fun run(ctx: LoweringCtx) {
        val visitor = InlineBlockVisitor(ctx)
        ctx.query<Item>().forEach {
            visitor.visitItem(it)
        }
        if (visitor.changed) changed = true
        if (changed) ctx.clearQuery<Stmt>()
    }

}

private class InlineBlockVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var changed = false

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Block -> {
                val block = kind.block

                if (block.stmts.size == 1 && block.stmts.single().kind is StmtKind.Expr) {
                    expr.kind = (block.stmts.single().kind as StmtKind.Expr).expr.kind
                    super.visitExpr(expr)
                    return
                }

                if (!inBlock) {
                    throw ctx.dcx().err("no owning block to inline statements", expr.span)
                }

                var flag = false

                when (val currentKind = currentStmt.kind) {
                    is StmtKind.Expr -> {
                        if (currentKind.expr == expr) {
                            currentBlock.stmts.removeAt(currentPosition)
                            currentBlock.stmts.addAll(currentPosition, block.stmts)
                            offset(block.stmts.size - 1)
                        } else flag = true
                    }
                    else -> flag = true
                }

                // This block is used as an expression
                if (flag) {
                    if (block.stmts.isEmpty() || block.stmts.last().kind !is StmtKind.Expr) {
                        val err = ctx.dcx().err("expected value")
                        err.spanLabel(block.span, "block used as expression must end with one")
                        throw err
                    }
                    currentBlock.stmts.addAll(currentPosition, block.stmts.dropLast(1))
                    expr.kind = (block.stmts.last().kind as StmtKind.Expr).expr.kind
                    offset(block.stmts.size - 1)
                }

                changed = true
            }
            else -> {}
        }
        super.visitExpr(expr)
    }

}
