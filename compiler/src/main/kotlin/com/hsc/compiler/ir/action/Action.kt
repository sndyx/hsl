package com.hsc.compiler.ir.action

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlinx.serialization.encoding.encodeStructure

@Serializable(with = ActionSerializer::class)
sealed class Action {

    @Serializable
    data class ApplyInventoryLayout(val layout: String) : Action()
    @Serializable
    data class ApplyPotionEffect(
        val effect: PotionEffect,
        val duration: Int,
        val amplifier: Int,
        @SerialName("overrideExistingEffects")
        val override: Boolean
    ) : Action()
    @Serializable
    data object BalancePlayerTeam : Action()
    @Serializable
    data object CancelEvent : Action()
    @Serializable
    data class ChangeGlobalStat(
        val name: String,
        @SerialName("mode") val op: StatOp,
        val value: String
    ) : Action()
    @Serializable
    data class ChangeHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String
    ) : Action()
    @Serializable
    data class ChangeHungerLevel(
        @SerialName("mode") val op: StatOp,
        @SerialName("hunger") val value: String
    ) : Action()
    @Serializable
    data class ChangeMaxHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("health") val value: String,
        @SerialName("healOnChange") val heal: Boolean
    ) : Action()
    data class ChangePlayerGroup(val group: String, val protectDemotion: Boolean) : Action()
    @Serializable
    data class ChangePlayerStat(
        val name: String,
        @SerialName("mode") val op: StatOp,
        val value: String
    ) : Action()
    @Serializable
    data class ChangeTeamStat(
        val name: String,
        @SerialName("mode") val op: StatOp,
        val value: String
    ) : Action()
    @Serializable
    data object ClearAllPotionEffects : Action()
    @Serializable
    data object CloseMenu : Action()
    @Serializable
    data class Conditional(
        val conditions: List<Condition>,
        val matchAnyCondition: Boolean,
        @SerialName("if") val ifActions: List<Action>,
        @SerialName("else") val elseActions: List<Action>,
    ) : Action()
    @Serializable
    data class DisplayActionBar(val message: String) : Action()
    @Serializable
    data class DisplayMenu(val menu: String) : Action()
    @Serializable
    data class DisplayTitle(
        val title: String,
        val subtitle: String,
        val fadeIn: Int,
        val stay: Int,
        val fadeOut: Int,
    ) : Action()
    @Serializable
    data class EnchantHeldItem(
        val enchantment: Enchantment,
        val level: Int,
    ) : Action()
    @Serializable
    data object Exit : Action()
    @Serializable
    data class FailParkour(val reason: String) : Action()
    @Serializable
    data object FullHeal : Action()
    @Serializable
    data class GiveExperienceLevels(val levels: Int) : Action()
    @Serializable
    data class GiveItem(
        val item: ItemStack,
        val allowMultiple: Boolean,
        val inventorySlot: Int,
        val replaceExistingItem: Boolean,
    )
    @Serializable
    data object GoToHouseSpawn : Action()
    @Serializable
    data object KillPlayer : Action()
    @Serializable
    data object ParkourCheckpoint : Action()
    @Serializable
    data class PauseExecution(val ticks: Int) : Action()
    @Serializable
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
    data class RandomAction(
        val actions: List<Action>,
    )
    @Serializable
    data class SendMessage(val message: String) : Action()
    @Serializable
    data class ExecuteFunction(val name: String, val global: Boolean) : Action()

}

val serialMap = mapOf(
    "apply_inventory_layout" to Action.ApplyInventoryLayout::class,
    "apply_potion_effect" to Action.ApplyPotionEffect::class,
    "balance_player_team" to Action.BalancePlayerTeam::class,
    "cancel_event" to Action.CancelEvent::class,
    "change_global_stat" to Action.ChangeGlobalStat::class,
    "change_health" to Action.ChangeHealth::class,
    "change_hunger_level" to Action.ChangeHungerLevel::class,
    "change_max_health" to Action.ChangeMaxHealth::class,
    "change_player_group" to Action.ChangePlayerGroup::class,
    "change_player_stat" to Action.ChangePlayerStat::class,
    "execute_function" to Action.ExecuteFunction::class,
)

@ExperimentalSerializationApi
object ActionSerialDescriptor : SerialDescriptor {
    override val elementsCount: Int = 2
    override val kind: SerialKind = StructureKind.LIST
    override val serialName: String = "Action"

    override fun getElementAnnotations(index: Int): List<Annotation> = emptyList()

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return if (index == 0) String.serializer().descriptor
        else Action.serializer().descriptor // This is definitely not right, but it works.
    }

    override fun getElementIndex(name: String): Int = name.toInt()
    override fun getElementName(index: Int): String = index.toString()
    override fun isElementOptional(index: Int): Boolean = false

}

object ActionSerializer : KSerializer<Action> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = ActionSerialDescriptor

    @OptIn(InternalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Action {
        val name = decoder.decodeString()
        return serialMap[name]!!.serializer().deserialize(decoder)
    }

    override fun serialize(encoder: Encoder, value: Action) {
        val entry = serialMap.entries.find { it.value.isInstance(value) }!!
        encoder.encodeCollection(descriptor, 2) {
            encodeStringElement(String.serializer().descriptor, 0, entry.key)
            val serializer = serializer(entry.value.javaObjectType)
            encodeSerializableElement(serializer.descriptor, 1, serializer, value)
        }
    }


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