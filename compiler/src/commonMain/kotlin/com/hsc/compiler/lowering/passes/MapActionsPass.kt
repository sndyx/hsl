package com.hsc.compiler.lowering.passes

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.*
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.ArgParser
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.similar

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

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.isGlobal || !Action.builtins.contains(kind.ident.name)) return
                p = ArgParser(ctx, kind.args)
                val action = when (kind.ident.name) {
                    "apply_layout" -> parseApplyLayout()
                    "potion_effect" -> parsePotionEffect()
                    "balance_player_team" -> Action.BalancePlayerTeam
                    "cancel_event" -> Action.CancelEvent
                    "change_player_group" -> parseChangePlayerGroup()
                    "clear_effects" -> Action.ClearAllPotionEffects
                    "close_menu" -> Action.CloseMenu
                    "action_bar" -> parseActionBar()
                    "display_menu" -> parseDisplayMenu()
                    "display_title" -> parseDisplayTitle()
                    "enchant_held_item" -> parseEnchantHeldItem()
                    "exit" -> Action.Exit
                    "fail_parkour" -> parseFailParkour()
                    "full_heal" -> Action.FullHeal
                    "give_exp_levels" -> parseGiveExpLevels()
                    "give_item" -> parseGiveItem()
                    "spawn" -> Action.GoToHouseSpawn
                    "kill" -> Action.KillPlayer
                    "parkour_checkpoint" -> Action.ParkourCheckpoint
                    "pause" -> parsePause()
                    "play_sound" -> parsePlaySound()
                    "send_message" -> parseSendMessage()
                    "reset_inventory" -> Action.ResetInventory
                    "remove_item" -> parseRemoveItem()
                    "set_player_team" -> parseSetPlayerTeam()
                    "use_held_item" -> Action.UseHeldItem
                    "set_gamemode" -> parseSetGameMode()
                    "set_compass_target" -> parseSetCompassTarget()
                    "teleport_player" -> parseTeleportPlayer()
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
        p.assertLength(1, "apply_layout(<layout>)")
        return Action.ApplyInventoryLayout(
            p.nextStringLit()
        )
    }

    fun parsePotionEffect(): Action {
        p.assertLength(4, "potion_effect(<effect>, <duration>, <level>, <override_existing_effects>)")
        val effectStr = p.nextStringLit()
        val effect = runCatching {
            PotionEffect.valueOf(effectStr)
        }.getOrElse {
            throw ctx.dcx().err("invalid potion effect `$effectStr`")
        }
        val duration = p.nextNumberLit().toInt()
        val level = p.nextNumberLit().toInt()
        val override = p.nextBooleanLit()
        return Action.ApplyPotionEffect(effect, duration, level, override)
    }

    fun parseChangePlayerGroup(): Action {
        p.assertLength(2, "change_player_group(<group>, <protect_demotion>)")
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
        p.assertLength(1, "display_menu(<menu>)")
        val menu = p.nextStringLit()
        return Action.DisplayMenu(menu)
    }

    fun parseDisplayTitle(): Action {
        p.assertLength(5, "display_title(<title>, <subtitle>, <fadein>, <stay>, <fadeout>)")
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
        val enchant = runCatching {
            Enchantment.valueOf(enchantString)
        }.getOrElse {
            throw ctx.dcx().err("invalid enchantment `$enchantString`")
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
        val inventorySlot = p.nextNumberLit().toInt()
        val replaceExistingItem = p.nextBooleanLit()
        return Action.GiveItem(item, allowMultiple, inventorySlot, replaceExistingItem)
    }

    fun parsePause(): Action {
        p.assertLength(1, "pause(<ticks>)")
        val ticks = p.nextNumberLit().toInt()
        return Action.PauseExecution(ticks)
    }

    /*
    @Serializable
    @SerialName("PLAY_SOUND")
    data class PlaySound(
        val sound: Sound,
        val volume: Float,
        val pitch: Float,
        // This is a terrible way to handle location but the serialization would be a nightmare otherwise...
        // users beware!
        val location: Location,
        val coordinates: String?,
    )
    */
    fun parsePlaySound(): Action {
        p.assertLength(4, "play_sound(<sound>, <TODO>)")
        val soundString = p.nextStringLit()
        val sound = Sound.entries.find { it.key.lowercase() == soundString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid sound `$soundString`", p.args.span)
            val options = Sound.entries.map { it.key }
            similar(soundString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        val volume = TODO("GAHHHH, FLOAT PARSING IS DEAD")
    }

    fun parseSendMessage(): Action {
        p.assertLength(1, "send_message(<message>)")
        val message = p.nextStringLit()
        return Action.SendMessage(message)
    }

    fun parseRemoveItem(): Action {
        p.assertLength(1, "remove_item(<item>)")
        val item = p.nextItemLit()
        return Action.RemoveItem(item)
    }

    fun parseSetPlayerTeam(): Action {
        p.assertLength(1, "set_player_team(<team>)")
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
        p.assertLength(1, "teleport_player(<location>)")
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