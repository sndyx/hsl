package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.*
import kotlin.math.pow

/**
 * A pass that evaluates constant equations.
 * Must run before complex expressions are flattened.
 */
object EvaluateConstantEquationsPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            val visitor = EvaluateConstantEquationsVisitor()
            do {
                visitor.changes = 0
                visitor.visitItem(it)
            } while (visitor.changes != 0)
        }
    }

}

private class EvaluateConstantEquationsVisitor : BlockAwareVisitor() {

    var changes = 0

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Binary -> {
                if (kind.a.kind is ExprKind.Lit && kind.b.kind is ExprKind.Lit) {
                    // If these aren't i32s by now, something has gone seriously
                    // wrong with the previous passes...
                    val a = ((kind.a.kind as ExprKind.Lit).lit as Lit.I64).value
                    val b = ((kind.b.kind as ExprKind.Lit).lit as Lit.I64).value
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
                        BinOpKind.In -> { error("unreachable") }
                    }
                    expr.kind = ExprKind.Lit(
                        Lit.I64(result)
                    )
                    changes++
                }
                else {
                    visitExpr(kind.a)
                    visitExpr(kind.b)
                }
            }
            else -> super.visitExpr(expr)
        }
    }

}
