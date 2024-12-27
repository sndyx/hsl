package com.hsc.compiler.codegen

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Function
import com.hsc.compiler.ir.action.FunctionProcessor
import com.hsc.compiler.ir.action.ItemStack
import com.hsc.compiler.ir.action.ItemStackSerializer
import com.hsc.compiler.ir.action.Location
import com.hsc.compiler.ir.action.Location.*
import com.hsc.compiler.ir.action.StatOp
import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

class ActionTransformer(internal val sess: CompileSess) {

    fun transform(ast: Ast): List<Function> {
        return ast.items.filter {
            it.kind is ItemKind.Fn
        }.map { item ->
            val fn = (item.kind as ItemKind.Fn).fn

            val processors = fn.processors?.let { transformProcessors(it.list) } ?: emptyList()
            Function(item.ident.name, transformBlock(fn.block), processors)
        }
    }

    fun transformProcessors(list: List<Processor>): List<FunctionProcessor> {
        return list.map { proc ->
            val args = proc.args.args.map { arg ->
                if (arg.lit() == null) {
                    throw sess.dcx().bug("processor arguments must be literals", arg.span)
                }

                val lit = arg.lit()!!.lit
                when (lit) {
                    is Lit.Location -> {
                        val x = lit.x?.let(::expectFloat)
                        val y = lit.y?.let(::expectFloat)
                        val z = lit.z?.let(::expectFloat)
                        val pitch = lit.pitch?.let(::expectFloat)
                        val yaw = lit.yaw?.let(::expectFloat)
                        val loc = Custom(
                            lit.relX, lit.relY, lit.relZ, x, y, z, pitch?.toFloat(), yaw?.toFloat()
                        )
                        Json.encodeToJsonElement(loc)
                    }
                    is Lit.Str -> {
                        JsonPrimitive(lit.value)
                    }
                    is Lit.I64 -> {
                        JsonPrimitive(lit.value)
                    }
                    is Lit.F64 -> {
                        JsonPrimitive(lit.value)
                    }
                    is Lit.Bool -> {
                        JsonPrimitive(lit.value)
                    }
                    is Lit.Item -> {
                        Json.encodeToJsonElement(ItemStackSerializer, ItemStack(lit.value.nbt, lit.value.name))
                    }

                    Lit.Null -> error("man")
                }
            }
            FunctionProcessor(proc.ident, args)
        }
    }

    private fun expectFloat(expr: Expr): Double {
        return when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> lit.value.toDouble()
                    is Lit.F64 -> lit.value.toDouble()
                    else -> {
                        throw sess.dcx().err("expected float, found ${lit.str()}")
                    }
                }
            }

            else -> {
                throw sess.dcx().err("expected float, found ${kind.str()}")
            }
        }
    }

    fun transformBlock(block: Block): List<Action> =
        block.stmts.map(::transformStmt)

    internal fun strict(span: Span, block: () -> Exception): Nothing {
        if (sess.opts.mode == Mode.Strict) {
            throw block()
        } else {
            throw sess.dcx().bug("unlowered construct", span)
        }
    }

    internal fun unsupported(message: String, span: Span): Diagnostic {
        return sess.dcx().err("cannot use $message in `strict` mode", span)
    }

    internal fun unwrapStatValue(expr: Expr): StatValue = when (val kind = expr.kind) {
        is ExprKind.Lit -> {
            when (val lit = kind.lit) {
                is Lit.I64 -> {
                    StatValue.I64(lit.value)
                }
                is Lit.F64 -> {
                    StatValue.I64(lit.value.toLong())
                }
                is Lit.Str -> {
                    StatValue.Str(lit.value)
                }
                is Lit.Bool -> {
                    StatValue.I64(if (lit.value) 1 else 0)
                }
                else -> {
                    throw sess.dcx().err("expected integer or placeholder")
                }
            }
        }
        is ExprKind.Var -> {
            StatValue.Str(when (val ident = kind.ident) {
                is Ident.Player -> "%stat.player/${ident.name}%"
                is Ident.Global -> "%stat.global/${ident.name}%"
                is Ident.Team -> "%stat.team/${ident.name} ${ident.team}%"
            })
        }
        else -> {
            val err = sess.dcx().err("expected integer or placeholder")
            err.span(expr.span)
            if (sess.opts.mode == Mode.Strict) {
                err.note(Level.Error, "cannot use complex expressions in `strict` mode")
            }
            throw err
        }
    }

    fun ActionTransformer.expectArgs(span: Span, args: List<Expr>, expected: Int) {
        if (args.size != expected) {
            val s1 = if (expected == 1) "" else "s"
            val s2 = if (args.size == 1) "" else "s"
            val was = if (args.size == 1) "was" else "were"
            val err = sess.dcx().err("this function takes $expected parameter$s1 but ${args.size} parameter$s2 $was supplied")
            err.span(span)
        }
    }

    fun ActionTransformer.unwrapString(expr: Expr): String =
        when (val kind = expr.kind) {
            is ExprKind.Lit -> when (val lit = kind.lit) {
                is Lit.Str -> lit.value
                else -> {
                    sess.dcx()
                        .err("expected string, found ${lit.str()}", expr.span)
                        .emit()
                    "error"
                }
            }
            is ExprKind.Var -> {
                identString(kind.ident)
            }
            else -> {
                sess.dcx().err("expected string, found ${expr.kind.str()}")
                "error"
            }
        }


    internal fun unwrapStatOp(op: BinOpKind): StatOp {
        return when (op) {
            BinOpKind.Add -> StatOp.Inc
            BinOpKind.Sub -> StatOp.Dec
            BinOpKind.Mul -> StatOp.Mul
            BinOpKind.Div -> StatOp.Div
            else -> {
                throw sess.dcx().err("unexpected operator")
            }
        }
    }

    private fun identString(ident: Ident): String = when (ident) {
        is Ident.Player -> "%stat.player/${ident.name}%"
        is Ident.Global -> "%stat.global/${ident.name}%"
        is Ident.Team -> "%stat.team/${ident.name} ${ident.team}%"
    }

}