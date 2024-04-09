package com.hsc.compiler.lowering.passes

import com.hsc.compiler.ir.action.Action
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx

object MapCallActionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        ctx.query<Expr>()
            .forEach { MapCallActionsVisitor(ctx).visitExpr(it) }
    }

}

/*
    @Serializable
    @SerialName("APPLY_LAYOUT")
    data class ApplyInventoryLayout(val layout: String) : Action()
    @Serializable
    @SerialName("POTION_EFFECT")
    data class ApplyPotionEffect(
        val effect: PotionEffect,
        val duration: Int,
        val level: Int,
        @SerialName("override_existing_effects")
        val override: Boolean
    ) : Action()
    @Serializable
    @SerialName("BALANCE_PLAYER_TEAM")
    data object BalancePlayerTeam : Action()
    @Serializable
    @SerialName("CANCEL_EVENT")
    data object CancelEvent : Action()
    @Serializable
    @SerialName("CHANGE_GLOBAL_STAT")
    data class ChangeGlobalStat(
        val stat: String,
        @SerialName("mode") val op: StatOp,
        val amount: StatValue
    ) : Action()
    @Serializable
    @SerialName("CHANGE_HEALTH")
    data class ChangeHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String
    ) : Action()
    @Serializable
    @SerialName("CHANGE_HUNGER_LEVEL")
    data class ChangeHungerLevel(
        @SerialName("mode") val op: StatOp,
        @SerialName("hunger") val value: String
    ) : Action()
    @Serializable
    @SerialName("CHANGE_MAX_HEALTH")
    data class ChangeMaxHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("max_health") val value: String,
        @SerialName("heal_on_change") val heal: Boolean
    ) : Action()
    @Serializable
    @SerialName("CHANGE_PLAYER_GROUP")
    data class ChangePlayerGroup(
        val group: String,
        @SerialName("demotion_protection") val protectDemotion: Boolean
    ) : Action()
    @Serializable
    @SerialName("CHANGE_STAT")
    data class ChangePlayerStat(
        val stat: String,
        @SerialName("mode") val op: StatOp,
        val amount: StatValue
    ) : Action()
    @Serializable
    @SerialName("CHANGE_TEAM_STAT")
    data class ChangeTeamStat(
        val stat: String,
        @SerialName("mode") val op: StatOp,
        val amount: StatValue,
        val team: String,
    ) : Action()
    @Serializable
    @SerialName("CLEAR_EFFECTS")
    data object ClearAllPotionEffects : Action()
    @Serializable
    @SerialName("CLOSE_MENU")
    data object CloseMenu : Action()
    @Serializable
    @SerialName("CONDITIONAL")
    data class Conditional(
        val conditions: List<Condition>,
        @SerialName("match_any_condition") val matchAnyCondition: Boolean,
        @SerialName("if_actions") val ifActions: List<Action>,
        @SerialName("else_actions") val elseActions: List<Action>,
    ) : Action()
    @Serializable
    @SerialName("ACTION_BAR")
    data class DisplayActionBar(val message: String) : Action()
    @Serializable
    @SerialName("DISPLAY_MENU")
    data class DisplayMenu(val menu: String) : Action()
    @Serializable
    @SerialName("TITLE")
    data class DisplayTitle(
        val title: String,
        val subtitle: String,
        @SerialName("fade_in") val fadeIn: Int,
        val stay: Int,
        @SerialName("fade_out") val fadeOut: Int,
    ) : Action()
    @Serializable
    @SerialName("ENCHANT_HELD_ITEM")
    data class EnchantHeldItem(
        val enchantment: Enchantment,
        val level: Int,
    ) : Action()
    @Serializable
    @SerialName("EXIT")
    data object Exit : Action()
    @Serializable
    @SerialName("FAIL_PARKOUR")
    data class FailParkour(val reason: String) : Action()
    @Serializable
    @SerialName("FULL_HEAL")
    data object FullHeal : Action()
    @Serializable
    @SerialName("GIVE_EXP_LEVELS")
    data class GiveExperienceLevels(val levels: Int) : Action()
    @Serializable
    @SerialName("GIVE_ITEM")
    data class GiveItem(
        val item: ItemStack,
        @SerialName("allow_multiple") val allowMultiple: Boolean,
        @SerialName("inventory_slot") val inventorySlot: Int,
        @SerialName("replace_existing_item") val replaceExistingItem: Boolean,
    )
    @Serializable
    @SerialName("SPAWN")
    data object GoToHouseSpawn : Action()
    @Serializable
    @SerialName("KILL")
    data object KillPlayer : Action()
    @Serializable
    @SerialName("PARKOUR_CHECKPOINT")
    data object ParkourCheckpoint : Action()
    @Serializable
    @SerialName("PAUSE")
    data class PauseExecution(@SerialName("ticks_to_wait") val ticks: Int) : Action()
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
    @Serializable
    @SerialName("RANDOM_ACTION")
    data class RandomAction(
        val actions: List<Action>,
    )
    @Serializable
    @SerialName("SEND_MESSAGE")
    data class SendMessage(val message: String) : Action()
    @Serializable
    @SerialName("TRIGGER_FUNCTION")
    data class ExecuteFunction(val name: String, val global: Boolean) : Action()
    @Serializable
    @SerialName("RESET_INVENTORY")
    data object ResetInventory : Action()
    @Serializable
    @SerialName("REMOVE_ITEM")
    data class RemoveItem(val item: ItemStack) : Action()
    @Serializable
    @SerialName("SET_PLAYER_TEAM")
    data class SetPlayerTeam(val team: String) : Action()
    @Serializable
    @SerialName("USE_HELD_ITEM")
    data object UseHeldItem : Action()
    @Serializable
    @SerialName("SET_GAMEMODE")
    data class SetGameMode(val gamemode: GameMode) : Action()
    @Serializable
    @SerialName("SET_COMPASS_TARGET")
    data class SetCompassTarget(val location: Location) : Action()
    @Serializable
    @SerialName("TELEPORT_PLAYER")
    data class TeleportPlayer(val location: Location) : Action()
    @Serializable
    @SerialName("SEND_TO_LOBBY")
    data class SendToLobby(val location: Lobby) : Action()
 */
private class MapCallActionsVisitor(val ctx: LoweringCtx) : AstVisitor {

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.global) return
                val action = when (kind.ident.name) {
                    "apply_layout" -> {
                        argParse(kind.args, Type.String)
                        ExprKind.Action(Action.ApplyInventoryLayout())
                    }
                    "send_message" -> {
                        argParse(kind.args, Type.String)
                        ExprKind.Action("send_message", kind.args.args, false)
                    }
                    "fail_parkour" -> {
                        argParse(kind.args, Type.String)
                        ExprKind.Action("fail_parkour", kind.args.args, false)
                    }
                    "is_sneaking" -> {
                        argParse(kind.args)
                        ExprKind.Action("is_sneaking", kind.args.args, true)
                    }
                    else -> null
                }
                if (action != null) expr.kind = action
            }
            else -> { } // Don't walk, we have already cached Exprs
        }
    }

    private fun argParse(args: Args, vararg types: Type) {
        if (args.args.size != types.size) {
            val s1 = if (types.size == 1) "" else "s"
            val s2 = if (args.args.size == 1) "" else "s"
            val was = if (args.args.size == 1) "was" else "were"
            val err = ctx.dcx().err("this function takes ${types.size} parameter$s1 but ${args.args.size} parameter$s2 $was supplied")
            err.span(args.span)
            err.emit()
            return
        }
        types.zip(args.args).forEach { (type, expr) ->
            val kind = expr.kind
            val matches = when (type) {
                // Maybe clean this up later...
                Type.String -> {
                    kind is ExprKind.Lit && kind.lit is Lit.Str
                }
                Type.Int -> {
                    kind is ExprKind.Lit && kind.lit is Lit.I64
                }
                Type.IntVar -> {
                    (kind is ExprKind.Lit && kind.lit is Lit.I64) || kind is ExprKind.Var
                }
                Type.Item -> {
                    kind is ExprKind.Lit && kind.lit is Lit.Item
                }
                Type.Bool -> {
                    kind is ExprKind.Lit && kind.lit is Lit.Bool
                }
            }

            if (!matches) {
                val err = ctx.dcx().err("expected ${type.str}, found ${expr.kind.str()}")
                err.span(expr.span)
                err.emit()
            }
        }
    }

}

private enum class Type(val str: kotlin.String) {
    String("string"),
    Int("integer"),
    IntVar("integer"),
    Item("item"),
    Bool("bool")
}