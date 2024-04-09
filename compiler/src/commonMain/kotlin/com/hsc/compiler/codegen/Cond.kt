package com.hsc.compiler.codegen

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Condition
import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind

/**
 * Transforms an [ExprKind.If] into an [Action.Conditional].
 */
fun ActionTransformer.transformCond(cond: ExprKind.If): Action {
    val block = transformBlock(cond.block)
    val other = cond.other?.let { transformBlock(it) } ?: emptyList()
    // Represents whether the conditional is an AND conditional or an OR conditional
    val and = when (val kind = cond.expr.kind) {
        is ExprKind.Binary -> {
            when (kind.kind) {
                BinOpKind.And -> true
                BinOpKind.Or -> false
                else -> {
                    // single condition, unwrap and return
                    val single = unwrapCond(cond.expr)
                    return Action.Conditional(listOf(single), false, block, other)
                }
            }
        }
        else -> {
            // This should almost certainly never happen unless in `strict` mode.
            // But on the off chance it does, here's a bug that creates a bug!
            strict(cond.expr.span) {
                unsupported("complex expressions", cond.expr.span)
            }
        }
    }

    val dfs = mutableListOf(cond.expr)
    while (dfs.isNotEmpty()) {
        val expr = dfs.removeLast()
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                if (kind.kind == BinOpKind.And || kind.kind == BinOpKind.Or) {
                    TODO()
                }
            }
            else -> {
                throw sess.dcx().err("expected binary expression, found ${kind.str()}", expr.span)
            }
        }
    }

    TODO("transformCond")
}

private fun ActionTransformer.unwrapCond(expr: Expr): Condition {
    TODO("unwrapCond")
}

// Note: && holds higher precedence than || in parser
// This should make the algorithm simpler.

// BEWARE! PROCEED INTO RAMBLINGS WITH CAUTION!

// x || y && z || w

// if (x || y) -> temp = 1
// if (z || w) -> temp++
// if (temp == 2) -> hsl:if
// else -> hsl:else

// x && y || z || w

// if (y || z || w) -> temp = 1
// if (x && temp == 1) hsl:if
// else -> hsl:else

// x && y || z && w || a || b || c

// if (y || z) -> temp = 1
// if (w || a || b || c) -> temp++
// if (x && temp == 2) hsl:if
// else -> hsl:else

// (x && y) || (z && w)

// if (x && y) -> temp = 1
// if (z && w) -> temp = 1
// if (temp == 1) -> hsl:if
// else -> hsl:else

// (x && y) || z <-- single value end case

// if (x && y) -> temp = 1
// if (z || temp == 1) -> hsl:if
// else -> hsl:else


// if ((x && y) || z) && w || a

// if (x && y) -> temp = 1
// if (z || temp == 1) -> temp = 2
// if (w || a) -> temp2 = 1
// if (temp == 2 && temp2 == 1) -> hsl:if
// else -> hsl:else

// if ((x && y) || z) && w && a

// if (x && y) -> temp = 1
// if (z || temp == 1) -> temp = 2
// if (w && a && temp == 2) -> hsl:if
// else -> hsl:else