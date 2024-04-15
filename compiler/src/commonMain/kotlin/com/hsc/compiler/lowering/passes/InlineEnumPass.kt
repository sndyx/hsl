package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object InlineEnumPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val enums = ctx.query<Item>().filter { it.kind is ItemKind.Enum }.flatMap {
            val enum = (it.kind as ItemKind.Enum).enum

            val team = it.ident.name
            enum.values.map { (name, value) ->
                Pair(Ident.Team(team, name), value)
            }
        }.toMap()
        ctx.query<Expr>().forEach { expr ->
            inlineEnum(expr, enums)
        }
        ctx.query<Item>().filter { it.kind is ItemKind.Enum }.forEach {
            ctx.ast.items.remove(it)
        }
        ctx.clearQuery<Item>()
    }

}

private fun inlineEnum(expr: Expr, enums: Map<Ident.Team, Long>) {
    when (val kind = expr.kind) {
        is ExprKind.Var -> {
            enums.entries
                .find { it.key == kind.ident }
                ?.let {
                    expr.kind = ExprKind.Lit(Lit.I64(it.value))
                }
        }
        else -> {}
    }
}