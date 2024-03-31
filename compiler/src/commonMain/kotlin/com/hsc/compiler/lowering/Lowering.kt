package com.hsc.compiler.lowering

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.passes.*
import kotlin.reflect.KClass

private val passes: Map<Mode, List<AstPass>> = mapOf(
    Mode.Normal to listOf(
        RedeclarationCheckPass,
        OwnedRecursiveCallCheckPass,
        ReturnAssignPass,
        InlineFunctionPass,
        RemoveParenPass,
        EvaluateConstantEquationsPass,
        FlattenComplexExpressionsPass,
        MapCallActionsPass,
        InlineFunctionParametersPass,
        InlineFunctionCallAssignmentPass,
        FlattenTempReassignPass,
        EmptyBlockCheckPass,
        // CleanupTempVarsPass,
        LimitCheckPass,
    ),
    Mode.Strict to listOf(
        RedeclarationCheckPass,
        OwnedRecursiveCallCheckPass,
        EmptyBlockCheckPass,
        LimitCheckPass,
    )
)

fun lower(ctx: LoweringCtx) {
    passes[ctx.sess.opts.mode]!!.forEach {
        it.run(ctx)
    }
}

class LoweringCtx(val ast: Ast, val sess: CompileSess) {

    @PublishedApi
    internal val cache: Map<ULong, Any>
    @PublishedApi
    internal val queryCache: MutableMap<KClass<*>, List<Any>> = mutableMapOf()

    init {
        val visitor = NodeVisitor()
        visitor.visitAst(ast)
        cache = visitor.visited
    }

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

    inline fun <reified T : Any> node(id: NodeId): T = node(id.id)
    inline fun <reified T : Any> node(id: ULong): T = cache[id] as T

}

class NodeVisitor : AstVisitor {
    val visited = mutableMapOf<ULong, Any>()

    override fun visitBlock(block: Block) {
        visited[block.id.id] = block
        super.visitBlock(block)
    }

    override fun visitExpr(expr: Expr) {
        visited[expr.id.id] = expr
        super.visitExpr(expr)
    }

    override fun visitItem(item: Item) {
        visited[item.id.id] = item
        super.visitItem(item)
    }

    override fun visitStmt(stmt: Stmt) {
        visited[stmt.id.id] = stmt
        super.visitStmt(stmt)
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