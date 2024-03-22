package com.hsc.compiler.codegen

import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import kotlinx.serialization.json.Json

val limits by lazy {
    Json.decodeFromString<Map<String, Int>>(
        Unit::class.java
            .classLoader
            .getResource("limits.json")!!
            .readText()
    )
}

fun stmtActionKind(stmt: Stmt): String {
    when (val kind = stmt.kind) {
        is StmtKind.Assign -> {
            return if (kind.ident.global) "change_global_stat"
            else "change_player_stat"
        }
        is StmtKind.AssignOp -> {
            return if (kind.ident.global) "change_global_stat"
            else "change_player_stat"
        }
        StmtKind.Break -> error("cannot map break stmt")
        StmtKind.Continue -> error("cannot map continue stmt")
        is StmtKind.Expr -> {
            TODO()
        }
        is StmtKind.For -> error("cannot map for stmt")
        is StmtKind.Ret -> error("cannot map return stmt")
        is StmtKind.While -> error("cannot map while stmt")
        is StmtKind.Action -> return "todo"
    }
}

fun limits(block: Block): Map<String, Int> {
    val actions = block.stmts.map { stmtActionKind(it) }

    return limits.map { (name, value) ->
        name to (value - actions.count { it == name })
    }.toMap()
}