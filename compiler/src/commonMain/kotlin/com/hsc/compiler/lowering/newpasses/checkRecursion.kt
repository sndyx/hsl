package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.lowering.*

/**
 * Checks for recursion and gives a warning.
 */
fun checkRecursion(ctx: LoweringCtx) {
    ctx.getFunctionItems().forEach { (item, fn) ->
        walk(fn) { expr ->
            if (expr.call()?.ident == item.ident) {
                val warn = ctx.dcx().warn("recursive call")
                warn.reference(item.span, "for this function")
                warn.spanLabel(expr.span, "recursive call here")
                warn.emit()
            }
        }
    }
}