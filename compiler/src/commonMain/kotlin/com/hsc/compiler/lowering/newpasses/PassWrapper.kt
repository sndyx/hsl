package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.passes.AstPass

class PassWrapper(val pass: (LoweringCtx) -> Unit) : AstPass {

    override fun run(ctx: LoweringCtx) {
        pass(ctx)
    }

}