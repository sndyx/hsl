package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.traverse

object CommonSubexpressionEliminationPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                val fn = (it.kind as ItemKind.Fn).fn

                val map = mutableSetOf<MappedExpr>()

                TODO("Rahh!")
            }
    }

}

private data class MappedExpr(val expr: Expr) {
    val idents = buildList {
        traverse(expr) { expr.variable()?.ident?.let(::add) }
    }
}