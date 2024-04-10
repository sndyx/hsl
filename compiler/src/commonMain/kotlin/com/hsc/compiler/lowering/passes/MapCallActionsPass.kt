package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.action.*
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object MapCallActionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Expr>()
            .forEach { MapCallActionsVisitor(ctx).visitExpr(it) }
    }

}

private class MapCallActionsVisitor(val ctx: LoweringCtx) : AstVisitor {

    lateinit var p: ArgParser

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.global || !Action.builtins.contains(kind.ident.name)) return
                p = ArgParser(ctx, kind.args)
                val action = when (kind.ident.name) {
                    "apply_layout" -> parseLayout()
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
                if (action != null) expr.kind = ExprKind.Action(action)
            }
            else -> {} // Don't walk, we have already cached Exprs
        }
    }

    fun parseLayout(): Action {
        p.assertLength(1)
        return Action.ApplyInventoryLayout(
            p.nextStringLit()
        )
    }

    fun parsePotionEffect(): Action {
        p.assertLength(4)
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
        p.assertLength(2)
        val group = p.nextStringLit()
        val protectDemotion = p.nextBooleanLit()
        return Action.ChangePlayerGroup(group, protectDemotion)
    }

    fun parseActionBar(): Action {
        p.assertLength(1)
        val message = p.nextStringLit()
        return Action.DisplayActionBar(message)
    }

    fun parseDisplayMenu(): Action {
        p.assertLength(1)
        val menu = p.nextStringLit()
        return Action.DisplayMenu(menu)
    }

    fun parseDisplayTitle(): Action {
        p.assertLength(5)
        val title = p.nextStringLit()
        val subtitle = p.nextStringLit()
        val fadeIn = p.nextNumberLit().toInt()
        val stay = p.nextNumberLit().toInt()
        val fadeOut = p.nextNumberLit().toInt()
        return Action.DisplayTitle(title, subtitle, fadeIn, stay, fadeOut)
    }

    fun parseEnchantHeldItem(): Action {
        p.assertLength(2)
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
        p.assertLength(1)
        val message = p.nextStringLit()
        return Action.DisplayMenu(message)
    }

    fun parseGiveExpLevels(): Action {
        p.assertLength(1)
        val levels = p.nextNumberLit().toInt()
        return Action.GiveExperienceLevels(levels)
    }

    fun parseGiveItem(): Action {
        p.assertLength(4)
        val item = p.nextItemLit()
        val allowMultiple = p.nextBooleanLit()
        val inventorySlot = p.nextNumberLit().toInt()
        val replaceExistingItem = p.nextBooleanLit()
        return Action.GiveItem(item, allowMultiple, inventorySlot, replaceExistingItem)
    }

    fun parsePause(): Action {
        p.assertLength(1)
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
        p.assertLength(4)
        val soundString = p.nextStringLit()
        val sound = runCatching {
            Sound.valueOf(soundString)
        }.getOrElse {
            throw ctx.dcx().err("invalid sound `$soundString`")
        }
        val volume = TODO("GAHHHH, FLOAT PARSING IS DEAD")
    }

    fun parseSendMessage(): Action {
        p.assertLength(1)
        val message = p.nextStringLit()
        return Action.SendMessage(message)
    }

    fun parseRemoveItem(): Action {
        p.assertLength(1)
        val item = p.nextItemLit()
        return Action.RemoveItem(item)
    }

    fun parseSetPlayerTeam(): Action {
        p.assertLength(1)
        val team = p.nextStringLit()
        return Action.SetPlayerTeam(team)
    }

    fun parseSetGameMode(): Action {
        p.assertLength(1)
        val gameModeString = p.nextStringLit()
        val gameMode = runCatching {
            GameMode.valueOf(gameModeString)
        }.getOrElse {
            throw ctx.dcx().err("invalid gamemode `$gameModeString`")
        }
        return Action.SetGameMode(gameMode)
    }

    fun parseSetCompassTarget(): Action {
        p.assertLength(1)
        val location = p.nextLocation()
        return Action.SetCompassTarget(location)
    }

    fun parseTeleportPlayer(): Action {
        p.assertLength(1)
        val location = p.nextLocation()
        return Action.TeleportPlayer(location)
    }

    fun parseSendToLobby(): Action {
        p.assertLength(1)
        val lobbyString = p.nextStringLit()
        val lobby = runCatching {
            Lobby.valueOf(lobbyString)
        }.getOrElse {
            throw ctx.dcx().err("invalid lobby `$lobbyString`")
        }
        return Action.SendToLobby(lobby)
    }

}

private class ArgParser(val ctx: LoweringCtx, val args: Args) {

    var pos = 0

    fun assertLength(length: Int) {
        if (args.args.size != length) {
            val s1 = if (length == 1) "" else "s"
            val s2 = if (args.args.size == 1) "" else "s"
            val was = if (args.args.size == 1) "was" else "were"
            val err = ctx.dcx()
                .err("this function takes $length parameter$s1 but ${args.args.size} parameter$s2 $was supplied")
            err.span(args.span)
            throw err
        }
    }

    fun nextStringLit(): String {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Str -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected string, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected string, found ${kind.str()}")
            }
        }
    }

    fun nextNumberLit(): Long {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextValue(): StatValue {
        val expr = bump()
        return when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.I64 -> StatValue.I64(lit.value)
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            is ExprKind.Var -> {
                if (kind.ident.global) StatValue.Str("%stat.global/${kind.ident.name}%")
                else StatValue.Str("%stat.player/${kind.ident.name}%")
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextBooleanLit(): Boolean {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Bool -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextItemLit(): ItemStack {
        val expr = bump()
        when (val kind = expr.kind) {
            is ExprKind.Lit -> {
                when (val lit = kind.lit) {
                    is Lit.Item -> return lit.value
                    else -> {
                        throw ctx.dcx().err("expected integer, found ${lit.str()}")
                    }
                }
            }
            else -> {
                throw ctx.dcx().err("expected integer, found ${kind.str()}")
            }
        }
    }

    fun nextLocation(): Location {
        TODO()
    }


    fun bump(): Expr = args.args[pos++]

}