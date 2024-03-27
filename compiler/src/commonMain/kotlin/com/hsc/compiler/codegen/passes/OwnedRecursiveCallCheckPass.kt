package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

object OwnedRecursiveCallCheckPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            OwnedRecursiveCallVisitor(sess.dcx(), it.ident).visitItem(it)
        }
    }

}

private class OwnedRecursiveCallVisitor(val dcx: DiagCtx, val ident: Ident) : AstVisitor {
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
                    val err = dcx.err("owned recursive call")
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