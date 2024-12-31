package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getExprs
import com.hsc.compiler.lowering.getItems

fun inlineConsts(ctx: LoweringCtx) {
    ctx.getItems()
        .filter { it.kind is ItemKind.Const }
        .forEach { item ->
            val value = (item.kind as ItemKind.Const).value

            ctx.getExprs().forEach { expr ->
                val variable = expr.variable() ?: return@forEach

                if (variable.ident == item.ident) expr.kind = value.kind
            }

            ctx.ast.items.remove(item)
        }
}