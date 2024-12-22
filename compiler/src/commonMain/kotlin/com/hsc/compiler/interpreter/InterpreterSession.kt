package com.hsc.compiler.interpreter

import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.Ast
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class InterpreterSession(val sess: CompileSess, val ast: Ast, val terminal: Terminal) {

    fun run() = runBlocking {
        val housing = VirtualHousing()
        housing.terminal = terminal
        housing.functions.addAll(ast.items.filter { it.kind is ItemKind.Fn })
        val main = Player(housing, "main")
        housing.players.add(main)
        main.executeFunction("main")
        while (housing.isActive()) {
            if (!sess.opts.instant) delay(50)
            housing.tick()
        }
    }

}

class ScheduledExecution(var delay: Long, val stmts: List<Stmt>)
class FunctionCooldown(var delay: Long, val function: String)