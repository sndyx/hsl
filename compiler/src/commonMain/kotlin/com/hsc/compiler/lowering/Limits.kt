package com.hsc.compiler.lowering

import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import kotlinx.serialization.json.Json

val limits by lazy {
    Json.decodeFromString<Map<String, Int>>(
        """
            {
              "apply_inventory_layout": 5,
              "apply_potion_effect": 22,
              "balance_player_team": 1,
              "change_global_stat": 10,
              "change_health": 5,
              "change_player_group": 1,
              "change_player_stat": 10,
              "change_team_stat": 10,
              "clear_all_potion_effects": 5,
              "close_menu": 1,
              "conditional": 15,
              "display_action_bar": 5,
              "display_menu": 10,
              "display_title": 5,
              "enchant_held_item": 23,
              "fail_parkour": 1,
              "full_heal": 5,
              "give_experience_levels": 5,
              "give_item": 20,
              "go_to_house_spawn": 1,
              "kill_player": 1,
              "parkour_checkpoint": 1,
              "play_sound": 25,
              "pause_execution": 30,
              "random_action": 5,
              "remove_item": 20,
              "reset_inventory": 1,
              "send_a_chat_message": 20,
              "send_to_lobby": 1,
              "set_compass_target": 5,
              "set_gamemode": 1,
              "set_hunger_level": 5,
              "set_max_health": 5,
              "set_player_team": 1,
              "teleport_player": 5,
              "trigger_function": 10,
              "use_remove_held_item": 1
            }
        """.trimIndent()
    )
}

fun stmtActionKind(stmt: Stmt): String {
    return when (val kind = stmt.kind) {
        is StmtKind.Assign -> {
            if (kind.ident.global) "change_global_stat"
            else "change_player_stat"
        }
        is StmtKind.AssignOp -> {
            if (kind.ident.global) "change_global_stat"
            else "change_player_stat"
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
        is StmtKind.Action -> "todo"
    }
}

fun limits(block: Block): Map<String, Int> {
    val actions = block.stmts.map { stmtActionKind(it) }

    return limits.map { (name, value) ->
        name to (value - actions.count { it == name })
    }.toMap()
}