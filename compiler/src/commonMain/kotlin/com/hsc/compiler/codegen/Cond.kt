package com.hsc.compiler.codegen

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Comparison
import com.hsc.compiler.ir.action.Condition
import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

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
                    val single = unwrapCond(cond.expr)!! // binary expression is safe to assert
                    return Action.Conditional(listOf(single), false, block, other)
                }
            }
        }
        is ExprKind.Var, is ExprKind.Lit -> true
        is ExprKind.Condition -> true
        is ExprKind.Unary -> true
        else -> {
            // This should almost certainly never happen unless in `strict` mode.
            // But on the off chance it does, here's a bug that creates a bug!

            // UPDATE 2/9/25: it created a bug :despair:
            strict(cond.expr.span) {
                unsupported("complex expressions", cond.expr.span)
            }
        }
    }

    val res = mutableListOf<Expr>()
    val dfs = mutableListOf(cond.expr)
    while (dfs.isNotEmpty()) {
        val expr = dfs.removeLast()
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                when (kind.kind) {
                    BinOpKind.And, BinOpKind.Or -> {
                        if ((kind.kind == BinOpKind.And && and) || (kind.kind == BinOpKind.Or && !and)) {
                            // Operands match
                            dfs.add(kind.a)
                            dfs.add(kind.b)
                        } else {
                            strict(expr.span) {
                                // This span should be fine
                                val span = Span(kind.a.span.hi + 1, kind.b.span.lo - 1, kind.a.span.fid)
                                val type = if (and) "&&" else "||"
                                val not = if (!and) "&&" else "||"
                                sess.dcx().err("mismatched conditional operand $not, expected $type", span)
                            }
                        }
                    }
                    BinOpKind.Lt, BinOpKind.Le, BinOpKind.Gt, BinOpKind.Ge, BinOpKind.Eq -> {
                        res.add(expr) // This is a good expression, probably
                    }
                    BinOpKind.Ne -> {
                        strict(expr.span) {
                            sess.dcx().err("cannot use != in `strict` mode", expr.span)
                        }
                    }
                    else -> {
                        strict(expr.span) {
                            sess.dcx().err("cannot use complex expressions in `strict` mode", expr.span)
                        }
                    }
                }
            }
            is ExprKind.Condition, is ExprKind.Var, is ExprKind.Lit, is ExprKind.Unary -> {
                res.add(expr)
            }
            else -> {
                throw sess.dcx().err("expected binary expression, found ${kind.str()}", expr.span)
            }
        }
    }

    return Action.Conditional(res.mapNotNull(this::unwrapCond), !and, block, other)
}

private fun ActionTransformer.unwrapCond(cond: Expr): Condition? {
    var new: Condition? = null
    if (cond.unary()?.kind?.equals(UnaryOpKind.Not) == true) {
        new = unwrapCond0(cond.unary()!!.expr)
        new?.inverted = true
    } else {
        new = unwrapCond0(cond)
    }
    return new
}

private fun ActionTransformer.unwrapCond0(cond: Expr): Condition? {
    return when (val kind = cond.kind) {
        is ExprKind.Condition -> {
            kind.condition
        }
        is ExprKind.Binary -> {
            val comparison = when (kind.kind) {
                BinOpKind.Lt -> Comparison.Lt
                BinOpKind.Le -> Comparison.Le
                BinOpKind.Gt -> Comparison.Gt
                BinOpKind.Ge -> Comparison.Ge
                BinOpKind.Eq -> Comparison.Eq
                else -> {
                    throw sess.dcx().bug("unexpected bin op", cond.span)
                }
            }

            val ident = when (val a = kind.a.kind) {
                is ExprKind.Var -> a.ident
                is ExprKind.Lit -> {
                    if (a.lit is Lit.Str) {
                        val other = unwrapStatValue(kind.b)
                        return Condition.RequiredPlaceholderNumber((a.lit as Lit.Str).value, comparison, other)
                    } else {
                        throw sess.dcx().err("left side of a comparison must be a variable", cond.span)
                    }
                }
                else -> {
                    throw sess.dcx().err("left side of a comparison must be a variable", cond.span)
                }
            }
            val other = unwrapStatValue(kind.b)
            when (ident) {
                is Ident.Player -> Condition.PlayerStatRequirement(ident.name, comparison, other)
                is Ident.Global -> Condition.GlobalStatRequirement(ident.name, comparison, other)
                is Ident.Team -> Condition.TeamStatRequirement(ident.name, ident.team, comparison, other)
            }
        }
        is ExprKind.Lit -> {
            when (val lit = kind.lit) {
                is Lit.Bool -> {
                    if (!lit.value) Condition.PlayerStatRequirement("@nothing", Comparison.Eq, StatValue.I64(1))
                    else null
                }
                is Lit.I64 -> {
                    when (lit.value) {
                        0L -> Condition.PlayerStatRequirement("@nothing", Comparison.Eq, StatValue.I64(0))
                        1L -> null
                        else -> throw sess.dcx().err("expected condition, found literal", cond.span)
                    }
                }
                else -> {
                    throw sess.dcx().err("expected condition, found literal", cond.span)
                }
            }
        }
        is ExprKind.Var -> {
            val ident = kind.ident
            when (ident) {
                is Ident.Player -> Condition.PlayerStatRequirement(ident.name, Comparison.Eq, StatValue.I64(1))
                is Ident.Global -> Condition.GlobalStatRequirement(ident.name, Comparison.Eq, StatValue.I64(1))
                is Ident.Team -> Condition.TeamStatRequirement(ident.name, ident.team, Comparison.Eq, StatValue.I64(1))
            }
        }
        else -> {
            throw sess.dcx().bug("unexpected conditional", cond.span)
        }
    }
}