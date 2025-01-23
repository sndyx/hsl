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
                        throw ctx.dcx().err("expected string, found ${lit.str()}", expr.span)
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected string, found ${kind.str()}", expr.span)
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
                        throw ctx.dcx().err("expected integer, found ${lit.str()}", expr.span)
                    }
                }
            }

            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}", expr.span)
            }
        }
    }

    private fun expectFloat(expr: Expr): Double {
        return when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> lit.value.toDouble()
                    is Lit.F64 -> lit.value.toDouble()
                    else -> {
                        throw ctx.dcx().err("expected float, found ${lit.str()}", expr.span)
                    }
                }
            }

            else -> {
                throw ctx.dcx().err("expected float, found ${kind.str()}", expr.span)
            }
        }
    }

    fun nextFloatLit(): Double = expectFloat(bump())

    fun nextValue(): StatValue {
        val expr = bump()
        return ctx.statValueOf(expr)
    }

    fun nextBooleanLit(): Boolean {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Bool -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected boolean, found ${lit.str()}", expr.span)
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected boolean, found ${kind.str()}", expr.span)
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
                        throw ctx.dcx().err("expected item, found ${lit.str()}", expr.span)
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected item, found ${kind.str()}", expr.span)
            }
        }
    }

    fun nextLocation(): Location {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                return when (val lit = kind.lit) {
                    is Lit.Location -> {
                        val x = lit.x?.let(::expectFloat)
                        val y = lit.y?.let(::expectFloat)
                        val z = lit.z?.let(::expectFloat)
                        val pitch = lit.pitch?.let(::expectFloat)
                        val yaw = lit.yaw?.let(::expectFloat)
                        Location.Custom(
                            lit.relX, lit.relY, lit.relZ, x, y, z, pitch?.toFloat(), yaw?.toFloat()
                        )
                    }

                    is Lit.Str -> when (lit.value) {
                        "house_spawn" -> Location.HouseSpawn
                        "current_location" -> Location.CurrentLocation
                        "invokers_location" -> Location.InvokersLocation
                        else -> throw ctx.dcx().err("expected location, found ${lit.str()}", expr.span)
                    }

                    else -> {
                        throw ctx.dcx().err("expected location, found ${lit.str()}", expr.span)
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected location, found ${kind.str()}", expr.span)
            }
        }
    }


    fun bump(): Expr = args.args[pos++]

}