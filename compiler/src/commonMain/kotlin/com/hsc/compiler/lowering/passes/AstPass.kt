package com.hsc.compiler.lowering.passes

import com.hsc.compiler.lowering.LoweringCtx

interface AstPass {

    fun run(ctx: LoweringCtx)

}