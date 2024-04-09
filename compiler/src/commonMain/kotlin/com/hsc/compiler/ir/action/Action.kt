package com.hsc.compiler.ir.action

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
sealed class Action {

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
    @SerialName("BALANCE_TEAM")
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
    @SerialName("SET_HEALTH")
    data class ChangeHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String
    ) : Action()
    @Serializable
    @SerialName("SET_HUNGER_LEVEL")
    data class ChangeHungerLevel(
        @SerialName("mode") val op: StatOp,
        @SerialName("hunger") val value: String
    ) : Action()
    @Serializable
    @SerialName("SET_MAX_HEALTH")
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
    @SerialName("change_team_stat")
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
    @SerialName("BAIL_PARKOUR")
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

}

@Serializable
class ItemStack : MutableMap<String, Any> by mutableMapOf()

enum class Location {
    @SerialName("house_spawn") HouseSpawn,
    // We probably should never be using this
    @SerialName("current_location") CurrentLocation,
    @SerialName("invokers_location") InvokersLocation,
    @SerialName("custom_coordinates") CustomCoordinates,
}

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
enum class StatOp {
    @SerialName("SET") Set,
    @SerialName("INCREMENT") Inc,
    @SerialName("DECREMENT") Dec,
    @SerialName("MULTIPLY") Mul,
    @SerialName("DIVIDE") Div,
}

@Serializable
sealed class StatValue {
    @Serializable(with = StatI64Serializer::class)
    data class I64(val value: Long) : StatValue()
    @Serializable(with = StatStrSerializer::class)
    data class Str(val value: String) : StatValue()
}

object StatI64Serializer : KSerializer<StatValue.I64> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.I64) {
        encoder.encodeLong(value.value)
    }
    override fun deserialize(decoder: Decoder): StatValue.I64 { error("Not implemented!") }
}

object StatStrSerializer : KSerializer<StatValue.Str> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.Str) {
        encoder.encodeString(value.value)
    }
    override fun deserialize(decoder: Decoder): StatValue.Str { error("Not implemented!") }
}