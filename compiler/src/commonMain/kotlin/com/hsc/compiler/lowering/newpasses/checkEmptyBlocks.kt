package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getBlocks

/**
 * Checks for any blocks that contain no statements and gives a warning.
 */
fun checkEmptyBlocks(ctx: LoweringCtx) {
    ctx.getBlocks().forEach { block ->
        if (block.stmts.isEmpty()) {
            ctx.dcx().warn("empty block", block.span).emit()
        }
    }
}