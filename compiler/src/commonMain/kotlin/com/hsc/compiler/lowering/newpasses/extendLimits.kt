package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.Args
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
import com.hsc.compiler.lowering.getFunctionItems
import com.hsc.compiler.lowering.limitsMap
import com.hsc.compiler.lowering.stmtActionKind
import com.hsc.compiler.lowering.wrap
import com.hsc.compiler.span.Span
import kotlin.collections.set

fun extendLimits(ctx: LoweringCtx) = with(ctx) {
    getFunctionItems().forEach { (item, fn) ->
        if (item.ident.name == "main") return@forEach // don't optimize main
        if (fn.processors?.list?.any { it.ident == "strict" } == true) return@forEach

        extendLimits(ctx, item)
    }
}

private fun extendLimits(ctx: LoweringCtx, item: Item) = with(ctx) {
    val fn = (item.kind as ItemKind.Fn).fn

    val limits = limitsMap.toMutableMap()

    var stmts = fn.block.stmts

    var pos = 0
    while (pos < stmts.size) {
        val stmt = stmts[pos]
        val kind = stmtActionKind(stmt)

        if (limits.getValue(kind) < 1) {

            // first, try to make a conditional
            if (limits.getValue("CONDITIONAL") > 0) {

                val innerLimits = limitsMap.toMutableMap()
                val innerStmts = mutableListOf<Stmt>()
                while (pos < stmts.size) {
                    val stmt = stmts[pos]
                    val kind = stmtActionKind(stmt)

                    if (kind == "CONDITIONAL" || innerLimits.getValue(kind) < 1) break
                    innerLimits[kind] = innerLimits.getValue(kind) - 1

                    innerStmts.add(stmts.removeAt(pos))
                }

                if (innerStmts.isEmpty()) continue
                val ifStmt = Stmt(Span.none, StmtKind.Expr(
                    Expr(Span.none, ExprKind.If(
                        Expr(Span.none, ExprKind.Lit(Lit.Bool(true))),
                        wrap(innerStmts), null
                    ))
                ))

                stmts.add(pos, ifStmt)

                continue
            }
            // now we have to make a function continuation

            // for some reason we get a concurrent modification exception here if we removeAt() ???
            // not sure why, that might be a Kotlin compiler bug
            val newStmts = stmts.subList(pos, stmts.size)
            stmts = stmts.dropLast(stmts.size - pos).toMutableList()
            fn.block.stmts = stmts

            val ident = makeContinuation(ctx, item, newStmts)

            val call = Stmt(Span.none, StmtKind.Expr(
                Expr(Span.none, ExprKind.Call(ident, Args(Span.none, mutableListOf())))
            ))

            fn.block.stmts.add(call)
        }
        limits[kind] = limits.getValue(kind) - 1
        pos++
    }
}

private fun makeContinuation(ctx: LoweringCtx, item: Item, stmts: List<Stmt>): Ident = with(ctx) {
    val newFn = Fn(null, FnSig(Span.none, emptyList()), wrap(stmts))

    val name = item.ident.name
    val newName = if (
        name.split("_").lastOrNull()?.startsWith("c") == true // _c
        && name.split("_").last().drop(1).toIntOrNull() != null // _cInteger
    ) {
        val part = name.split("_").dropLast(1).joinToString("_")
        part + "_c" + (name.split("_").last().drop(1).toInt() + 1)
    } else name + "_c2"

    val newItem = Item(item.span, Ident.Player(newName), ItemKind.Fn(newFn))
    ast.items.add(newItem)

    extendLimits(this, newItem)

    return Ident.Player(newName)
}