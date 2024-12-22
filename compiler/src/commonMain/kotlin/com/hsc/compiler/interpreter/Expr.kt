package com.hsc.compiler.interpreter

import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Lit

fun Player.executeExpr(expr: Expr) {
    when (val kind = expr.kind) {
        is ExprKind.Call -> {
            if (kind.ident.isGlobal) {
                for (player in housing.players) {
                    player.executeFunction(kind.ident.name)
                }
            } else {
                executeFunction(kind.ident.name)
            }
        }
        is ExprKind.If -> {
            if (matchesCondition(kind.expr)) {
                executeBlock(kind.block)
            } else {
                kind.other?.let { executeBlock(it) }
            }
        }
        else -> error("unreachable")
    }
}

fun Player.exprValue(expr: Expr): Long {
    return when (val kind = expr.kind) {
        is ExprKind.Var -> {
            when (val ident = kind.ident) {
                is Ident.Player -> {
                    stats[ident.name] ?: 0L
                }
                is Ident.Global -> {
                    housing.globalStats[ident.name] ?: 0L
                }
                is Ident.Team -> {
                    housing.teamStats[ident.team]?.get(ident.name) ?: 0L
                }
            }
        }
        is ExprKind.Lit -> {
            when (val lit = kind.lit) {
                is Lit.I64 -> lit.value
                is Lit.F64 -> lit.value.toLong()
                is Lit.Bool -> if (lit.value) 1L else 0L
                is Lit.Str -> placeholderValue(lit.value.removeSurrounding("%"))
                else -> error("unreachable")
            }
        }
        else -> error("unreachable")
    }
}

fun Player.exprValue(expr: String): Long {
    return expr.toLongOrNull() ?: placeholderValue(expr.removeSurrounding("%"))
}