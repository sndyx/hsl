package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.walk

fun inlineFunctions(ctx: LoweringCtx) = with(ctx) {
    val stmts = query<Stmt>()
    stmts.forEach { stmt ->
        walk(stmt) { expr ->
            val call = expr.call() ?: return@walk

            // find a function associated with this call, or return
            val fn = (ctx.query<Item>()
                .find { it.ident == call.ident }
                ?.kind as? ItemKind.Fn)?.fn ?: return@walk

            // make sure this is an inline function
            fn.processors?.list?.find { it == "inline" } ?: return@walk

            val body = fn.block.deepCopy()

            // backwards inline args
            fn.sig.args.forEach { arg ->
                // check if this arg is ever reassigned
                val isMutated = body.stmts
                    .map { it.assign()?.ident ?: it.assignOp()?.ident }
                    .any { it == arg }

                val replacement = if (isMutated) {
                    //val ident = firstAvailableTemp(ctx, fn, )
                    //body.stmts.add(0, Stmt(Span.none, StmtKind.Assign()))
                } else {

                }
            }
        }
    }
}