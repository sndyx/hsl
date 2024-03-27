package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.*

/**
 * A pass that will flatten temp vars that are immediately reassigned.
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
object FlattenTempReassignPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            FlattenTempReassignVisitor.visitItem(it)
        }
    }

}

private object FlattenTempReassignVisitor : BlockAwareVisitor() {

    var prevTempAssign: Expr? = null
    var prevIdent: Ident? = null

    override fun visitBlock(block: Block) {
        prevTempAssign = null
        prevIdent = null
        super.visitBlock(block)
    }

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.Assign -> {
                if (prevTempAssign != null) {
                    when (val exprKind = kind.expr.kind) {
                        is ExprKind.Var -> {
                            if (exprKind.ident == prevIdent) {
                                kind.expr = prevTempAssign!!
                                currentBlock.stmts.removeAt(currentPosition - 1)
                                added(-1)
                            }
                        }
                        else -> { }
                    }
                }

                if (kind.ident.name.startsWith("_")) {
                    prevTempAssign = kind.expr
                    prevIdent = kind.ident
                }
                else {
                    prevTempAssign = null
                    prevIdent = null
                }
            }
            else -> {
                if (kind is StmtKind.Expr && kind.expr.kind is ExprKind.Var) {
                    // Remove useless vars
                    currentBlock.stmts.removeAt(currentPosition)
                    added(-1)
                }
                prevTempAssign = null
            }
        }
        super.visitStmt(stmt)
    }

}