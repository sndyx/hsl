package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

object CleanupTempVarsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val cleanUp = mutableSetOf<Ident>()

        ctx.query<Stmt>().forEach { stmt ->
            when (val kind = stmt.kind) {
                is StmtKind.Assign -> {
                    if (kind.ident.name.startsWith("_")) cleanUp.add(kind.ident)
                }
                is StmtKind.AssignOp -> {
                    if (kind.ident.name.startsWith("_")) cleanUp.add(kind.ident)
                }
                else -> { /* Ignore */ }
            }
        }

        val block = Block(Span.none, mutableListOf())

        cleanUp.forEach {
            val stmt = Stmt(Span.none, StmtKind.Assign(
                it, Expr(Span.none, ExprKind.Lit(Lit.I64(0)))
            ))
            block.stmts.add(stmt)
        }

        val fn = Fn(null, FnSig(Span.none, emptyList()), block)
        val item = Item(Span.none, Ident(false, "cleanup"), ItemKind.Fn(fn))

        ctx.ast.items.add(item)
        ctx.clearQuery<Item>() // Remove cached queries as we have inserted a new element
    }

}