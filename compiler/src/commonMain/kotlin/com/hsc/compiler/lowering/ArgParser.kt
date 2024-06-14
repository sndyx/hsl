package com.hsc.compiler.lowering

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.ItemStack
import com.hsc.compiler.ir.action.Location
import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.Args
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Lit

class ArgParser(val ctx: LoweringCtx, val args: Args) {

    var pos = 0

    fun assertLength(length: Int, usage: String) {
        if (args.args.size != length) {
            val s1 = if (length == 1) "" else "s"
            val s2 = if (args.args.size == 1) "" else "s"
            val was = if (args.args.size == 1) "was" else "were"
            val err = ctx.dcx()
                .err("this function takes $length parameter$s1 but ${args.args.size} parameter$s2 $was supplied")
            err.span(args.span)
            err.note(Level.Hint, "usage: $usage")
            throw err
        }
    }

    fun nextStringLit(): String {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Str -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected string, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected string, found ${kind.str()}")
            }
        }
    }

    fun nextNumberLit(): Long {
        val expr = bump()
        return when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> lit.value
                    is Lit.F64 -> lit.value.toLong()
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }

            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextFloatLit(): Double {
        val expr = bump()
        return when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> lit.value.toDouble()
                    is Lit.F64 -> lit.value.toDouble()
                    else -> {
                        throw ctx.dcx().err("expected float, found ${lit.str()}")
                    }
                }
            }

            else -> {
                throw ctx.dcx().err("expected float, found ${kind.str()}")
            }
        }
    }

    fun nextValue(): StatValue {
        val expr = bump()
        return when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> StatValue.I64(lit.value)
                    is Lit.F64 -> StatValue.I64(lit.value.toLong())
                    is Lit.Str -> StatValue.Str(lit.value)
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            is ExprKind.Var -> {
                if (kind.ident.isGlobal) StatValue.Str("%stat.global/${kind.ident.name}%")
                else StatValue.Str("%stat.player/${kind.ident.name}%")
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextBooleanLit(): Boolean {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Bool -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextItemLit(): ItemStack {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Item -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextLocation(): Location {
        TODO()
    }


    fun bump(): Expr = args.args[pos++]

}