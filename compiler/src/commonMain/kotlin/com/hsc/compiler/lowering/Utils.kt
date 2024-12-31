package com.hsc.compiler.lowering

import com.hsc.compiler.ir.action.StatValue
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span

fun LoweringCtx.getItems(): List<Item> {
    return buildList {
        object : AstVisitor {
            override fun visitItem(item: Item) {
                add(item)
                super.visitItem(item)
            }
        }.visitAst(ast)
    }
}

fun LoweringCtx.getFunctions(): List<Fn> {
    return getItems()
        .filter { it.kind is ItemKind.Fn }
        .map { (it.kind as ItemKind.Fn).fn }
}

fun LoweringCtx.getFunctionItems(): List<Pair<Item, Fn>> {
    return getItems()
        .filter { it.kind is ItemKind.Fn }
        .map { it to (it.kind as ItemKind.Fn).fn }
}

fun LoweringCtx.getFunction(ident: Ident): Fn? {
    return (getItems()
        .find { it.ident == ident }
        ?.kind as? ItemKind.Fn)?.fn
}

fun LoweringCtx.getBlocks(): List<Block> = buildList {
    object : AstVisitor {
        override fun visitBlock(block: Block) {
            add(block)
            super.visitBlock(block)
        }
    }.visitAst(ast)
}

fun LoweringCtx.getStmts(): List<Stmt> = buildList {
    object : AstVisitor {
        override fun visitStmt(stmt: Stmt) {
            add(stmt)
            super.visitStmt(stmt)
        }
    }.visitAst(ast)
}

fun LoweringCtx.getExprs(): List<Expr> = buildList {
    object : AstVisitor {
        override fun visitExpr(expr: Expr) {
            add(expr)
            super.visitExpr(expr)
        }
    }.visitAst(ast)
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

fun walk(entity: Visitable, maxDepth: Int, consumer: (Expr) -> Unit) {
    entity.visit(object : AstVisitor {
        var depth = 0
        override fun visitBlock(block: Block) {
            depth++
            if (depth < maxDepth) super.visitBlock(block)
            depth--
        }
        override fun visitExpr(expr: Expr) {
            consumer(expr)
            super.visitExpr(expr)
        }
    })
}

fun walkAware(entity: Visitable, consumer: BlockAwareVisitor.(Expr) -> Unit) {
    entity.visit(object : BlockAwareVisitor() {
        override fun visitExpr(expr: Expr) {
            consumer(expr)
            super.visitExpr(expr)
        }
    })
}

fun LoweringCtx.statValueOf(expr: Expr): StatValue {
    return when (val kind = expr.kind) {
        is ExprKind.Lit -> {
            when (val lit = kind.lit) {
                is Lit.I64 -> StatValue.I64(lit.value)
                is Lit.F64 -> StatValue.I64(lit.value.toLong())
                is Lit.Str -> StatValue.Str(lit.value)
                else -> {
                    throw dcx().err("expected integer, found ${lit.str()}")
                }
            }
        }
        is ExprKind.Var -> {
            if (kind.ident.isGlobal) StatValue.Str("%stat.global/${kind.ident.name}%")
            else StatValue.Str("%stat.player/${kind.ident.name}%")
        }
        else -> {
            throw dcx().err("expected integer, found ${kind.str()}")
        }
    }
}