package com.hsc.compiler.codegen

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Ident

fun ActionTransformer.transformExpr(expr: Expr): Action {
    return when (val kind = expr.kind) {
        is ExprKind.Binary -> {
            val err = sess.dcx().err("expected statement, found binary expression")
            err.span(expr.span)
            val op: String? = when (kind.kind) {
                BinOpKind.Add -> "+"
                BinOpKind.Sub -> "-"
                BinOpKind.Mul -> "*"
                BinOpKind.Div -> "/"
                BinOpKind.Rem -> "%"
                BinOpKind.Pow -> "^"
                else -> null
            }
            if (op != null) {
                err.note(Level.Hint, "did you mean `$op=`?")
            }
            throw err
        }
        is ExprKind.Call -> {
            if (kind.args.isEmpty()) {
                makeCall(kind.ident)
            } else {
                strict(expr.span) {
                    unsupported("function arguments", expr.span)
                }
            }
        }
        is ExprKind.If -> {
            return transformCond(kind)
        }
        is ExprKind.Match -> {
            TODO()
        }
        is ExprKind.Block -> {
            strict(expr.span) {
                unsupported("block as expression", expr.span)
            }
        }
        is ExprKind.Paren -> {
            strict(expr.span) {
                unsupported("parenthesized expressions", expr.span)
            }
        }
        is ExprKind.Unary -> {
            strict(expr.span) {
                unsupported("unary expressions", expr.span)
            }
        }
        is ExprKind.Lit, is ExprKind.Var -> {
            throw sess.dcx().err("unexpected expression", expr.span)
        }
    }
}

private fun ActionTransformer.makeCall(ident: Ident): Action {
    return Action.ExecuteFunction(ident.name, ident.global)
}