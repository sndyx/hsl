package com.hsc.compiler.lowering

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import com.hsc.compiler.span.Span

fun wrap(stmts: List<Stmt>): Block {
    val span = Span(stmts.first().span.lo, stmts.last().span.hi, stmts.first().span.fid)
    return Block(span, stmts.toMutableList())
}

fun coalesce(stmts: List<Stmt>): Stmt {
    val block = wrap(stmts)
    val expr = Expr(block.span, ExprKind.Block(block))
    return Stmt(block.span, StmtKind.Expr(expr))
}

fun traverse(expr: Expr, consumer: (Expr) -> Unit) {
    object : AstVisitor {
        override fun visitExpr(expr: Expr) {
            consumer(expr)
            super.visitExpr(expr)
        }
    }.visitExpr(expr)
}