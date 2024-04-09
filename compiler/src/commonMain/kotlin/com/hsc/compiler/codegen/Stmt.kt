package com.hsc.compiler.codegen

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.StatOp
import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind

fun ActionTransformer.transformStmt(stmt: Stmt): Action {
    return when (val kind = stmt.kind) {
        is StmtKind.Action -> {
            unwrapAction(stmt)
        }
        is StmtKind.Assign -> {
            val value = unwrapStatValue(kind.expr)
            makeAssign(kind.ident, null, value)
        }
        is StmtKind.AssignOp -> {
            val op = kind.kind
            val value = unwrapStatValue(kind.expr)
            makeAssign(kind.ident, op, value)
        }
        is StmtKind.Expr -> {
            transformExpr(kind.expr)
        }
        is StmtKind.For, is StmtKind.Ret, is StmtKind.While,
        StmtKind.Break, StmtKind.Continue -> {
            strict(stmt.span) {
                sess.dcx().err("unsupported operation in `strict` mode", stmt.span)
            }
        }
    }
}

private fun ActionTransformer.makeAssign(ident: Ident, op: BinOpKind?, value: StatValue): Action {
    val statOp = op?.let { unwrapStatOp(it) } ?: StatOp.Set
    return if (ident.global) {
        Action.ChangeGlobalStat(ident.name, statOp, value)
    } else {
        Action.ChangePlayerStat(ident.name, statOp, value)
    }
}