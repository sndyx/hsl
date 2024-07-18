package com.hsc.compiler.interpreter

import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Lit

fun Player.matchesCondition(condition: Expr): Boolean {
    if (condition.kind is ExprKind.Lit) return ((condition.kind as ExprKind.Lit).lit as Lit.Bool).value
    val bin = condition.kind as ExprKind.Binary // should always be binary...
    return when (bin.kind) {
        BinOpKind.And -> matchesCondition(bin.a) && matchesCondition(bin.b)
        BinOpKind.Or -> matchesCondition(bin.a) || matchesCondition(bin.b)

        BinOpKind.Eq -> exprValue(bin.a) == exprValue(bin.b)
        BinOpKind.Gt -> exprValue(bin.a) > exprValue(bin.b)
        BinOpKind.Ge -> exprValue(bin.a) >= exprValue(bin.b)
        BinOpKind.Lt -> exprValue(bin.a) < exprValue(bin.b)
        BinOpKind.Le -> exprValue(bin.a) <= exprValue(bin.b)
        else -> error("unreachable")
    }
}