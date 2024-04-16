package com.hsc.compiler.lowering.passes

import com.hsc.compiler.lowering.LoweringCtx

interface VolatilePass : AstPass {
    var changed: Boolean
}

fun symbiotic(a: VolatilePass, b: VolatilePass): AstPass {
    return SymbioticPass(a, b)
}

private class SymbioticPass(val a: VolatilePass, val b: VolatilePass) : AstPass {

    override fun run(ctx: LoweringCtx) {
        a.run(ctx)
        b.run(ctx)
        while (true) {
            if (b.changed) a.run(ctx)
            else break
            b.changed = false
            if (a.changed) b.run(ctx)
            else break
            a.changed = false
        }
        a.run(ctx)
        b.run(ctx)
    }

}