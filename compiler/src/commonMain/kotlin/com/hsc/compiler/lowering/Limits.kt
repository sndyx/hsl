package com.hsc.compiler.lowering

import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import com.hsc.compiler.span.Span

val limits = mapOf(
    "APPLY_LAYOUT" to 5,
    "POTION_EFFECT" to 22,
    "BALANCE_PLAYER_TEAM" to 1,
    "CANCEL_EVENT" to 1, // guess
    "CHANGE_GLOBAL_STAT" to 10,
    "CHANGE_HEALTH" to 5,
    "CHANGE_HUNGER_LEVEL" to 5,
    "CHANGE_MAX_HEALTH" to 5,
    "CHANGE_PLAYER_GROUP" to 1,
    "CHANGE_STAT" to 10,
    "CHANGE_TEAM_STAT" to 10,
    "CLEAR_EFFECTS" to 5,
    "CLOSE_MENU" to 1,
    "CONDITIONAL" to 15,
    "ACTION_BAR" to 5,
    "DISPLAY_MENU" to 10,
    "TITLE" to 5,
    "ENCHANT_HELD_ITEM" to 23, // wtf
    "EXIT" to 1, // guess
    "FAIL_PARKOUR" to 1,
    "FULL_HEAL" to 5,
    "GIVE_EXP_LEVELS" to 5,
    "GIVE_ITEM" to 20,
    "SPAWN" to 1,
    "KILL" to 1,
    "PARKOUR_CHECKPOINT" to 1,
    "PAUSE" to 30,
    "PLAY_SOUND" to 25,
    "RANDOM_ACTION" to 5,
    "SEND_MESSAGE" to 20,
    "TRIGGER_FUNCTION" to 10,
    "RESET_INVENTORY" to 1,
    "REMOVE_ITEM" to 20,
    "SET_PLAYER_TEAM" to 1,
    "USE_HELD_ITEM" to 20,
    "SET_GAMEMODE" to 1,
    "SET_COMPASS_TARGET" to 5,
    "TELEPORT_PLAYER" to 5,
    "SEND_TO_LOBBY" to 1,
)

fun stmtActionKind(stmt: Stmt): String {
    return when (val kind = stmt.kind) {
        is StmtKind.Action -> {
            kind.action.actionName
        }
        is StmtKind.Assign -> {
            if (kind.ident.isGlobal) "CHANGE_GLOBAL_STAT"
            else "CHANGE_STAT"
        }
        is StmtKind.AssignOp -> {
            if (kind.ident.isGlobal) "CHANGE_GLOBAL_STAT"
            else "CHANGE_STAT"
        }
        StmtKind.Break -> ""
        StmtKind.Continue -> ""
        is StmtKind.Expr -> {
            //
            ""
        }
        is StmtKind.For -> ""
        is StmtKind.Ret -> ""
        is StmtKind.While -> ""
    }
}

fun limits(block: Block): Map<String, Int> {
    val actions = block.stmts.map { stmtActionKind(it) }

    return limits.map { (name, value) ->
        name to (value - actions.count { it == name })
    }.toMap()
}

fun checkLimits(block: Block): Pair<String, Span>? {
    val map = limits.toMutableMap()

    for (stmt in block.stmts) {
        val kind = stmtActionKind(stmt)
        map[kind] = (map[kind] ?: Int.MAX_VALUE) - 1
        if (map[kind] == -1) {
            return Pair(kind, stmt.span)
        }
    }

    return null
}