package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Condition
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.span.Span

/**
 * A pass that inlines function parameters.
 *
 * eg:
 * ```
 * fn do_something(_param) { ... }
 *
 * do_something(5)
 * ```
 * becomes:
 * ```
 * _param = 5
 * do_something()
 * ```
 */
object InlineFunctionParametersPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            val visitor = InlineFunctionParametersVisitor(ctx)
            do {
                visitor.changes = 0
                visitor.visitItem(it)
            } while (visitor.changes != 0)
        }
        functions.forEach {
            val fn = (it.kind as ItemKind.Fn).fn
            fn.sig.args = emptyList() // Args are no more!
        }
    }

}

private class InlineFunctionParametersVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var changes = 0
    val alreadyInlined = mutableListOf<ExprKind>()

    override fun visitExpr(expr: Expr) {
        if (alreadyInlined.contains(expr.kind)) return
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.name in Action.builtins || kind.ident.name in Condition.builtins) {
                    super.visitExpr(expr); return
                }

                var emitted = false
                val item = ctx.query<Item>()
                    .filter { it.kind is ItemKind.Fn }
                    .find { it.ident.name == kind.ident.name }

                if (item == null && kind.args.args.isNotEmpty()) {
                    val err = ctx.dcx().err("cannot call unresolved function with parameters")
                    err.span(expr.span)
                    err.emit()
                    emitted = true
                }

                if (item != null) {
                    val fn = (item.kind as ItemKind.Fn).fn
                    if (fn.sig.args.size != kind.args.args.size) {
                        val s1 = if (fn.sig.args.size == 1) "" else "s"
                        val s2 = if (kind.args.args.size == 1) "" else "s"
                        val was = if (kind.args.args.size == 1) "was" else "were"
                        val err = ctx.dcx().err("this function takes ${fn.sig.args.size} parameter$s1 but ${kind.args.args.size} parameter$s2 $was supplied")
                        err.span(kind.args.span)
                        throw err // This will probably mess up future passes if not thrown
                    }

                    val stmts = fn.sig.args.mapIndexed { idx, ident ->
                        Stmt(Span.none, StmtKind.Assign(ident, kind.args.args[idx]))
                    }
                    currentBlock.stmts.addAll(currentPosition, stmts)
                    added(stmts.size)
                    if (stmts.isNotEmpty()) changes++
                    kind.args.args.clear()
                    alreadyInlined.add(kind)
                    return
                }
                // else do nothing, function is assumed external and out of our control.

                // do NOT let him cook with this one... this span creation is 1: incorrect, 2: terrible...
                if (!emitted) { // Only run if we haven't already emitted an error for this
                    val span = Span(kind.args.span.lo - kind.ident.name.length, kind.args.span.hi, kind.args.span.fid)
                    val warn = ctx.dcx().warn("unresolved function")
                    warn.span(span)
                    warn.emit()
                }
            }
            else -> { super.visitExpr(expr) }
        }
    }

}


