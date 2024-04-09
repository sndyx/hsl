package com.hsc.compiler.codegen

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

fun ActionTransformer.unwrapAction(action: Stmt): Action {
    val kind = action.kind as StmtKind.Action // assertion should never fail!

    return when (kind.name) {
        "send_message" -> {
            expectArgs(action.span, kind.exprs, 1)
            val message = unwrapString(kind.exprs[0])
            Action.SendMessage(message)
        }
        else -> error("unknown built-in action")
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
