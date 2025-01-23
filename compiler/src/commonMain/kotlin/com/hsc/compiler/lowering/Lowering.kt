package com.hsc.compiler.lowering

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.newpasses.*
import com.hsc.compiler.lowering.passes.*
import com.hsc.compiler.pretty.prettyPrintAst
import kotlinx.datetime.Clock
import kotlin.reflect.KClass

private val passes: Map<Mode, List<AstPass>> = mapOf(
    Mode.Normal to listOf(
        NameItemsPass,
        CheckRedeclarationPass,
        ReturnAssignPass,
        InlineConstPass,
        InlineEnumPass,
        InlineFunctionPass,
        CheckTempVariablesAssignedBeforeUsePass, // after InlineFunctionPass, for several reasons
        ExpandInPass,
        ExpandModPass,
        ExpandMatchPass,
        FlipNotConditionsPass,
        RaiseNotEqPass,
        RaiseUnaryMinusPass,
        ConstantFoldingPass,
        InlineBlockPass,
        InlineFunctionParametersPass,
        ExpandComplexExpressionsPass,
        MapActionsPass, // Before call assignment, or will become valid expression
        MapConditionsPass,
        InlineFunctionCallAssignmentPass,
        ExpandIfExpressionPass,
        CollapseTempReassignPass,
        CheckEmptyBlockPass,
        CheckOwnedRecursiveCallPass,
        FindTempVariablesPass,
        CheckLimitsPass,
        CleanupTempVarsPass,
        CheckLimitsPass,
    ),
    Mode.Optimize to listOf(
        PassWrapper(::convertStrings),
        NameItemsPass,
        CheckRedeclarationPass,
        PassWrapper(::inlineConsts),
        InlineEnumPass,
        PassWrapper(::inlineFunctions),
        ReturnAssignPass, // should come after InlineFunctionPass
        FindTempVariablesPass, // earlier to allow all temp variables in ExpandComplexExpressionsPass
        FindLastVariableUsagePass,
        // ignored in #strict
        CheckTempVariablesAssignedBeforeUsePass, // after InlineFunctionPass, for several reasons
        ExpandInPass,
        ExpandModPass,
        ExpandMatchPass,
        PassWrapper(::expandComplexConditions),
        FlipNotConditionsPass,
        RaiseNotEqPass,
        RaiseUnaryMinusPass,
        // ignored in #strict
        PassWrapper(::inlineFunctionParameters),
        ConstantFoldingPass, // Before inline block, at least in optimize
        // ignored in #strict
        PassWrapper(::inlineBlocks),
        // ignored in #strict
        PassWrapper(::expandComplexExpressions),
        PassWrapper(::foldStrings),
        MapActionsPass, // Before call assignment, or will become valid expression
        MapConditionsPass,
        // ignored in #strict
        InlineFunctionCallAssignmentPass,
        ExpandIfExpressionPass,
        // ignored in #strict
        CollapseTempReassignPass,
        CheckEmptyBlockPass,
        CheckOwnedRecursiveCallPass,
        // ignored in #strict
        PassWrapper(::extendLimits),
        // ignored in #strict
        CleanupTempVarsPass,
        // ignored in #strict
        PassWrapper(::extendLimits),
        // ignored in #strict
        PassWrapper(::checkLimits)
    ),
    Mode.Strict to listOf(
        NameItemsPass,
        CheckRedeclarationPass,
        CheckOwnedRecursiveCallPass,
        RaiseUnaryMinusPass,
        CheckEmptyBlockPass,
        MapActionsPass,
        CheckLimitsPass,
    )
)

fun lower(ctx: LoweringCtx) {
    passes[ctx.sess.opts.mode]!!.forEach {
        //val startTime = Clock.System.now()
        //println(it::class.simpleName)
        //if (it is PassWrapper) println(it.pass.toString())
        it.run(ctx)
        //print(" ")
        //println(Clock.System.now() - startTime)
        //prettyPrintAst(Terminal(ansiLevel = AnsiLevel.ANSI256), ctx.ast)
    }

    // prettyPrintAst(Terminal(ansiLevel = AnsiLevel.ANSI256), ctx.ast)
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

    override fun visitLit(lit: Lit) {
        if (type.isInstance(lit)) visited += lit
        super.visitLit(lit)
    }

}