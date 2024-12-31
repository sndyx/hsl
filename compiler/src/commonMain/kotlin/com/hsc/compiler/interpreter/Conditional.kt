package com.hsc.compiler.interpreter

import com.hsc.compiler.ir.action.Comparison
import com.hsc.compiler.ir.action.Condition
import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Lit

fun Player.matchesCondition(condition: Expr): Boolean {
    if (condition.kind is ExprKind.Lit) return ((condition.kind as ExprKind.Lit).lit as Lit.Bool).value

    when (condition.kind) {
        is ExprKind.Binary -> {
            val bin = condition.kind as ExprKind.Binary
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
        is ExprKind.Condition -> {
            val cond = condition.kind as ExprKind.Condition
            when (cond.condition) {
                is Condition.RequiredPlaceholderNumber -> {
                    val rpn = cond.condition
                    val value = statValue(rpn.amount)
                    return when (rpn.mode) {
                        Comparison.Eq -> exprValue(rpn.placeholder) == value
                        Comparison.Gt -> exprValue(rpn.placeholder) > value
                        Comparison.Ge -> exprValue(rpn.placeholder) >= value
                        Comparison.Lt -> exprValue(rpn.placeholder) < value
                        Comparison.Le -> exprValue(rpn.placeholder) <= value
                    }
                }
                else -> error("unexpected")
            }
        }
        else -> {}
    }

    error("unexpected")
}