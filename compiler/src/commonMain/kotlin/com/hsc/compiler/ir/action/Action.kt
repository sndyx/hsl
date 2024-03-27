package com.hsc.compiler.ir.action

import kotlinx.serialization.*

@Serializable
sealed class Action {

    @Serializable
    @SerialName("apply_inventory_layout")
    data class ApplyInventoryLayout(val layout: String) : Action()
    @Serializable
    @SerialName("apply_potion_effect")
    data class ApplyPotionEffect(
        val effect: PotionEffect,
        val duration: Int,
        val amplifier: Int,
        @SerialName("overrideExistingEffects")
        val override: Boolean
    ) : Action()
    @Serializable
    @SerialName("balance_player_team")
    data object BalancePlayerTeam : Action()
    @Serializable
    @SerialName("cancel_event")
    data object CancelEvent : Action()
    @Serializable
    @SerialName("change_global_stat")
    data class ChangeGlobalStat(
        val name: String,
        @SerialName("mode") val op: StatOp,
        val value: String
    ) : Action()
    @Serializable
    @SerialName("change_health")
    data class ChangeHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String
    ) : Action()
    @Serializable
    @SerialName("change_hunger_level")
    data class ChangeHungerLevel(
        @SerialName("mode") val op: StatOp,
        @SerialName("hunger") val value: String
    ) : Action()
    @Serializable
    @SerialName("change_max_health")
    data class ChangeMaxHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String,
        @SerialName("healOnChange") val heal: Boolean
    ) : Action()
    @Serializable
    @SerialName("change_player_group")
    data class ChangePlayerGroup(val group: String, val protectDemotion: Boolean) : Action()
    @Serializable
    @SerialName("change_player_stat")
    data class ChangePlayerStat(
        val name: String,
        @SerialName("mode") val op: StatOp,
        val value: String
    ) : Action()
    @Serializable
    @SerialName("change_team_stat")
    data class ChangeTeamStat(
        val name: String,
        @SerialName("mode") val op: StatOp,
        val value: String
    ) : Action()
    @Serializable
    @SerialName("clear_all_potion_effects")
    data object ClearAllPotionEffects : Action()
    @Serializable
    @SerialName("close_menu")
    data object CloseMenu : Action()
    @Serializable
    @SerialName("conditional")
    data class Conditional(
        val conditions: List<Condition>,
        val matchAnyCondition: Boolean,
        @SerialName("if") val ifActions: List<Action>,
        @SerialName("else") val elseActions: List<Action>,
    ) : Action()
    @Serializable
    @SerialName("display_action_bar")
    data class DisplayActionBar(val message: String) : Action()
    @Serializable
    @SerialName("display_menu")
    data class DisplayMenu(val menu: String) : Action()
    @Serializable
    @SerialName("display_title")
    data class DisplayTitle(
        val title: String,
        val subtitle: String,
        val fadeIn: Int,
        val stay: Int,
        val fadeOut: Int,
    ) : Action()
    @Serializable
    @SerialName("enchant_held_item")
    data class EnchantHeldItem(
        val enchantment: Enchantment,
        val level: Int,
    ) : Action()
    @Serializable
    @SerialName("exit")
    data object Exit : Action()
    @Serializable
    @SerialName("fail_parkour")
    data class FailParkour(val reason: String) : Action()
    @Serializable
    @SerialName("full_heal")
    data object FullHeal : Action()
    @Serializable
    @SerialName("give_experience_levels")
    data class GiveExperienceLevels(val levels: Int) : Action()
    @Serializable
    @SerialName("give_item")
    data class GiveItem(
        val item: ItemStack,
        val allowMultiple: Boolean,
        val inventorySlot: Int,
        val replaceExistingItem: Boolean,
    )
    @Serializable
    @SerialName("go_to_house_spawn")
    data object GoToHouseSpawn : Action()
    @Serializable
    @SerialName("kill_player")
    data object KillPlayer : Action()
    @Serializable
    @SerialName("parkour_checkpoint")
    data object ParkourCheckpoint : Action()
    @Serializable
    @SerialName("pause_execution")
    data class PauseExecution(val ticks: Int) : Action()
    @Serializable
    @SerialName("play_sound")
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
    @SerialName("random_action")
    data class RandomAction(
        val actions: List<Action>,
    )
    @Serializable
    @SerialName("send_message")
    data class SendMessage(val message: String) : Action()
    @Serializable
    @SerialName("execute_function")
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
    @SerialName("set") Set,
    @SerialName("increment") Inc,
    @SerialName("decrement") Dec,
    @SerialName("multiply") Mul,
    @SerialName("divide") Div,
}