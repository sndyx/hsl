package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getExprs

fun foldStrings(ctx: LoweringCtx) = with(ctx) {
    var changed = true
    while (changed) {
        changed = false

        getExprs().forEach { expr ->
            val kind = expr.binary() ?: return@forEach

            if (
                kind.a.kind is ExprKind.Lit && kind.b.kind is ExprKind.Var
                || kind.a.kind is ExprKind.Var && kind.b.kind is ExprKind.Lit
            ) {
                val exprA = (kind.a.lit() ?: kind.b.lit())!!
                val exprB = (kind.a.variable() ?: kind.b.variable())!!

                if (exprA.lit !is Lit.Str) return

                val varStr = when (val ident = exprB.ident) {
                    is Ident.Player -> "%stat.player/${ident.name}%"
                    is Ident.Global -> "%stat.global/${ident.name}%"
                    is Ident.Team -> "%stat.team/${ident.name} ${ident.team}%"
                }

                val result = (exprA.lit as Lit.Str).value + varStr

                expr.kind = ExprKind.Lit(Lit.Str(result))
                changed = true
                return@forEach
            }

            if (kind.a.kind is ExprKind.Lit && kind.a.lit()?.lit is Lit.Str
                && kind.b.kind is ExprKind.Lit && kind.b.lit()?.lit is Lit.Str) {
                val result = (kind.a.lit()!!.lit as Lit.Str).value + (kind.b.lit()!!.lit as Lit.Str).value

                expr.kind = ExprKind.Lit(Lit.Str(result))
                return@forEach
            }
        }
    }
}