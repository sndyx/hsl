package com.hsc.compiler.lowering

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

fun LoweringCtx.getFunctions(): List<Fn> {
    return query<Item>()
        .filter { it.kind is ItemKind.Fn }
        .map { (it.kind as ItemKind.Fn).fn }
}

fun LoweringCtx.getFunction(ident: Ident): Fn? {
    return (query<Item>()
        .find { it.ident == ident }
        ?.kind as? ItemKind.Fn)?.fn
}

fun wrap(stmts: List<Stmt>): Block {
    val span = Span(stmts.first().span.lo, stmts.last().span.hi, stmts.first().span.fid)
    return Block(span, stmts.toMutableList())
}

fun coalesce(stmts: List<Stmt>): Stmt {
    val block = wrap(stmts)
    val expr = Expr(block.span, ExprKind.Block(block))
    return Stmt(block.span, StmtKind.Expr(expr))
}

fun walk(entity: Visitable, consumer: (Expr) -> Unit) {
    entity.visit(object : AstVisitor {
        override fun visitExpr(expr: Expr) {
            consumer(expr)
            super.visitExpr(expr)
        }
    })
}

fun walkAware(fn: Fn, consumer: BlockAwareVisitor.(Expr) -> Unit) {
    return object : BlockAwareVisitor() {
        override fun visitExpr(expr: Expr) {
            consumer(expr)
            super.visitExpr(expr)
        }
    }.visitFn(fn)
}