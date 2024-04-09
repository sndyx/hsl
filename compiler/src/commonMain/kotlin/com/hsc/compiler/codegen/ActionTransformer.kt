package com.hsc.compiler.codegen

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.CompileException
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Function
import com.hsc.compiler.ir.action.StatOp
import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

class ActionTransformer(internal val sess: CompileSess) {

    fun transform(ast: Ast): List<Function> {
        return ast.items.filter{
            it.kind is ItemKind.Fn
        }.map { item ->
            val fn = (item.kind as ItemKind.Fn).fn

            Function(item.ident.name, transformBlock(fn.block))
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
                is Lit.Str -> {
                    StatValue.Str(lit.value)
                }
                else -> {
                    val err = sess.dcx().err("expected integer or placeholder")
                    throw CompileException(err)
                }
            }
        }
        is ExprKind.Var -> {
            StatValue.Str(if (kind.ident.global) {
                "%stat.global/${kind.ident.name}%"
            } else {
                "%stat.player/${kind.ident.name}%"
            })
        }
        else -> {
            val err = sess.dcx().err("expected integer or placeholder")
            err.span(expr.span)
            if (sess.opts.mode == Mode.Strict) {
                err.note(Level.Error, "cannot use complex expressions in `strict` mode")
            }
            throw CompileException(err)
        }
    }

    internal fun unwrapStatOp(op: BinOpKind): StatOp {
        TODO("unwrapStatOp")
    }

    internal fun identString(ident: Ident): String =
        if (ident.global) "%stat.global/${ident.name}%"
        else "%stat.player/${ident.name}%"

}