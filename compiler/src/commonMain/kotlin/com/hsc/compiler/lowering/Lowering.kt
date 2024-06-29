package com.hsc.compiler.lowering

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.passes.*
import kotlinx.datetime.Clock
import kotlin.reflect.KClass

private val passes: Map<Mode, List<AstPass>> = mapOf(
    Mode.Normal to listOf(
        CheckRedeclarationPass,
        ReturnAssignPass,
        InlineConstPass,
        InlineEnumPass,
        InlineFunctionPass,
        ExpandInPass,
        ExpandModPass,
        ExpandMatchPass,
        FlipNotConditionsPass,
        RaiseNotEqPass,
        RaiseUnaryMinusPass,
        ConstantFoldingPass,
        InlineBlockPass,
        ExpandComplexExpressionsPass,
        InlineFunctionParametersPass,
        MapActionsPass, // Before call assignment, or will become valid expression
        MapConditionsPass,
        InlineFunctionCallAssignmentPass,
        CollapseTempReassignPass,
        CheckEmptyBlockPass,
        CheckOwnedRecursiveCallPass,
        // CleanupTempVarsPass,
        CheckLimitsPass,
    ),
    Mode.Optimize to listOf(
        CheckRedeclarationPass,
        ReturnAssignPass,
        InlineConstPass,
        InlineEnumPass,
        InlineFunctionPass,
        ExpandInPass,
        ExpandModPass,
        ExpandMatchPass,
        FlipNotConditionsPass,
        RaiseNotEqPass,
        RaiseUnaryMinusPass,
        ConstantFoldingPass, // Before inline block, at least in optimize
        InlineBlockPass,
        ExpandComplexExpressionsPass,
        InlineFunctionParametersPass,
        MapActionsPass, // Before call assignment, or will become valid expression
        MapConditionsPass,
        InlineFunctionCallAssignmentPass,
        CollapseTempReassignPass,
        CheckEmptyBlockPass,
        CheckOwnedRecursiveCallPass,
        // CleanupTempVarsPass,
        CheckLimitsPass,
    ),
    Mode.Strict to listOf(
        CheckRedeclarationPass,
        CheckOwnedRecursiveCallPass,
        CheckEmptyBlockPass,
        MapActionsPass,
        CheckLimitsPass,
    )
)

fun lower(ctx: LoweringCtx) {
    passes[ctx.sess.opts.mode]!!.forEach {
        //val startTime = Clock.System.now()
        //println(it::class.simpleName)
        it.run(ctx)
        //print(" ")
        //println(Clock.System.now() - startTime)
        //prettyPrintAst(Terminal(ansiLevel = AnsiLevel.ANSI256), ctx.ast)
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

    override fun visitFn(fn: Fn) {
        if (type.isInstance(fn)) visited += fn
        super.visitFn(fn)
    }

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