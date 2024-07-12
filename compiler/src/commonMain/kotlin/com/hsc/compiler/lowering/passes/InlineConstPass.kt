package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object InlineConstPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val consts = ctx.query<Item>().filter { it.kind is ItemKind.Const }
        ctx.query<Expr>().forEach { expr ->
            inlineConst(expr, consts)
        }
        consts.forEach {
            val kind = (it.kind as ItemKind.Const).value.kind
            if (!(kind is ExprKind.Lit && kind.lit is Lit.Item)) {
                ctx.ast.items.remove(it)
            } // remove all consts that aren't items
        }
        ctx.clearQuery<Item>()
    }

}

private fun inlineConst(expr: Expr, consts: List<Item>) {
    when (val kind = expr.kind) {
        is ExprKind.Var -> {
            consts.find { it.ident == kind.ident }?.let {
                val const = it.kind as ItemKind.Const
                expr.kind = const.value.kind.deepCopy()
            }
        }
        else -> {}
    }
}