package com.hsc.compiler.codegen

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

fun ActionTransformer.unwrapAction(action: Expr): Action {
    val kind = action.kind as ExprKind.Action // assertion should never fail!

    return when (kind.name) {
        "send_message" -> {
            expectArgs(action.span, kind.exprs, 1)
            val message = unwrapString(kind.exprs[0])
            Action.SendMessage(message)
        }
        else -> error("unknown built-in action")
    }
}