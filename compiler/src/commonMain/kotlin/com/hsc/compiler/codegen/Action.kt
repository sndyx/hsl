package com.hsc.compiler.codegen

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.*

fun ActionTransformer.unwrapAction(action: Expr): Action {
    val kind = action.kind as ExprKind.Action // assertion should never fail!

    return kind.action
}