package com.hsc.compiler.ir.action

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

@Serializable
sealed class Action(
    @Transient val actionName: String = ""
) {

    companion object {
        val builtins = setOf(
            "apply_layout",
            "potion_effect",
            "balance_player_team",
            "cancel_event",
            "change_health",
            "change_hunger_level",
            "change_max_health",
            "change_player_group",
            "clear_effects",
            "close_menu",
            "action_bar",
            "display_menu",
            "title",
            "enchant_held_item",
            "exit",
            "fail_parkour",
            "full_heal",
            "give_exp_levels",
            "give_item",
            "spawn",
            "kill",
            "parkour_checkpoint",
            "pause",
            "play_sound",
            // "random_action",
            "send_message",
            "reset_inventory",
            "remove_item",
            "set_player_team",
            "use_held_item",
            "set_gamemode",
            "set_compass_target",
            "teleport_player",
            "send_to_lobby"
        )
    }

    @Serializable
    @SerialName("APPLY_LAYOUT")
    data class ApplyInventoryLayout(val layout: String) : Action("APPLY_LAYOUT")
    @Serializable
    @SerialName("POTION_EFFECT")
    data class ApplyPotionEffect(
        val effect: PotionEffect,
        val duration: Int,
        val level: Int,
        @SerialName("override_existing_effects")
        val override: Boolean
    ) : Action("POTION_EFFECT")
    @Serializable
    @SerialName("BALANCE_PLAYER_TEAM")
    data object BalancePlayerTeam : Action("BALANCE_PLAYER_TEAM")
    @Serializable
    @SerialName("CANCEL_EVENT")
    data object CancelEvent : Action("CANCEL_EVENT")
    @Serializable
    @SerialName("CHANGE_GLOBAL_STAT")
    data class ChangeGlobalStat(
        val stat: String,
        @SerialName("mode") val op: StatOp,
        val amount: StatValue
    ) : Action("CHANGE_GLOBAL_STAT")
    @Serializable
    @SerialName("CHANGE_HEALTH")
    data class ChangeHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String
    ) : Action("CHANGE_HEALTH")
    @Serializable
    @SerialName("CHANGE_HUNGER_LEVEL")
    data class ChangeHungerLevel(
        @SerialName("mode") val op: StatOp,
        @SerialName("hunger") val value: String
    ) : Action("CHANGE_HUNGER_LEVEL")
    @Serializable
    @SerialName("CHANGE_MAX_HEALTH")
    data class ChangeMaxHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("max_health") val value: String,
        @SerialName("heal_on_change") val heal: Boolean
    ) : Action("CHANGE_MAX_HEALTH")
    @Serializable
    @SerialName("CHANGE_PLAYER_GROUP")
    data class ChangePlayerGroup(
        val group: String,
        @SerialName("demotion_protection") val protectDemotion: Boolean
    ) : Action("CHANGE_PLAYER_GROUP")
    @Serializable
    @SerialName("CHANGE_STAT")
    data class ChangePlayerStat(
        val stat: String,
        @SerialName("mode") val op: StatOp,
        val amount: StatValue
    ) : Action("CHANGE_STAT")
    @Serializable
    @SerialName("CHANGE_TEAM_STAT")
    data class ChangeTeamStat(
        val stat: String,
        @SerialName("mode") val op: StatOp,
        val amount: StatValue,
        val team: String,
    ) : Action("CHANGE_TEAM_STAT")
    @Serializable
    @SerialName("CLEAR_EFFECTS")
    data object ClearAllPotionEffects : Action("CLEAR_EFFECTS")
    @Serializable
    @SerialName("CLOSE_MENU")
    data object CloseMenu : Action("CLOSE_MENU")
    @Serializable
    @SerialName("CONDITIONAL")
    data class Conditional(
        val conditions: List<Condition>,
        @SerialName("match_any_condition") val matchAnyCondition: Boolean,
        @SerialName("if_actions") val ifActions: List<Action>,
        @SerialName("else_actions") val elseActions: List<Action>,
    ) : Action("CONDITIONAL")
    @Serializable
    @SerialName("ACTION_BAR")
    data class DisplayActionBar(val message: String) : Action("ACTION_BAR")
    @Serializable
    @SerialName("DISPLAY_MENU")
    data class DisplayMenu(val menu: String) : Action("DISPLAY_MENU")
    @Serializable
    @SerialName("TITLE")
    data class DisplayTitle(
        val title: String,
        val subtitle: String,
        @SerialName("fade_in") val fadeIn: Int,
        val stay: Int,
        @SerialName("fade_out") val fadeOut: Int,
    ) : Action("TITLE")
    @Serializable
    @SerialName("ENCHANT_HELD_ITEM")
    data class EnchantHeldItem(
        val enchantment: Enchantment,
        val level: Int,
    ) : Action("ENCHANT_HELD_ITEM")
    @Serializable
    @SerialName("EXIT")
    data object Exit : Action("EXIT")
    @Serializable
    @SerialName("FAIL_PARKOUR")
    data class FailParkour(val reason: String) : Action("FAIL_PARKOUR")
    @Serializable
    @SerialName("FULL_HEAL")
    data object FullHeal : Action("FULL_HEAL")
    @Serializable
    @SerialName("GIVE_EXP_LEVELS")
    data class GiveExperienceLevels(val levels: Int) : Action("GIVE_EXP_LEVELS")
    @Serializable
    @SerialName("GIVE_ITEM")
    data class GiveItem(
        val item: ItemStack,
        @SerialName("allow_multiple") val allowMultiple: Boolean,
        @SerialName("inventory_slot") val inventorySlot: Int,
        @SerialName("replace_existing_item") val replaceExistingItem: Boolean,
    ) : Action("GIVE_ITEM")
    @Serializable
    @SerialName("SPAWN")
    data object GoToHouseSpawn : Action("SPAWN")
    @Serializable
    @SerialName("KILL")
    data object KillPlayer : Action("KILL")
    @Serializable
    @SerialName("PARKOUR_CHECKPOINT")
    data object ParkourCheckpoint : Action("PARKOUR_CHECKPOINT")
    @Serializable
    @SerialName("PAUSE")
    data class PauseExecution(@SerialName("ticks_to_wait") val ticks: Int) : Action("PAUSE")
    @Serializable
    @SerialName("PLAY_SOUND")
    data class PlaySound(
        val sound: Sound,
        val volume: Float,
        val pitch: Float,
        val location: Location,
    ) : Action("PLAY_SOUND")
    @Serializable
    @SerialName("RANDOM_ACTION")
    data class RandomAction(
        val actions: List<Action>,
    ) : Action("RANDOM_ACTION")
    @Serializable
    @SerialName("SEND_MESSAGE")
    data class SendMessage(val message: String) : Action("SEND_MESSAGE")
    @Serializable
    @SerialName("TRIGGER_FUNCTION")
    data class ExecuteFunction(val name: String, val global: Boolean) : Action("TRIGGER_FUNCTION")
    @Serializable
    @SerialName("RESET_INVENTORY")
    data object ResetInventory : Action("RESET_INVENTORY")
    @Serializable
    @SerialName("REMOVE_ITEM")
    data class RemoveItem(val item: ItemStack) : Action("REMOVE_ITEM")
    @Serializable
    @SerialName("SET_PLAYER_TEAM")
    data class SetPlayerTeam(val team: String) : Action("SET_PLAYER_TEAM")
    @Serializable
    @SerialName("USE_HELD_ITEM")
    data object UseHeldItem : Action("USE_HELD_ITEM")
    @Serializable
    @SerialName("SET_GAMEMODE")
    data class SetGameMode(val gamemode: GameMode) : Action("SET_GAMEMODE")
    @Serializable
    @SerialName("SET_COMPASS_TARGET")
    data class SetCompassTarget(val location: Location) : Action("SET_COMPASS_TARGET")
    @Serializable
    @SerialName("TELEPORT_PLAYER")
    data class TeleportPlayer(val location: Location) : Action("TELEPORT_PLAYER")
    @Serializable
    @SerialName("SEND_TO_LOBBY")
    data class SendToLobby(val location: Lobby) : Action("SEND_TO_LOBBY")

}

@Serializable(with = ItemStackSerializer::class)
data class ItemStack(
    val nbt: JsonObject,
)

@Serializable
data class Location(
    val relX: Long,
    val relY: Long,
    val relZ: Long,
    val x: Long,
    val y: Long,
    val z: Long,
    val pitch: Float,
    val yaw: Float,
)

enum class PotionEffect {
    @SerialName("strength") Strength,
    @SerialName("regeneration") Regeneration;
}

enum class Enchantment {
    @SerialName("protection") Protection;
}
enum class Sound {
    @SerialName("meow") Meow;
}

enum class GameMode {
    Adventure,
    Survival,
    Creative;
}

enum class Lobby {
    @SerialName("Main Lobby") MainLobby,
    @SerialName("Tournament Hall") TournamentHall,
    @SerialName("Blitz SG") BlitzSG,
    @SerialName("The TNT Games") TNTGames,
    @SerialName("Mega Walls") MegaWalls,
    @SerialName("Arcade Games") ArcadeGames,
    @SerialName("Cops and Crims") CopsAndCrims,
    @SerialName("UHC Champions") UHCChampions,
    @SerialName("Warlords") Warlords,
    @SerialName("Smash Heroes") SmashHeroes,
    @SerialName("Housing") Housing,
    @SerialName("SkyWars") SkyWars,
    @SerialName("Speed UHC") SpeedUHC,
    @SerialName("Classic Games") ClassicGames,
    @SerialName("Prototype") Prototype,
    @SerialName("Bed Wars") BedWars,
    @SerialName("Murder Mystery") MurderMystery,
    @SerialName("Build Battle") BuildBattle,
    @SerialName("Duels") Duels,
    @SerialName("Wool Wars") WoolWars;
}

enum class StatOp {
    @SerialName("SET") Set,
    @SerialName("INCREMENT") Inc,
    @SerialName("DECREMENT") Dec,
    @SerialName("MULTIPLY") Mul,
    @SerialName("DIVIDE") Div,
}

@Serializable(with = StatValueBaseSerializer::class)
sealed class StatValue {
    @Serializable(with = StatI64Serializer::class)
    data class I64(val value: Long) : StatValue()
    @Serializable(with = StatStrSerializer::class)
    data class Str(val value: String) : StatValue()
}

object ItemStackSerializer : KSerializer<ItemStack> {
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun deserialize(decoder: Decoder): ItemStack { error("not implemented!") }

    override fun serialize(encoder: Encoder, value: ItemStack) {
        JsonObject.serializer().serialize(encoder, value.nbt)
    }
}

// For the love of god, Kotlin will not choose a fucking polymorphic serializer
// for my sealed class (or tell me fucking why)!!!!! We have to do this garbage.
object StatValueBaseSerializer : KSerializer<StatValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("StatValueHellfireDespairPitsSerializer")

    override fun deserialize(decoder: Decoder): StatValue { error("Not implemented!") }

    override fun serialize(encoder: Encoder, value: StatValue) {
        if (value is StatValue.I64) {
            StatI64Serializer.serialize(encoder, value)
        } else {
            StatStrSerializer.serialize(encoder, value as StatValue.Str)
        }
    }

}

object StatI64Serializer : KSerializer<StatValue.I64> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.I64) {
        encoder.encodeLong(value.value)
    }
    override fun deserialize(decoder: Decoder): StatValue.I64 { error("Not implemented!") }
}

object StatStrSerializer : KSerializer<StatValue.Str> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.Str) {
        encoder.encodeString(value.value)
    }
    override fun deserialize(decoder: Decoder): StatValue.Str { error("Not implemented!") }
}