package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object CheckOwnedRecursiveCallPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach {
                OwnedRecursiveCallVisitor(ctx, it.ident).visitItem(it)
            }
    }

}

private class OwnedRecursiveCallVisitor(val ctx: LoweringCtx, val ident: Ident) : AstVisitor {
    var currentFnDeclSpan: Span? = null
    override fun visitItem(item: Item) {
        if (item.kind is ItemKind.Fn) {
            // Don't have a good way to capture this otherwise...
            currentFnDeclSpan = Span(item.span.lo, item.kind.fn.sig.span.hi, item.span.fid)
        }
        super.visitItem(item)
    }
    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident == ident) {
                    val err = ctx.dcx().err("owned recursive call")
                    err.reference(currentFnDeclSpan!!, "function declared here")
                    err.spanLabel(expr.span, "self referential call")
                    err.emit()
                }
            }
            else -> { }
        }
        super.visitExpr(expr)
    }
}