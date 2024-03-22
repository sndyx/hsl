package com.hsc.compiler.pretty

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.ir.ast.*

fun prettyPrintAst(items: List<Item>) {
    items.forEach {
        PrettyPrintVisitor.visitItem(it)
    }
}

object PrettyPrintVisitor : AstVisitor {

    private val t = Terminal()
    private var indent = 0

    val i: String get() = " ".repeat(indent * 2)

    override fun visitItem(item: Item) {
        when (val kind = item.kind) {
            is ItemKind.Fn -> {
                kind.fn.processors?.list?.forEach {
                    t.println(brightYellow("#$it"))
                }
                t.println("${blue("fn")} ${bold(item.ident.name)}${white("(")}${kind.fn.sig.args.joinToString()}${white(")")} ${white("{")}")
            }
        }
        super.visitItem(item)
        t.println(white("}"))
    }

    override fun visitBlock(block: Block) {
        indent++
        super.visitBlock(block)
        indent--
    }

    override fun visitStmt(stmt: Stmt) {
        when (val kind = stmt.kind) {
            is StmtKind.Ret -> {
                t.print("${i}${blue("return")}")
                kind.expr?.let {
                    t.print(" ")
                    visitExpr(it)
                }
                t.println()
            }
            is StmtKind.Expr -> {
                t.print(i)
                visitExpr(kind.expr)
                t.println()
            }
            is StmtKind.Assign -> {
                t.print("${i}${kind.ident} ${white("=")} ")
                visitExpr(kind.expr)
                t.println()
            }
            is StmtKind.AssignOp -> {
                t.print("${i}${kind.ident} ${white("${kind.kind.str}=")} ")
                visitExpr(kind.expr)
                t.println()
            }
            is StmtKind.For -> {
                t.print("${i}${blue("for")} ${white("(")}${kind.label} ${blue("in")} ")
                visitExpr(kind.range.lo)
                t.print(white(".."))
                visitExpr(kind.range.hi)
                t.println(white(") {"))
                visitBlock(kind.block)
                t.println(white("}"))
            }
            is StmtKind.Break -> t.println(blue("${i}break"))
            is StmtKind.Continue -> t.println(blue("${i}continue"))
            is StmtKind.Action -> {
                t.print("${i}${(bold + italic)(kind.name)}${white("(")}")
                kind.exprs.forEachIndexed { idx, it ->
                    visitExpr(it)
                    if (idx != kind.exprs.size - 1) t.print(white(", "))
                }
                t.println(white(")"))
            }
            is StmtKind.While -> {
                t.print("${i}${blue("while")} ${white("(")}")
                visitExpr(kind.cond)
                t.println(white(")"))
                visitBlock(kind.block)
            }
        }
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                t.print(gray("["))
                visitExpr(kind.a)
                t.print(" ${white(kind.kind.str)} ")
                visitExpr(kind.b)
                t.print(gray("]"))
            }
            is ExprKind.Block -> {
                if (kind.block.stmts.size == 1 && kind.block.stmts.single().kind is StmtKind.Expr) {
                    t.print(white("{ "))
                    visitExpr((kind.block.stmts.single().kind as StmtKind.Expr).expr)
                    t.print(white(" }"))
                } else {
                    t.println(white("{"))
                    visitBlock(kind.block)
                    t.print(white("${i}}"))
                }
            }
            is ExprKind.Call -> {
                t.print("${kind.ident}${white("(")}")
                kind.args.args.forEachIndexed { idx, it ->
                    visitExpr(it)
                    if (idx != kind.args.args.size - 1) t.print(white(", "))
                }
                t.print(white(")"))
            }
            is ExprKind.If -> {
                t.print("${blue("if")} ${white("(")}")
                visitExpr(kind.cond)
                t.println(white(") {"))
                visitBlock(kind.block)
                t.print(white("${i}}"))
                kind.other?.let {
                    t.print(" ")
                    t.println("${blue("else")} ${white("{")}")
                    visitBlock(it)
                    t.print(white("${i}}"))
                }
            }
            is ExprKind.Lit -> {
                val value = when (val lit = kind.lit) {
                    Lit.Null -> blue("null")
                    is Lit.Bool -> blue(lit.value.toString())
                    is Lit.I64 -> white(lit.value.toString())
                    is Lit.Item -> lit.value.toString()
                    is Lit.Str -> white("\"${lit.value}\"")
                }
                t.print(value)
            }
            is ExprKind.Match -> {
                t.print("${blue("match")} ${white("(")}")
                visitExpr(kind.expr)
                t.println(white(") {"))
                indent++
                kind.arms.forEach {
                    t.print(i)
                    it.query.forEachIndexed { idx, expr ->
                        visitExpr(expr)
                        if (idx != it.query.size - 1) t.print(", ")
                    }
                    t.println(white(" -> "))
                    visitExpr(it.value)
                }
                indent--
                t.println(white("${i}}"))
            }
            is ExprKind.Paren -> {
                t.print(white("("))
                visitExpr(kind.expr)
                t.print(white(")"))
            }
            is ExprKind.Unary -> {
                when (kind.kind) {
                    UnaryOpKind.Neg -> {
                        t.println(white("-"))
                    }
                    UnaryOpKind.Not -> {
                        t.println(white("!"))
                    }
                }
                visitExpr(kind.expr)
            }
            is ExprKind.Var -> {
                t.print(kind.ident)
            }
        }
    }

    val BinOpKind.str: String get() = when (this) {
        BinOpKind.Add -> "+"
        BinOpKind.Sub -> "-"
        BinOpKind.Mul -> "*"
        BinOpKind.Div -> "/"
        BinOpKind.Pow -> "^"
        BinOpKind.Rem -> "%"
        BinOpKind.Eq -> "=="
        BinOpKind.Lt -> "<"
        BinOpKind.Le -> "<="
        BinOpKind.Gt -> ">"
        BinOpKind.Ge -> ">="
        BinOpKind.Ne -> "!="
        BinOpKind.In -> "in"
        BinOpKind.And -> "&&"
        BinOpKind.Or -> "||"
    }

}