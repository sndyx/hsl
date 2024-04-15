package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.fold

/**
 * A pass that evaluates constant equations.
 * Must run before complex expressions are flattened.
 */
object ConstantFoldingPass : VolatilePass {

    override var changed: Boolean = false

    override fun run(ctx: LoweringCtx) {
        val visitor = ConstantFoldingClassVisitor(ctx)
        ctx.query<Stmt>().forEach { stmt ->
            visitor.visitStmt(stmt)
        }
    }

}

private class ConstantFoldingClassVisitor(val ctx: LoweringCtx) : AstVisitor {
    override fun visitExpr(expr: Expr) {
        fold(ctx.sess, expr)
    }
}