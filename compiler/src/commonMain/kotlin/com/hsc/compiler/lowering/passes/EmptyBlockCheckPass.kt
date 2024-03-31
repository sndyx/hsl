package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.lowering.LoweringCtx

object EmptyBlockCheckPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Block>().forEach {
            if (it.stmts.isEmpty()) {
                val warn = ctx.dcx().warn("empty block")
                warn.span(it.span.loSpan)
                warn.emit()
            }
        }
    }

}