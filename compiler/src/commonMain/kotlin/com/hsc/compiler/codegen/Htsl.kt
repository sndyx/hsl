package com.hsc.compiler.codegen

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.action.*
import com.hsc.compiler.ir.action.Function
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

private val actionMap = mapOf(
    "apply_layout" to "applyLayout",
    "potion_effect" to "applyPotion",
    "balance_player_team" to "balanceTeam",
    "cancel_event" to "cancelEvent",
    "change_health" to "changeHealth",
    "change_hunger_level" to "hungerLevel",
    "change_max_health" to "maxHealth",
    "change_player_group" to "changePlayerGroup",
    "clear_effects" to "clearEffects",
    "close_menu" to "closeMenu",
    "action_bar" to "actionBar",
    "display_menu" to "displayMenu",
    "title" to "title",
    "enchant_held_item" to "enchant",
    "exit" to "exit",
    "fail_parkour" to "failParkour",
    "full_heal" to "fullHeal",
    "give_exp_levels" to "xpLevel",
    "give_item" to "giveItem",
    "spawn" to "houseSpawn",
    "kill" to "kill",
    "parkour_checkpoint" to "parkCheck",
    "pause" to "pause",
    "play_sound" to "sound",
    // "random_action",
    "send_message" to "chat",
    "reset_inventory" to "resetInventory",
    "remove_item" to "removeItem",
    "set_player_team" to "setTeam",
    "use_held_item" to "consumeItem",
    "set_gamemode" to "gamemode",
    "set_compass_target" to "compassTarget",
    "teleport_player" to "tp",
    "send_to_lobby" to "lobby",
    "change_velocity" to "setVelocity",
    "launch_to_target" to "launchTarget",
    "drop_item" to "dropItem",
)

private val conditionMap = mapOf(
    "in_group" to "hasGroup",
    "has_permission" to "hasPermission",
    "in_region" to "inRegion",
    "has_item" to "hasItem",
    "in_parkour" to "doingParkour",
    "potion_effect" to "hasPotion",
    "sneaking" to "isSneaking",
    "flying" to "isFlying",
    "gamemode" to "gamemode",
    "in_team" to "hasTeam",
    "pvp_enabled" to "canPvp",
    "fishing_environment" to "fishingEnv",
    "portal_type" to "portal",
    "damage_cause" to "damageCause",
    "block_type" to "blockType",
    "is_item" to "isItem"
)

private val statOpMap = mapOf(
    StatOp.Set to "=",
    StatOp.Inc to "+=",
    StatOp.Dec to "-=",
    StatOp.Mul to "*=",
    StatOp.Div to "/=",
)

private val comparisonMap = mapOf(
    Comparison.Eq to "==",
    Comparison.Lt to "<",
    Comparison.Le to "<=",
    Comparison.Gt to ">",
    Comparison.Ge to ">="
)

private fun statVal(amount: StatValue): String {
    return when (amount) {
        is StatValue.I64 -> amount.value.toString()
        is StatValue.Str -> "\"${amount.value}\""
    }
}

fun generateHtsl(sess: CompileSess, function: Function): String {
    return function.actions.joinToString("\n") { generateHtslAction(sess, it) }
}

private fun generateHtslAction(sess: CompileSess, action: Action): String {
    val sb = StringBuilder()
    when (action) {
        is Action.Conditional -> {
            if (action.matchAnyCondition) sb.append("if or (")
            else sb.append("if and (")
            sb.append(action.conditions.joinToString { generateHtslCondition(sess, it) })
            sb.append(") {\n")
            action.ifActions.forEach { // we desire a hanging \n, no joinToString
                sb.append(generateHtslAction(sess, it)).append("\n")
            }
            sb.append("}")
            if (action.elseActions.isNotEmpty()) {
                sb.append(" else {\n")
                sb.append(action.elseActions.joinToString("\n") { generateHtslAction(sess, it) })
                sb.append("\n}")
            }
        }
        is Action.RandomAction -> {
            sb.append("random {\n")
            action.actions.forEach {
                sb.append(generateHtslAction(sess, it)).append("\n")
            }
            sb.append("}")
        }
        is Action.ChangePlayerStat -> {
            sb.append("stat ${action.stat} ${statOpMap[action.op]} ${statVal(action.amount)}")
        }
        is Action.ChangeGlobalStat -> {
            sb.append("globalstat ${action.stat} ${statOpMap[action.op]} ${statVal(action.amount)}")
        }
        is Action.ChangeTeamStat -> {
            sb.append("teamstat ${action.stat} ${action.team} ${statOpMap[action.op]} ${statVal(action.amount)}")
        }
        is Action.ExecuteFunction -> {
            sb.append("function \"${action.name}\" ${action.global}")
        }
        else -> {
            sb.append(actionMap[action.actionName.lowercase()])
            val encoder = HtslEncoder(sess)
            encoder.encodeSerializableValue(Action.serializer(), action)
            encoder.list.drop(1).forEach { sb.append(" $it") }
        }
    }
    return sb.toString()
}

private fun generateHtslCondition(sess: CompileSess, condition: Condition): String {
    val sb = StringBuilder()
    if (condition.inverted) sb.append("!")
    when (condition) {
        is Condition.PlayerStatRequirement -> {
            sb.append("stat ${condition.stat} ${comparisonMap[condition.op]} ${statVal(condition.value)}")
        }
        is Condition.GlobalStatRequirement -> {
            sb.append("globalstat ${condition.stat} ${comparisonMap[condition.op]} ${statVal(condition.value)}")
        }
        is Condition.TeamStatRequirement -> {
            sb.append("teamstat ${condition.stat} ${condition.team} ${comparisonMap[condition.op]} ${statVal(condition.value)}")
        }
        is Condition.RequiredHealth -> {
            sb.append("health ${comparisonMap[condition.mode]} ${statVal(condition.amount)}")
        }
        is Condition.RequiredHungerLevel -> {
            sb.append("hunger ${comparisonMap[condition.mode]} ${statVal(condition.amount)}")
        }
        is Condition.RequiredMaxHealth -> {
            sb.append("maxHealth ${comparisonMap[condition.mode]} ${statVal(condition.amount)}")
        }
        is Condition.RequiredDamageAmount -> {
            sb.append("damageAmount ${comparisonMap[condition.mode]} ${statVal(condition.amount)}")
        }
        is Condition.RequiredPlaceholderNumber -> {
            sb.append("placeholder ${condition.placeholder} ${comparisonMap[condition.mode]} ${statVal(condition.amount)}")
        }
        else -> {
            sb.append(conditionMap[condition.conditionName.lowercase()])
            val encoder = HtslEncoder(sess)
            encoder.encodeSerializableValue(Condition.serializer(), condition)
            encoder.list.drop(1).forEach { sb.append(" $it") }
        }
    }
    return sb.toString()
}

@OptIn(ExperimentalSerializationApi::class)
private class HtslEncoder(val sess: CompileSess) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    val list: MutableList<String> = mutableListOf()

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (value is ItemStack) {
            encodeString(value.name!!)
        }
        else if (value is Location) {
            when (value) {
                is Location.Custom -> {
                    fun rel(bool: Boolean) = if (bool) "~" else ""
                    fun nil(any: Any?) = if (any != null) any else ""
                    encodeString("custom_coordinates")
                    encodeString("${rel(value.relX)}${nil(value.x)}" +
                            " ${rel(value.relY)}${nil(value.y)}" +
                            " ${rel(value.relZ)}${nil(value.z)}" +
                            if (value.pitch == null) "" else " ${value.pitch} ${value.yaw}")
                }
                is Location.CurrentLocation -> encodeString("current_location")
                is Location.InvokersLocation -> encodeString("invokers_location")
                is Location.HouseSpawn -> encodeString("house_spawn")
            }
        }
        else {
            super.encodeSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun encodeBoolean(value: Boolean) { list.add(value.toString()) }
    override fun encodeInt(value: Int) { list.add(value.toString()) }
    override fun encodeLong(value: Long) { list.add(value.toString()) }
    override fun encodeFloat(value: Float) { list.add(value.toString()) }
    override fun encodeDouble(value: Double) { list.add(value.toString()) }
    override fun encodeString(value: String) { list.add("\"$value\"") }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        list.add("\"" + enumDescriptor.getElementName(index).lowercase() + "\"")
    }

}