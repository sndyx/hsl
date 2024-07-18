package com.hsc.compiler.interpreter

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.action.GameMode
import com.hsc.compiler.ir.action.Location
import com.hsc.compiler.ir.action.StatOp
import com.hsc.compiler.ir.ast.BinOpKind
import com.hsc.compiler.ir.ast.Stmt
import com.hsc.compiler.ir.ast.StmtKind
import kotlin.math.min


fun Player.executeStmt(stmt: Stmt) {
    when (val kind = stmt.kind) {
        is StmtKind.Action -> executeAction(kind.action)
        is StmtKind.Assign -> {
            setStat(kind.ident, exprValue(kind.expr))
        }
        is StmtKind.AssignOp -> {
            val a = getStat(kind.ident)
            val b = exprValue(kind.expr)
            val result = when (kind.kind) {
                BinOpKind.Add -> a + b
                BinOpKind.Sub -> a - b
                BinOpKind.Mul -> a * b
                BinOpKind.Div -> if (b == 0L) 0L else a / b
                else -> error("unreachable")
            }
            setStat(kind.ident, result)
        }
        is StmtKind.Random -> {
            executeStmt(kind.block.stmts.random())
        }
        is StmtKind.Expr -> {
            executeExpr(kind.expr)
        }
        else -> error("unreachable")
    }
}

fun Player.executeAction(action: Action) {
    when (action) {
        is Action.ApplyInventoryLayout -> { /* Do nothing */ }
        is Action.ApplyPotionEffect -> TODO()
        Action.BalancePlayerTeam -> TODO()
        Action.CancelEvent -> { /* Do nothing */ }
        is Action.ChangeHealth -> health = min(compute(action.op, health.toLong(), exprValue(action.value)).toInt(), maxHealth)
        is Action.ChangeHungerLevel -> hunger = compute(action.op, hunger.toLong(), exprValue(action.value)).toInt()
        is Action.ChangeMaxHealth -> maxHealth = compute(action.op, maxHealth.toLong(), exprValue(action.value)).toInt()
        is Action.ChangePlayerGroup -> group = action.group
        Action.ClearAllPotionEffects -> TODO()
        Action.CloseMenu -> { /* Do nothing */ }
        is Action.DisplayActionBar -> logActionBar(action.message)
        is Action.DisplayMenu -> logDisplayMenu(action.menu)
        is Action.DisplayTitle -> logTitle(action.title, action.subtitle)
        is Action.EnchantHeldItem -> { /* Do nothing */ }
        is Action.ExecuteFunction -> {
            if (action.global) {
                VirtualHousing.players.forEach {
                    executeFunction(action.name)
                }
            }
        }
        Action.Exit -> throw ActionExitException()
        is Action.FailParkour -> { /* Do nothing */ }
        Action.FullHeal -> health = maxHealth
        is Action.GiveExperienceLevels -> experience += action.levels
        is Action.GiveItem -> { /* Do nothing */ }
        Action.KillPlayer -> health = maxHealth
        Action.ParkourCheckpoint -> { /* Do nothing */ }
        is Action.PauseExecution -> { /* Impl in Function.kt */ }
        is Action.PlaySound -> logSound(action.sound)
        is Action.RemoveItem -> { /* Do nothing */ }
        Action.ResetInventory -> { /* Do nothing */ }
        is Action.SendMessage -> logMessage(action.message)
        is Action.SendToLobby -> {
            VirtualHousing.players.remove(this@executeAction)
            throw ActionExitException()
        }
        is Action.SetCompassTarget -> { /* Do nothing */ }
        is Action.SetGameMode -> gamemode = when(action.gamemode) {
            GameMode.Creative -> Player.GameMode.Creative
            GameMode.Survival -> Player.GameMode.Survival
            GameMode.Adventure -> Player.GameMode.Adventure
        }
        is Action.SetPlayerTeam -> team = action.team
        is Action.TeleportPlayer -> {
            when (val loc = action.location) {
                is Location.CurrentLocation, is Location.InvokersLocation -> { /* Do nothing */ }
                is Location.HouseSpawn -> {
                    x = 0.0
                    y = 0.0
                    z = 0.0
                    pitch = 0.0f
                    yaw = 0.0f
                }
                is Location.Custom -> {
                    x = if (loc.relX) x + (loc.x ?: 0.0) else loc.x!!
                    y = if (loc.relY) y + (loc.y ?: 0.0) else loc.y!!
                    z = if (loc.relZ) z + (loc.z ?: 0.0) else loc.z!!
                    loc.pitch?.let {
                        pitch = loc.pitch
                        yaw = loc.yaw!! // yaw should always be assigned if pitch is assigned
                    }
                }
            }
        }
        Action.UseHeldItem -> { /* Do nothing */ }
        else -> error("unreachable")
    }
}

fun Player.compute(op: StatOp, a: Long, b: Long): Long {
    return when (op) {
        StatOp.Set -> b
        StatOp.Inc -> a + b
        StatOp.Dec -> a - b
        StatOp.Mul -> a * b
        StatOp.Div -> if (b == 0L) 0L else a / b // prevent DivideByZeroError
    }
}