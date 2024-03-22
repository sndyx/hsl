package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.*

object InlineFunctionPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            val visitor = InlineFunctionVisitor(sess.map)
            do {
                visitor.changes = 0
                visitor.visitItem(it)
            } while (visitor.changes != 0)
        }
        functions.forEach {
            if ((it.kind as ItemKind.Fn).fn.processors?.list?.contains("inline") == true) {
                sess.map.cache.remove(it.id.id)
            }
        }
    }

}

private class InlineFunctionVisitor(val map: AstMap) : BlockAwareVisitor() {

    var changes = 0
    var cident: Ident? = null

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.Assign -> {
                cident = kind.ident
            }
            is StmtKind.AssignOp -> {
                // cident = Ident(false, "_temp0")
                // this might conflict with the complex expressions pass...
            }
            else -> {
                cident = null
            }
        }
        super.visitStmt(stmt)
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                val calleeName = kind.ident.name
                val callee = map.query<Item>().find { it.kind is ItemKind.Fn && it.ident.name == calleeName }
                if (callee != null) {
                    val fn = (callee.kind as ItemKind.Fn).fn
                    if (fn.processors?.list?.contains("inline") == true) {

                        // deepCopy() to remove ref! Will otherwise link multiple parts badly
                        val inlineBlock = fn.block.deepCopy()

                        // backwards-inline args
                        fn.sig.args.forEachIndexed { idx, ident ->
                            val visitor = InlinedFunctionTransformerVisitor(ident, kind.args.args[idx])
                            visitor.visitBlock(inlineBlock)
                        }
                        kind.args.args.clear()

                        expr.kind = ExprKind.Var(Ident(false, "_return"))
                        // currentBlock.stmts.removeAt(currentPosition)

                        currentBlock.stmts.addAll(currentPosition, inlineBlock.stmts)
                        added(inlineBlock.stmts.size)
                        changes++
                    }
                }
            }
            else -> super.visitExpr(expr)
        }
    }


}

private class InlinedFunctionTransformerVisitor(val old: Ident, val new: Expr) : AstVisitor {

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Var -> {
                if (kind.ident == old) {
                    expr.kind = new.kind
                    expr.span = new.span
                }
            } else -> super.visitExpr(expr)
        }
    }

}

