package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.limits
import com.hsc.compiler.lowering.optimizeLimits
import com.hsc.compiler.span.Span

object CleanupTempVarsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val cleanUp = mutableSetOf<Ident>()

        ctx.query<Item>().filter { it.kind is ItemKind.Fn }.forEach { item ->
            val fn = (item.kind as ItemKind.Fn).fn

            val visitor = CleanupTempVarsVisitor(ctx)
            visitor.visitBlock(fn.block)

            val tempPlayer = visitor.tempVars.filter { it is Ident.Player }.toMutableList()
            val tempGlobal = visitor.tempVars.filter { it is Ident.Global }.toMutableList()
            val tempTeam = visitor.tempVars.filter { it is Ident.Team }.toMutableList()

            val map = mapOf(
                "CHANGE_STAT" to tempPlayer,
                "CHANGE_GLOBAL_STAT" to tempGlobal,
                "CHANGE_TEAM_STAT" to tempTeam,
            )

            val localLimits = limits(fn.block).toMutableMap()

            map.forEach { (key, values) ->
                repeat(localLimits[key]!!) {
                    if (values.isNotEmpty()) {
                        val stmt = Stmt(Span.none, StmtKind.Assign(
                            values.removeFirst(), Expr(Span.none, ExprKind.Lit(Lit.I64(0)))
                        ))
                        fn.block.stmts.add(stmt)
                    }
                }
            }

            var conditionals = localLimits["CONDITIONAL"]!!

            var changed = false
            if (conditionals > 0) {
                do {
                    changed = false
                    map.forEach { (_, values) ->
                        repeat(10) {
                            if (values.isNotEmpty()) {
                                val stmt = Stmt(
                                    Span.none, StmtKind.Assign(
                                        values.removeFirst(), Expr(Span.none, ExprKind.Lit(Lit.I64(0)))
                                    )
                                )
                                fn.block.stmts.add(stmt)
                                changed = true
                            }
                        }
                    }
                    conditionals--
                } while (changed && conditionals > 0)
            }

            cleanUp.addAll(map.flatMap { it.value })
        }

        if (cleanUp.isNotEmpty()) {
            val block = Block(Span.none, mutableListOf())

            cleanUp.forEach {
                val stmt = Stmt(
                    Span.none, StmtKind.Assign(
                        it, Expr(Span.none, ExprKind.Lit(Lit.I64(0)))
                    )
                )
                block.stmts.add(stmt)
            }

            val fn = Fn(null, FnSig(Span.none, emptyList()), block)
            val item = Item(Span.none, Ident.Player("cleanup"), ItemKind.Fn(fn))

            ctx.ast.items.add(item)
            ctx.clearQuery<Item>() // Remove cached queries as we have inserted a new element
        }
    }

}

private class CleanupTempVarsVisitor(val ctx: LoweringCtx) : AstVisitor {

    val tempVars = mutableSetOf<Ident>()

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.Assign -> {
                if (kind.ident.name.startsWith(ctx.sess.opts.tempPrefix)) {
                    tempVars.add(kind.ident)
                }
            }
            is StmtKind.AssignOp -> {
                if (kind.ident.name.startsWith(ctx.sess.opts.tempPrefix)) {
                    tempVars.add(kind.ident)
                }
            }
            else -> super.visitStmt(stmt)
        }
    }

}