package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getItems
import com.hsc.compiler.lowering.walkAware

/**
 * A pass that tries to inline block expressions into their owning blocks
 *
 * eg:
 * ```
 * x = {
 *   message("Hello")
 *   y
 * }
 * ```
 * becomes:
 * ```
 * message("Hello")
 * x = y
 * ```
 */
fun inlineBlocks(ctx: LoweringCtx) = with(ctx) {
    getItems().forEach { item ->
        walkAware(item) { expr ->
            val block = expr.block()?.block ?: return@walkAware

            if (block.stmts.size == 1 && block.stmts.single().expr() != null) {
                // this block contains one single expression stmt, so just unwrap
                expr.kind = (block.stmts.single().expr()!!.expr.kind)
                return@walkAware
            }

            if (!inBlock) {
                throw ctx.dcx().err("no owning block to inline statements", expr.span)
            }

            if (currentStmt.expr()?.expr == expr) {
                // this block is just wrapped in a statement
                currentBlock.stmts.removeAt(currentPosition)
                currentBlock.stmts.addAll(currentPosition, block.stmts)
                offset(block.stmts.size - 1)
                return@walkAware
            }

            if (block.stmts.isEmpty() || block.stmts.last().expr() == null) {
                val err = ctx.dcx().err("expected value")
                err.spanLabel(block.span, "block used as expression must end with one")
                throw err
            }

            currentBlock.stmts.addAll(currentPosition, block.stmts.dropLast(1))
            expr.kind = block.stmts.last().expr()!!.expr.kind
            offset(block.stmts.size - 1)
        }
    }
}