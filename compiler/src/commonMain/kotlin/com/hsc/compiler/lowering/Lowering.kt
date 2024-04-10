package com.hsc.compiler.lowering

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.passes.*
import com.hsc.compiler.pretty.prettyPrintAst
import kotlin.reflect.KClass

private val passes: Map<Mode, List<AstPass>> = mapOf(
    Mode.Normal to listOf(
        RedeclarationCheckPass,
        OwnedRecursiveCallCheckPass,
        ReturnAssignPass,
        InlineFunctionPass,
        RemoveParenPass,
        ExpandInPass,
        ExpandMatchPass,
        FlipNotConditionsPass,
        RaiseNotEqPass,
        ConstantFoldingPass,
        FlattenComplexExpressionsPass,
        InlineFunctionParametersPass,
        InlineFunctionCallAssignmentPass,
        InlineBlockPass,
        FlattenTempReassignPass,
        MapCallActionsPass,
        EmptyBlockCheckPass,
        // CleanupTempVarsPass,
        LimitCheckPass,
    ),
    Mode.Strict to listOf(
        RedeclarationCheckPass,
        OwnedRecursiveCallCheckPass,
        EmptyBlockCheckPass,
        MapCallActionsPass,
        LimitCheckPass,
    )
)

fun lower(ctx: LoweringCtx) {
    passes[ctx.sess.opts.mode]!!.forEach {
        it.run(ctx)
        // prettyPrintAst(Terminal(ansiLevel = AnsiLevel.ANSI256), ctx.ast)
    }
}

class LoweringCtx(val ast: Ast, val sess: CompileSess) {

    @PublishedApi
    internal val queryCache: MutableMap<KClass<*>, List<Any>> = mutableMapOf()

    fun dcx(): DiagCtx = sess.dcx()

    inline fun <reified T : Any> query(): List<T> {
        if (queryCache.containsKey(T::class)) return queryCache[T::class]!!.map { it as T }
        val visitor = QueryVisitor(T::class)
        visitor.visitAst(ast)
        return visitor.visited.map { it as T }.also { queryCache[T::class] = it }
    }

    inline fun <reified T : Any> clearQuery() {
        queryCache.remove(T::class)
    }

}

class QueryVisitor(private val type: KClass<*>) : AstVisitor {
    val visited = mutableListOf<Any>()

    override fun visitBlock(block: Block) {
        if (type.isInstance(block)) visited += block
        super.visitBlock(block)
    }

    override fun visitExpr(expr: Expr) {
        if (type.isInstance(expr)) visited += expr
        super.visitExpr(expr)
    }

    override fun visitItem(item: Item) {
        if (type.isInstance(item)) visited += item
        super.visitItem(item)
    }

    override fun visitStmt(stmt: Stmt) {
        if (type.isInstance(stmt)) visited += stmt
        super.visitStmt(stmt)
    }

}