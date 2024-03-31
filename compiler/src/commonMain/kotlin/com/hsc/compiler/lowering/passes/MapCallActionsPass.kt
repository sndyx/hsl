package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object MapCallActionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Item>().filter { it.kind is ItemKind.Fn }
            .forEach { MapCallActionsVisitor(ctx).visitItem(it) }
    }

}

private class MapCallActionsVisitor(val ctx: LoweringCtx) : AstVisitor {

    override fun visitStmt(stmt: Stmt) {
        val stmtKind = stmt.kind
        if (stmtKind !is StmtKind.Expr) { super.visitStmt(stmt); return }
        when (val kind = stmtKind.expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.global) return
                val action = when (kind.ident.name) {
                    "send_message" -> {
                        argParse(kind.args, Type.String)
                        StmtKind.Action("send_message", kind.args.args)
                    }
                    "fail_parkour" -> {
                        argParse(kind.args, Type.String)
                        StmtKind.Action("fail_parkour", kind.args.args)
                    }
                    else -> null
                }
                if (action != null) stmt.kind = action
            }
            else -> super.visitStmt(stmt)
        }
    }

    private fun argParse(args: Args, vararg types: Type) {
        if (args.args.size != types.size) {
            val s1 = if (types.size == 1) "" else "s"
            val s2 = if (args.args.size == 1) "" else "s"
            val was = if (args.args.size == 1) "was" else "were"
            val err = ctx.dcx().err("this function takes ${types.size} parameter$s1 but ${args.args.size} parameter$s2 $was supplied")
            err.span(args.span)
            err.emit()
            return
        }
        types.zip(args.args).forEach { (type, expr) ->
            val kind = expr.kind
            val matches = when (type) {
                // Maybe clean this up later...
                Type.String -> {
                    kind is ExprKind.Lit && kind.lit is Lit.Str
                }
                Type.Int -> {
                    kind is ExprKind.Lit && kind.lit is Lit.I64
                }
                Type.IntVar -> {
                    (kind is ExprKind.Lit && kind.lit is Lit.I64) || kind is ExprKind.Var
                }
                Type.Item -> {
                    kind is ExprKind.Lit && kind.lit is Lit.Item
                }
                Type.Bool -> {
                    kind is ExprKind.Lit && kind.lit is Lit.Bool
                }
            }

            if (!matches) {
                val err = ctx.dcx().err("expected ${type.str}, found ${expr.kind.str()}")
                err.span(expr.span)
                err.emit()
            }
        }
    }

}

private enum class Type(val str: kotlin.String) {
    String("string"),
    Int("integer"),
    IntVar("integer"),
    Item("item"),
    Bool("bool")
}