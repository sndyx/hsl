package com.hsc.compiler.lowering.passes

import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.checkLimits
import com.hsc.compiler.span.Span

object CheckLimitsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Block>().forEach { block ->
            limitCheck(ctx, block)
        }
    }

}

private fun limitCheck(ctx: LoweringCtx, block: Block) {
    val (action, span) = checkLimits(block) ?: return
    val err = ctx.dcx().err("action limit surpassed: `${action.lowercase()}`")
    if (span == Span.none) {
        err.spanLabel(block.span, "in this scope")
    } else {
        err.spanLabel(span, "with this statement")
    }
    if (ctx.sess.opts.mode != Mode.Strict) {
        err.note(Level.Error, "could not optimize out actions")
    }
    err.emit()
}