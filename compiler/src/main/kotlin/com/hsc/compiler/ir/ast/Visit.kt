package com.hsc.compiler.ir.ast

interface AstVisitor {
    fun visitItem(item: Item) {
        walkItem(this, item)
    }
    fun visitFn(fn: Fn) {
        walkFn(this, fn)
    }
    fun visitBlock(block: Block) {
        walkBlock(this, block)
    }
    fun visitExpr(expr: Expr) {
        walkExpr(this, expr)
    }
    fun visitStmt(stmt: Stmt) {
        walkStmt(this, stmt)
    }
    fun visitArm(arm: Arm) {
        walkArm(this, arm)
    }
    fun visitRange(range: Range) {
        walkRange(this, range)
    }

    fun visitCall(expr: ExprKind.Call) { /* Ignore */ }
    fun visitProcessors(processors: Processors) { /* Ignore */ }
    fun visitFnSig(fnSig: FnSig) { /* Ignore */ }
    fun visitIdent(ident: Ident) { /* Ignore */ }
    fun visitLit(lit: Lit) { /* Ignore */  }
}

fun walkItem(v: AstVisitor, item: Item) {
    v.visitIdent(item.ident)
    when (item.kind) {
        is ItemKind.Fn -> {
            v.visitFn(item.kind.fn)
        }
    }
}

fun walkFn(v: AstVisitor, fn: Fn) {
    fn.processors?.let(v::visitProcessors)
    v.visitFnSig(fn.sig)
    v.visitBlock(fn.block)
}

fun walkBlock(v: AstVisitor, block: Block) {
    // To avoid concurrent modification, clone list
    // This should be relatively harmless?
    block.stmts.toList().forEach(v::visitStmt)
}

fun walkExpr(v: AstVisitor, expr: Expr) {
    when (val kind = expr.kind) {
        is ExprKind.Binary -> {
            v.visitExpr(kind.a)
            v.visitExpr(kind.b)
        }
        is ExprKind.Block -> v.visitBlock(kind.block)
        is ExprKind.Call -> {
            v.visitCall(kind)
            kind.args.args.forEach(v::visitExpr)
        }
        is ExprKind.If -> {
            v.visitExpr(kind.cond)
            v.visitBlock(kind.block)
            kind.other?.let(v::visitBlock)
        }
        is ExprKind.Lit -> v.visitLit(kind.lit)
        is ExprKind.Match -> {
            v.visitExpr(kind.expr)
            kind.arms.forEach(v::visitArm)
        }
        is ExprKind.Paren -> v.visitExpr(kind.expr)
        is ExprKind.Unary -> v.visitExpr(kind.expr)
        is ExprKind.Var -> v.visitIdent(kind.ident)
    }
}

fun walkStmt(v: AstVisitor, stmt: Stmt) {
    when (val kind = stmt.kind) {
        is StmtKind.Assign -> {
            v.visitIdent(kind.ident)
            v.visitExpr(kind.expr)
        }
        is StmtKind.AssignOp -> {
            v.visitIdent(kind.ident)
            v.visitExpr(kind.expr)
        }
        is StmtKind.Expr -> v.visitExpr(kind.expr)
        is StmtKind.For -> {
            v.visitIdent(kind.label)
            v.visitRange(kind.range)
            v.visitBlock(kind.block)
        }
        is StmtKind.Ret -> kind.expr?.let(v::visitExpr)
        is StmtKind.While -> {
            v.visitExpr(kind.cond)
            v.visitBlock(kind.block)
        }
        is StmtKind.Action -> {
            kind.exprs.forEach(v::visitExpr)
        }
        StmtKind.Break, StmtKind.Continue -> { /* Ignore */ }
    }
}

fun walkArm(v: AstVisitor, arm: Arm) {
    arm.query.forEach(v::visitExpr)
    v.visitExpr(arm.value)
}

fun walkRange(v: AstVisitor, range: Range) {
    v.visitExpr(range.lo)
    v.visitExpr(range.hi)
}