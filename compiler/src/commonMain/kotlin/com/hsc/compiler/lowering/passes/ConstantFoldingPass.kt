package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import kotlin.math.pow

/**
 * A pass that evaluates constant equations.
 * Must run before complex expressions are flattened.
 */
object ConstantFoldingPass : VolatilePass {

    override var changed: Boolean = false

    override fun run(ctx: LoweringCtx) {
        val functions = ctx.query<Item>().filter { it.kind is ItemKind.Fn }
        val visitor = EvaluateConstantEquationsVisitor(ctx)
        functions.forEach {
            do {
                visitor.changes = 0
                visitor.visitItem(it)
                if (visitor.changes > 0) changed = true
            } while (visitor.changes != 0)
        }
    }

}

class EvaluateConstantEquationsVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    var changes = 0

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                if (kind.a.kind is ExprKind.Lit && kind.b.kind is ExprKind.Lit) {
                    val exprA = kind.a.kind as ExprKind.Lit
                    val exprB = kind.b.kind as ExprKind.Lit

                    operation(exprA.lit, exprB.lit) {
                        context<Lit.I64, Lit.I64> { la, lb ->
                            val a = la.value
                            val b = lb.value
                            val result = when (kind.kind) {
                                BinOpKind.Add -> a + b
                                BinOpKind.Sub -> a - b
                                BinOpKind.Mul -> a * b
                                BinOpKind.Div -> a / b
                                BinOpKind.Rem -> a % b
                                BinOpKind.Pow -> a.toDouble().pow(b.toDouble()).toLong()
                                BinOpKind.And -> if (a == 1L && b == 1L) 1L else 0L
                                BinOpKind.Or -> if (a == 1L || b == 1L) 1L else 0L
                                BinOpKind.Eq -> if (a == b) 1L else 0L
                                BinOpKind.Ne -> if (a != b) 1L else 0L
                                BinOpKind.Lt -> if (a < b) 1L else 0L
                                BinOpKind.Le -> if (a <= b) 1L else 0L
                                BinOpKind.Ge -> if (a >= b) 1L else 0L
                                BinOpKind.Gt -> if (a > b) 1L else 0L
                                else -> null
                            }
                            result?.let {
                                expr.kind = ExprKind.Lit(Lit.I64(it))
                                changes++
                            }
                            return@operation
                        }
                        context<Lit.Str, Lit.Str> { la, lb ->
                            val a = la.value
                            val b = lb.value
                            val result = when (kind.kind) {
                                BinOpKind.Add -> a + b
                                else -> {
                                    ctx.dcx().err("invalid operand string ${kind.kind} string", expr.span).emit()
                                    "error"
                                }
                            }
                            expr.kind = ExprKind.Lit(Lit.Str(result))
                            changes++
                            return@operation
                        }
                        fun bool(a: Boolean, b: Boolean) {
                            val result = when (kind.kind) {
                                BinOpKind.Eq -> a == b
                                BinOpKind.Ne -> a != b
                                BinOpKind.And -> a && b
                                BinOpKind.Or -> a || b
                                else -> {
                                    ctx.dcx().err("invalid operand boolean ${kind.kind} integer", expr.span).emit()
                                    false
                                }
                            }
                            expr.kind = ExprKind.Lit(Lit.Bool(result))
                            changes++
                        }
                        context<Lit.Bool, Lit.Bool> { la, lb ->
                            bool(la.value, lb.value)
                            return@operation
                        }
                        context<Lit.Bool, Lit.I64> { la, lb ->
                            if (lb.value != 0L && lb.value != 1L) {
                                throw ctx.dcx().err("invalid operand types integer and boolean", expr.span)
                                // Don't emit this one, will be emitted twice otherwise.
                            }
                            bool(la.value, lb.value == 1L)
                            return@operation
                        }
                    }
                }
                else {
                    visitExpr(kind.a)
                    visitExpr(kind.b)
                }
            }
            is ExprKind.If -> {
                when (val litKind = kind.expr.kind) {
                    is ExprKind.Lit -> {
                        when (val lit = litKind.lit) {
                            is Lit.I64 -> {
                                if (lit.value == 1L) expr.kind = ExprKind.Block(kind.block)
                                else if (lit.value == 0L) expr.kind = ExprKind.Block(kind.other!!) // should be safe
                            }
                            is Lit.Bool -> {
                                if (lit.value) expr.kind = ExprKind.Block(kind.block)
                                else expr.kind = ExprKind.Block(kind.other!!)
                            }
                            else -> {}
                        }
                    } else -> super.visitExpr(expr)
                }
            }
            else -> super.visitExpr(expr)
        }
    }

}

private fun operation(a: Lit, b: Lit, block: OperationScope.() -> Unit) {
    OperationScope(a, b).block()
}

private class OperationScope(val a: Lit, val b: Lit) {

    inline fun <reified A : Lit, reified B : Lit> context(block: (A, B) -> Unit) {
        if (a is A && b is B) block(a, b)
        else if (a is B && b is A) block(b, a)
    }

}