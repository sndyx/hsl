package com.hsc.compiler.lowering.passes

import com.hsc.compiler.lowering.limits
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.BlockAwareVisitor
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.lowering.LoweringCtx

object LimitCheckPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            LimitCheckVisitor(ctx).visitItem(it)
        }
    }

}

private class LimitCheckVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    override fun visitBlock(block: Block) {
        val action = limits(block).entries.find { it.value < 0 }?.key
        if (action != null) {
            val err = ctx.dcx().err("action limit surpassed: `$action`")
            err.spanLabel(block.span, "in this scope")
            if (ctx.sess.opts.mode != Mode.Strict) {
                err.note(Level.Error, "could not optimize out actions")
            }
            err.emit()
        }
        super.visitBlock(block)
    }

}