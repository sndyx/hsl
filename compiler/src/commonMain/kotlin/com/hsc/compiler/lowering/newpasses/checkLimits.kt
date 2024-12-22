package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Level
import com.hsc.compiler.lowering.*
import com.hsc.compiler.span.Span

/**
 * Checks to see if any action limits have been surpassed and raises an error if so.
 */
fun checkLimits(ctx: LoweringCtx) = with(ctx) {
    val mainBlock = getFunctionItems()
        .find { (item, _) -> item.ident.name == "main" }
        ?.second?.block

    getBlocks().forEach blockLoop@{ block ->
        if (block == mainBlock) return@blockLoop

        val limits = limitsMap.toMutableMap()

        block.stmts.forEach { stmt ->
            val kind = stmtActionKind(stmt)

            limits[kind] = (limits[kind] ?: Int.MAX_VALUE) - 1
            if (limits[kind] == -1) {
                val err = ctx.dcx().err("action limit surpassed: `${kind.lowercase()}`")

                if (stmt.span == Span.none) err.spanLabel(block.span, "in this scope")
                else err.spanLabel(stmt.span, "with this statement")

                if (ctx.sess.opts.mode != Mode.Strict) {
                    err.note(Level.Error, "could not optimize out actions")
                }
                err.emit()
                return@blockLoop
            }
        }
    }
}