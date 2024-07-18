package com.hsc.compiler.interpreter

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind

fun Player.executeFunction(name: String) {
    val item = VirtualHousing.functions.find { it.ident.name == name } ?:
        throw ActionRuntimeException("cannot find function $name", trace)

    val fn = (item.kind as ItemKind.Fn).fn

    if (cooldowns.any { it.function == item.ident.name }) return
    cooldowns.add(FunctionCooldown(5, item.ident.name))

    try {
        executeBlock(fn.block)
    } catch (a: ActionExitException) {
        // this is fine, player has run exit() action
    }
}

fun Player.executeBlock(block: Block) {
    executeBlock(block.stmts)
}

fun Player.executeBlock(stmts: List<Stmt>) {
    var i = 0
    for (stmt in stmts) {
        if (stmt.kind is StmtKind.Action && (stmt.kind as StmtKind.Action).action is Action.PauseExecution) {
            val pauseTicks = ((stmt.kind as StmtKind.Action).action as Action.PauseExecution).ticks
            schedule(pauseTicks.toLong(), stmts.subList(i + 1, stmts.size))
            return
        }
        executeStmt(stmt)
        i++
    }
}