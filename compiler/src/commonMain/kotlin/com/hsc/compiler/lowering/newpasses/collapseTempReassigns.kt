package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.getBlocks
import com.hsc.compiler.lowering.isTemp

/**
 * A pass that will collapse temp vars that are immediately reassigned.
 *
 * Should *always* run after `FlattenComplexExpressionsPass`!
 *
 * eg:
 * ```
 * _temp = 5
 * x = _temp
 * ```
 * becomes:
 * ```
 * x = 5
 * ```
 */
fun collapseTempReassigns(ctx: LoweringCtx) {
    ctx.getBlocks().forEach { block ->

        block.visit(object : BlockAwareVisitor() {
            override fun visitStmt(stmt: Stmt) {
                super.visitStmt(stmt)

                val assign = stmt.assign() ?: return

                if (!ctx.isTemp(assign.ident)) return // only flatten temp variables
                if (currentBlock.stmts.size == currentPosition + 1) return // the next stmt does not exist

                val nextAssign = currentBlock.stmts[currentPosition + 1].assign() ?: return
                val nextIdent = nextAssign.expr.variable()?.ident ?: return // this is not a variable

                if (assign.ident == nextIdent) {
                    currentBlock.stmts.removeAt(currentPosition)
                    nextAssign.expr = assign.expr // flatten assignments
                }
            }
        })

    }
}