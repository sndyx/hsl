package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.lowering.LoweringCtx
import net.benwoodworth.knbt.NbtCompound

object NameItemsPass : AstPass {

    val itemSet = mutableSetOf<NbtCompound>()

    override fun run(ctx: LoweringCtx) {
        ctx.query<Lit>().forEach {
            if (it is Lit.Item) {
                if (it.value.name == null) {
                    if (!itemSet.contains(it.value.nbt)) {
                        it.value.name = "item${itemSet.size}"
                        itemSet.add(it.value.nbt)
                    } else {
                        it.value.name = "item${itemSet.indexOf(it.value.nbt)}"
                    }
                }
            }
        }
    }

}