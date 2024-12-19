package com.hsc.compiler.lowering.passes

import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.*
import com.hsc.compiler.span.Span

object CheckLimitsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        if (ctx.sess.opts.mode == Mode.Optimize) {
            ctx.query<Item>().filter { it.kind is ItemKind.Fn }.forEach { fn ->
                limitOpt(ctx, fn, (fn.kind as ItemKind.Fn).fn.block)
            }
        }
        val mainBlock = (ctx.query<Item>().filter { it.kind is ItemKind.Fn }.find { it.ident.name == "main" }?.kind as? ItemKind.Fn)?.fn?.block
        ctx.query<Block>().forEach { block ->
            if (block != mainBlock) {
                limitCheck(ctx, block)
            }
        }
    }

}

private fun limitOpt(ctx: LoweringCtx, item: Item, block: Block) {
    if (item.ident.name == "main") return
    if (checkLimits(block) == null) return
    // limits surpassed, somewhere

    val currentLimits = limitsMap.toMutableMap()

    var pos = 0
    while (pos < block.stmts.size) {
        val kind = stmtActionKind(block.stmts[pos])

        currentLimits[kind] = currentLimits[kind]!! - 1
        if (currentLimits[kind] == -1) { // we ran out of the current action kind
            currentLimits[kind] = 0
            if (currentLimits["CONDITIONAL"]!! < 1) {
                // make function continuation
                val contStmts = mutableListOf<Stmt>()
                while (block.stmts.size > pos) {
                    contStmts.add(block.stmts.removeAt(pos))
                }
                val newFn = Fn(null, FnSig(Span.none, emptyList()), Block(item.span, contStmts))

                val name = item.ident.name
                val newName = if (
                    name.split("_").lastOrNull()?.startsWith("c") == true // _c
                    && name.split("_").last().drop(1).toIntOrNull() != null // _cInteger
                    ) {
                    val part = name.split("_").dropLast(1).joinToString("_")
                    part + "_c" + (name.split("_").last().drop(1).toInt() + 1)
                } else name + "_c2"

                val newItem = Item(item.span, Ident.Player(newName), ItemKind.Fn(newFn))
                ctx.ast.items.add(newItem)
                limitOpt(ctx, newItem, newFn.block)

                val callStmt = Stmt(
                    Span.none, StmtKind.Expr(
                        Expr(Span.none, ExprKind.Call(Ident.Player(newName), Args(Span.none, mutableListOf())))
                    )
                )
                block.stmts.add(callStmt)

                return
            }

            val innerLimits = limitsMap.toMutableMap()

            val stmts = mutableListOf<Stmt>()

            var count = 0
            while (pos + count < block.stmts.size) {
                val innerKind = stmtActionKind(block.stmts[pos + count])
                if (innerKind == "CONDITIONAL") break
                innerLimits[innerKind] = innerLimits[innerKind]!! - 1
                if (innerLimits[kind] == -1) break
                stmts.add(block.stmts[pos + count])
                count++
            }

            val stmt = Stmt(block.span, StmtKind.Expr(Expr(block.span, ExprKind.If(
                Expr(Span.none, ExprKind.Lit(Lit.Bool(true))),
                Block(block.span, stmts), null
            )))) // if stmt
            block.stmts[pos] = stmt

            for (i in pos..(pos + count - 2)) {
                block.stmts.removeAt(pos + 1)
            }
            currentLimits["CONDITIONAL"] = currentLimits["CONDITIONAL"]!! - 1
        }
        pos++
    }
}

private fun limitCheck(ctx: LoweringCtx, block: Block) {
    val (action, span) = checkLimits(block) ?: return
    val err = ctx.dcx().err("action limit surpassed: `${action.lowercase()}`")
    if (span == Span.none) {
        err.spanLabel(block.span, "in this scope")
    } else {
        err.spanLabel(span, "with this statement")
    }
    if (ctx.sess.opts.mode != Mode.Strict) {
        err.note(Level.Error, "could not optimize out actions")
    }
    err.emit()
}