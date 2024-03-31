package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object RedeclarationCheckPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }.sortedBy { it.span.lo }
        val fnNames = mutableMapOf<String, Span>()

        functions.forEach {
            val fn = (it.kind as ItemKind.Fn).fn
            if (fnNames.containsKey(it.ident.name)) {
                val err = ctx.dcx().err("function declared twice")
                err.reference(fnNames[it.ident.name]!!, "previously declared here")
                err.spanLabel(Span(it.span.lo, fn.sig.span.hi, it.span.fid), "redeclared here")
                err.emit()
            } else {
                fnNames[it.ident.name] = Span(it.span.lo, fn.sig.span.hi, it.span.fid)
            }
        }
    }

}
