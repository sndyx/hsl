package com.hsc.compiler.lowering.newpasses

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.Condition
import com.hsc.compiler.ir.ast.Expr
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import com.hsc.compiler.lowering.*

/**
 * A pass that inlines function parameters into the owning block.
 *
 * eg:
 * ```
 * add(a, b)
 * ```
 * becomes:
 * ```
 * _param1 = a
 * _param2 = b
 * add()
 * ```
 */
fun inlineFunctionParameters(ctx: LoweringCtx) = with(ctx) {
    getFunctions().forEach { fn ->
        val alreadyInlined = mutableListOf<Expr>()

        var changed = true
        while (changed) {
            changed = false
            walkAware(fn) { expr ->
                val call = expr.call() ?: return@walkAware

                // check for a function that has already been inlined
                if (alreadyInlined.contains(expr)) return@walkAware
                alreadyInlined.add(expr)

                // make sure we don't affect builtin functions
                if (call.ident.name.let {
                        it in Action.builtins || it in Condition.builtins
                    }) return@walkAware

                val callee = getFunction(call.ident)

                if (callee == null) {
                    // function not found
                    val diag = if (call.args.isEmpty()) {
                        dcx().warn("unresolved function")
                    } else {
                        dcx().err("cannot call unresolved function with parameters")
                    }
                    diag.span(expr.span)
                    similar(call.ident.name, Action.builtins.toList()).forEach {
                        diag.note(Level.Hint, "did you mean `$it`?")
                    }
                    diag.emit()

                    return@walkAware
                }

                if (callee.sig.args.size != call.args.args.size) {
                    val s1 = if (callee.sig.args.size == 1) "" else "s"
                    val s2 = if (call.args.args.size == 1) "" else "s"
                    val was = if (call.args.args.size == 1) "was" else "were"
                    val err = ctx.dcx().err("this function takes ${callee.sig.args.size} parameter$s1 but ${call.args.args.size} parameter$s2 $was supplied")
                    err.span(call.args.span)
                    throw err // probably not recoverable
                }

                if (call.args.isEmpty()) return@walkAware
                changed = true

                callee.sig.args.forEachIndexed { index, arg ->
                    if (isTemp(arg) && isTempInUse(arg, fn, expr) && call.args.args[index].variable()?.ident != arg) {
                        // setting this parameter would disturb the code around it
                        val swapIdent = firstAvailableTemp(fn, expr)

                        currentBlock.stmts.add(currentPosition, Stmt(expr.span,
                            StmtKind.Assign(swapIdent, Expr(expr.span, ExprKind.Var(arg)))
                        ))
                        offset(1)
                        currentBlock.stmts.add(currentPosition + 1, Stmt(expr.span,
                            StmtKind.Assign(arg, Expr(expr.span, ExprKind.Var(swapIdent)))
                        ))
                    }

                    currentBlock.stmts.add(currentPosition, Stmt(expr.span,
                        StmtKind.Assign(arg, call.args.args[index])
                    )
                    )
                    offset(1)
                }

                call.args.args.clear()
            }
        }
    }
}