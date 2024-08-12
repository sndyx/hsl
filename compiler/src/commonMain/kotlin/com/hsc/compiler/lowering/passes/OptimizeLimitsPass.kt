package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.Args
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Fn
import com.hsc.compiler.ir.ast.FnSig
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.limitsMap
import com.hsc.compiler.lowering.stmtActionKind
import com.hsc.compiler.span.Span

object OptimizeLimitsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>()
            .filter { it.kind is ItemKind.Fn }
            .forEach { item ->
                val fn = (item.kind as ItemKind.Fn).fn
                optimizeLimits(ctx, item.ident.name, fn.block.stmts.iterator())
            }
    }

}

private fun optimizeLimits(ctx: LoweringCtx, fnName: String, iter: Iterator<Stmt>, continuation: Int = 0): List<Stmt> = buildList {
    val limits = limitsMap.toMutableMap()
    fun conditionals(): Int = limits["CONDITIONAL"]!!

    while (iter.hasNext()) {
        var stmt = iter.next()

        if (limits.getValue(stmtActionKind(stmt)) == 0) {
            if (conditionals() == 0) {
                // create function continuation
                val stmts = optimizeLimits(ctx, fnName, iter, continuation + 1)
                val blockSpan = Span(stmts.first().span.lo, stmts.last().span.hi, stmts.first().span.fid)
                val block = Block(blockSpan, stmts.toMutableList())
                val fn = Fn(null, FnSig(Span.none, emptyList()), block)
                val ident = Ident.Player(fnName + "_c" + continuation)

                ctx.ast.items.add(Item(blockSpan, ident, ItemKind.Fn(fn)))

                val callExpr = Expr(Span.none, ExprKind.Call(ident, Args(Span.none, mutableListOf())))
                stmt = Stmt(Span.none, StmtKind.Expr(callExpr))
            } else {
                // create new conditional
                val stmts = createBlock(iter)
                val blockSpan = Span(stmts.first().span.lo, stmts.last().span.hi, stmts.first().span.fid)
                val block = Block(blockSpan, stmts.toMutableList())

                val ifExpr = Expr(blockSpan, ExprKind.If(Expr(Span.none, ExprKind.Lit(Lit.Bool(true))), block, null))
                stmt = Stmt(Span.none, StmtKind.Expr(ifExpr))
            }
        }

        add(stmt)

        val kind = stmtActionKind(stmt)
        limits[kind] = limits.getValue(kind) - 1
    }
}

private fun createBlock(iter: Iterator<Stmt>) = buildList {
    val limits = limitsMap.toMutableMap()
    var hasPaused = false

    while (iter.hasNext()) {
        val stmt = iter.next()
        val kind = stmtActionKind(stmt)
        val kindRemaining = limits.getValue(kind)

        if (kindRemaining == 0) break // out of this action, exit block
        add(stmt)

        if (kind == "CONDITIONAL" || (hasPaused && kind == "PAUSE")) break // conditional or second pause, exit block
        if (kind == "PAUSE") hasPaused = true

        limits[kind] = kindRemaining - 1
    }
}