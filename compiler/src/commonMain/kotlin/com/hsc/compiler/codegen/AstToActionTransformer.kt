package com.hsc.compiler.codegen

import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Level
import com.hsc.compiler.errors.CompileException
import com.hsc.compiler.ir.action.*
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

class AstToActionTransformer(private val ctx: LoweringCtx) {

    fun run(): List<Function> {
        return ctx.query<Item>().filter { it.kind is ItemKind.Fn }.map {
            val fn = (it.kind as ItemKind.Fn).fn
            if (fn.processors != null) {
                val err = ctx.dcx().err("cannot use processors in `strict` mode")
                err.span(it.kind.fn.processors!!.span)
                throw CompileException(err)
            }
            if (fn.sig.args.isNotEmpty()) {
                val err = ctx.dcx().err("cannot use function arguments in `strict` mode")
                err.span(it.kind.fn.sig.span)
                throw CompileException(err)
            }
            val name = it.ident.name
            val actions = block(fn.block)
            val function = Function(name, actions)
            function
        }
    }

    private fun block(block: Block): List<Action> {
        return block.stmts.map {
            when (val stmtKind = it.kind) {
                is StmtKind.Assign -> {
                    val assign = (it.kind as StmtKind.Assign)
                    val value = unwrapStatValue(assign.expr)
                    if (assign.ident.global) {
                        Action.ChangeGlobalStat(assign.ident.name, StatOp.Set, value)
                    } else {
                        Action.ChangePlayerStat(assign.ident.name, StatOp.Set, value)
                    }
                }
                is StmtKind.AssignOp -> {
                    val assign = (it.kind as StmtKind.AssignOp)
                    val op = when (assign.kind) {
                        BinOpKind.Add -> StatOp.Inc
                        BinOpKind.Sub -> StatOp.Dec
                        BinOpKind.Mul -> StatOp.Mul
                        BinOpKind.Div -> StatOp.Div
                        else -> {
                            val err = ctx.dcx().err("unsupported operator in `strict` mode")
                            err.span(assign.expr.span)
                            throw CompileException(err)
                        }
                    }
                    val value = unwrapStatValue(assign.expr)
                    if (assign.ident.global) {
                        Action.ChangeGlobalStat(assign.ident.name, op, value)
                    } else {
                        Action.ChangePlayerStat(assign.ident.name, op, value)
                    }
                }
                StmtKind.Break -> TODO()
                StmtKind.Continue -> TODO()
                is StmtKind.Expr -> {
                    val expr = (it.kind as StmtKind.Expr).expr
                    when (val kind = expr.kind) {
                        is ExprKind.Binary -> TODO()
                        is ExprKind.Block -> TODO()
                        is ExprKind.Call -> {
                            if (kind.args.args.isNotEmpty()) {
                                val err = ctx.dcx().err("unsupported operation in `strict` mode")
                                err.span(kind.args.span)
                                throw CompileException(err)
                            }
                            Action.ExecuteFunction(kind.ident.name, kind.ident.global)
                        }
                        is ExprKind.If -> {
                            val expr = kind.expr
                            if (expr.kind is ExprKind.Binary) {
                                val binary = (expr.kind as ExprKind.Binary)
                                val comparator = when (binary.kind) {
                                    BinOpKind.Eq -> Comparator.Eq
                                    BinOpKind.Lt -> Comparator.Lt
                                    BinOpKind.Le -> Comparator.Le
                                    BinOpKind.Ge -> Comparator.Ge
                                    BinOpKind.Gt -> Comparator.Gt
                                    else -> {
                                        val err = ctx.dcx().err("expected comparison")
                                        err.span(expr.span)
                                        throw CompileException(err)
                                    }
                                }

                            } else {
                                val err = ctx.dcx().err("complex conditional argument in `strict` mode")
                                err.span(expr.span)
                                throw CompileException(err)
                            }
                            TODO()
                        }
                        is ExprKind.Lit -> TODO()
                        is ExprKind.Match -> TODO()
                        is ExprKind.Paren -> TODO()
                        is ExprKind.Unary -> TODO()
                        is ExprKind.Var -> TODO()
                    }
                }
                is StmtKind.Action -> {
                    when (stmtKind.name) {
                        "send_message" -> {
                            Action.SendMessage(((stmtKind.exprs[0].kind as ExprKind.Lit).lit as Lit.Str).value)
                        }
                        "fail_parkour" -> {
                            Action.FailParkour(((stmtKind.exprs[0].kind as ExprKind.Lit).lit as Lit.Str).value)
                        }
                        else -> TODO()
                    }
                }
                is StmtKind.For -> TODO()
                is StmtKind.Ret -> TODO()
                is StmtKind.While -> TODO()
            }
        }
    }

    private fun expectExprVariable(expr: Expr): String {
        return when (expr.kind) {
            is ExprKind.Var -> {
                val ident = (expr.kind as ExprKind.Var).ident
                if (ident.global) {
                    "%stat.global/${ident.name}%"
                } else {
                    "%stat.player/${ident.name}%"
                }
            }
            else -> {
                val err = ctx.dcx().err("expected literal or variable")
                err.span(expr.span)
                err.note(Level.Error, "cannot use complex expressions in `strict` mode")
                throw CompileException(err)
            }
        }
    }

    private fun unwrapStatValue(expr: Expr): StatValue {
        return when (expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = (expr.kind as ExprKind.Lit).lit) {
                    is Lit.I64 -> {
                        StatValue.I64(lit.value)
                    }
                    else -> {
                        val err = ctx.dcx().err("cannot assign to non-integer variable.")
                        err.span(expr.span)
                        throw CompileException(err)
                    }
                }
            }
            is ExprKind.Var -> {
                val ident = (expr.kind as ExprKind.Var).ident
                StatValue.Str(if (ident.global) {
                    "%stat.global/${ident.name}%"
                } else {
                    "%stat.player/${ident.name}%"
                })
            }
            else -> {
                val err = ctx.dcx().err("expected variable")
                err.span(expr.span)
                if (ctx.sess.opts.mode == Mode.Strict) {
                    err.note(Level.Error, "cannot use complex expressions in `strict` mode")
                }
                throw CompileException(err)
            }
        }
    }

}