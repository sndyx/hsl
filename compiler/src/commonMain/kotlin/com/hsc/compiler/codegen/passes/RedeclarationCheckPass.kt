package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.span.Span

object RedeclarationCheckPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }.sortedBy { it.span.lo }
        val fnNames = mutableMapOf<String, Span>()

        functions.forEach {
            val fn = (it.kind as ItemKind.Fn).fn
            if (fnNames.containsKey(it.ident.name)) {
                val err = sess.dcx().err("function declared twice")
                err.reference(fnNames[it.ident.name]!!, "previously declared here")
                err.spanLabel(Span(it.span.lo, fn.sig.span.hi, it.span.fid), "redeclared here")
                err.emit()
            } else {
                fnNames[it.ident.name] = Span(it.span.lo, fn.sig.span.hi, it.span.fid)
            }
        }
    }

}
