package com.hsc.compiler.lowering.passes

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.*
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.ArgParser
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.similar
import com.hsc.compiler.lowering.statValueOf

object MapActionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val visitor = MapCallActionsVisitor(ctx)
        ctx.query<Block>().forEach {
            visitor.visitBlock(it)
        }
    }

}

private class MapCallActionsVisitor(val ctx: LoweringCtx) : BlockAwareVisitor() {

    lateinit var p: ArgParser

    override fun visitStmt(stmt: Stmt) {
        super.visitStmt(stmt)

        val ident = stmt.assign()?.ident ?: stmt.assignOp()?.ident ?: return
        if (!ident.isTeam || (ident as Ident.Team).team != "player") return

        val name = ident.name
        if (name != "health" && name != "hunger" && name != "max_health") return

        val op = if (stmt.assign() != null) StatOp.Set
        else {
            when (stmt.assignOp()?.kind) {
                BinOpKind.Add -> StatOp.Inc
                BinOpKind.Sub -> StatOp.Dec
                BinOpKind.Mul -> StatOp.Mul
                BinOpKind.Div -> StatOp.Div
                else -> error("wrong op")
            }
        }

        val value = ctx.statValueOf(stmt.assign()?.expr ?: stmt.assignOp()!!.expr)

        val action = when (ident.name) {
            "health" -> {
                Action.ChangeHealth(op, value)
            }
            "hunger" -> {
                Action.ChangeHungerLevel(op, value)
            }
            "max_health" -> {
                Action.ChangeMaxHealth(op, value, false)
            }
            else -> error("unreachable")
        }

        stmt.kind = StmtKind.Action(action)
    }

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.isGlobal || !Action.builtins.contains(kind.ident.name)) return
                p = ArgParser(ctx, kind.args)
                val action = when (kind.ident.name) {
                    "set_layout" -> parseApplyLayout()
                    "effect" -> parsePotionEffect()
                    "balance_team" -> Action.BalancePlayerTeam
                    "cancel_event" -> Action.CancelEvent
                    "set_group" -> parseChangePlayerGroup()
                    "clear_effects" -> Action.ClearAllPotionEffects
                    "close_menu" -> Action.CloseMenu
                    "action_bar" -> parseActionBar()
                    "open_menu" -> parseDisplayMenu()
                    "title" -> parseDisplayTitle()
                    "enchant_held_item" -> parseEnchantHeldItem()
                    "exit" -> Action.Exit
                    "fail_parkour" -> parseFailParkour()
                    "heal" -> Action.FullHeal
                    "give_exp_levels" -> parseGiveExpLevels()
                    "give_item" -> parseGiveItem()
                    "kill" -> Action.KillPlayer
                    "parkour_checkpoint" -> Action.ParkourCheckpoint
                    "pause" -> parsePause()
                    "sound" -> parsePlaySound()
                    "message" -> parseSendMessage()
                    "reset_inventory" -> Action.ResetInventory
                    "remove_item" -> parseRemoveItem()
                    "set_team" -> parseSetPlayerTeam()
                    "remove_held_item" -> Action.UseHeldItem
                    "set_gamemode" -> parseSetGameMode()
                    "set_compass_target" -> parseSetCompassTarget()
                    "tp" -> parseTeleportPlayer()
                    "send_to_lobby" -> parseSendToLobby()
                    else -> null
                }
                if (action != null) {
                    // Ensure this statement is used correctly
                    val stmt = currentBlock.stmts[currentPosition]
                    if (stmt.kind is StmtKind.Expr && (stmt.kind as StmtKind.Expr).expr == expr) {
                        stmt.kind = StmtKind.Action(action)
                    } else {
                        ctx.dcx().err("expected expression, found statement", expr.span).emit()
                    }
                }
            }

            else -> {}
        }
    }

    fun parseApplyLayout(): Action {
        p.assertLength(1, "set_layout(<layout>)")
        return Action.ApplyInventoryLayout(
            p.nextStringLit()
        )
    }

    fun parsePotionEffect(): Action {
        p.assertLength(4, "effect(<effect>, <duration>, <level>, <override_existing_effects>)")
        val effectString = p.nextStringLit()
        val effect = PotionEffect.entries.find { it.key.lowercase() == effectString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid effect `$effectString`", p.args.span)
            val options = PotionEffect.entries.map { it.key }
            similar(effectString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        val duration = p.nextNumberLit().toInt()
        val level = p.nextNumberLit().toInt()
        val override = p.nextBooleanLit()
        return Action.ApplyPotionEffect(effect, duration, level, override)
    }

    fun parseChangePlayerGroup(): Action {
        p.assertLength(2, "set_group(<group>, <protect_demotion>)")
        val group = p.nextStringLit()
        val protectDemotion = p.nextBooleanLit()
        return Action.ChangePlayerGroup(group, protectDemotion)
    }

    fun parseActionBar(): Action {
        p.assertLength(1, "action_bar(<message>)")
        val message = p.nextStringLit()
        return Action.DisplayActionBar(message)
    }

    fun parseDisplayMenu(): Action {
        p.assertLength(1, "open_menu(<menu>)")
        val menu = p.nextStringLit()
        return Action.DisplayMenu(menu)
    }

    fun parseDisplayTitle(): Action {
        p.assertLength(5, "title(<title>, <subtitle>, <fadein>, <stay>, <fadeout>)")
        val title = p.nextStringLit()
        val subtitle = p.nextStringLit()
        val fadeIn = p.nextNumberLit().toInt()
        val stay = p.nextNumberLit().toInt()
        val fadeOut = p.nextNumberLit().toInt()
        return Action.DisplayTitle(title, subtitle, fadeIn, stay, fadeOut)
    }

    fun parseEnchantHeldItem(): Action {
        p.assertLength(2, "enchant_held_item(<enchantment>, <level>)")
        val enchantString = p.nextStringLit()
        val enchant = Enchantment.entries.find { it.key.lowercase() == enchantString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid enchant `$enchantString`", p.args.span)
            val options = Enchantment.entries.map { it.key }
            similar(enchantString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        val level = p.nextNumberLit().toInt()
        return Action.EnchantHeldItem(enchant, level)
    }

    fun parseFailParkour(): Action {
        p.assertLength(1, "fail_parkour(<message>)")
        val message = p.nextStringLit()
        return Action.DisplayMenu(message)
    }

    fun parseGiveExpLevels(): Action {
        p.assertLength(1, "give_exp_levels(<levels>)")
        val levels = p.nextNumberLit().toInt()
        return Action.GiveExperienceLevels(levels)
    }

    fun parseGiveItem(): Action {
        p.assertLength(4, "give_item(<item>, <allow_multiple>, <inventory_slot>, <replace_existing>)")
        val item = p.nextItemLit()
        val allowMultiple = p.nextBooleanLit()
        val inventorySlot = p.nextValue()
        val replaceExistingItem = p.nextBooleanLit()
        return Action.GiveItem(item, allowMultiple, inventorySlot, replaceExistingItem)
    }

    fun parsePause(): Action {
        p.assertLength(1, "pause(<ticks>)")
        val ticks = p.nextNumberLit().toInt()
        return Action.PauseExecution(ticks)
    }

    fun parsePlaySound(): Action {
        p.assertLength(4, "sound(<sound>, <volume>, <pitch>, <location>)")
        val soundString = p.nextStringLit()
        val sound = Sound.entries.find {
            it.key.lowercase() == soundString.lowercase() || it.label.lowercase() == soundString.lowercase()
        } ?: run {
            val err = ctx.dcx().err("invalid sound `$soundString`", p.args.span)
            val options = Sound.entries.map { it.label }
            similar(soundString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        val volume = p.nextFloatLit()
        val pitch = p.nextFloatLit()
        val location = p.nextLocation()
        return Action.PlaySound(sound, volume, pitch, location)
    }

    fun parseSendMessage(): Action {
        p.assertLength(1, "message(<message>)")
        val message = p.nextStringLit()
        return Action.SendMessage(message)
    }

    fun parseRemoveItem(): Action {
        p.assertLength(1, "remove_item(<item>)")
        val item = p.nextItemLit()
        return Action.RemoveItem(item)
    }

    fun parseSetPlayerTeam(): Action {
        p.assertLength(1, "set_team(<team>)")
        val team = p.nextStringLit()
        return Action.SetPlayerTeam(team)
    }

    fun parseSetGameMode(): Action {
        p.assertLength(1, "set_gamemode(<gamemode>)")
        val gameModeString = p.nextStringLit()
        val gameMode = GameMode.entries.find { it.key.lowercase() == gameModeString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid gamemode `$gameModeString`", p.args.span)
            val options = GameMode.entries.map { it.key }
            similar(gameModeString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        return Action.SetGameMode(gameMode)
    }

    fun parseSetCompassTarget(): Action {
        p.assertLength(1, "set_compass_target(<location>)")
        val location = p.nextLocation()
        return Action.SetCompassTarget(location)
    }

    fun parseTeleportPlayer(): Action {
        p.assertLength(1, "teleport(<location>)")
        val location = p.nextLocation()
        return Action.TeleportPlayer(location)
    }

    fun parseSendToLobby(): Action {
        p.assertLength(1, "send_to_lobby(<lobby>)")
        val lobbyString = p.nextStringLit()
        val lobby = Lobby.entries.find { it.key.lowercase() == lobbyString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid lobby `$lobbyString`", p.args.span)
            val options = Lobby.entries.map { it.key }
            similar(lobbyString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        return Action.SendToLobby(lobby)
    }

}