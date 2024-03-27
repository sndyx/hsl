package com.hsc.compiler.ir.action

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
sealed class Condition {

    @Serializable
    data class HasPotionEffect(
        val effect: PotionEffect
    ) : Condition()
    @Serializable
    data object DoingParkour : Condition()
    @Serializable
    data class HasItem(
        val item: ItemStack,
        @SerialName("whereToCheck") val location: InventoryLocation,
        @Serializable(with = CheckNbtSerializer::class) @SerialName("whatToCheck") val checkNbt: Boolean,
        @SerialName("requireAmount") val checkAmount: Boolean
    ) : Condition()
    @Serializable
    data class IsItem(
        val item: ItemStack,
        @SerialName("whereToCheck") val location: InventoryLocation,
        @Serializable(with = CheckNbtSerializer::class) @SerialName("whatToCheck") val checkNbt: Boolean,
        @SerialName("requireAmount") val checkAmount: Boolean
    ) : Condition()
    data class WithinRegion(val region: String) : Condition()
    data class RequiredPermission(val permission: String) : Condition()
    data class PlayerStatRequirement(val stat: String, val op: Comparator, val value: String) : Condition()
    data class GlobalStatRequirement(val stat: String, val op: Comparator, val value: String) : Condition()
    data class TeamStatRequirement(val stat: String, val op: Comparator, val value: String) : Condition()
    data class RequiredGroup(val group: String) : Condition()
    data class DamageCause(val type: com.hsc.compiler.ir.action.DamageCause) : Condition()
    data class BlockType(val block: ItemStack, val matchNbt: Boolean) : Condition()
    data class PortalType(val type: com.hsc.compiler.ir.action.PortalType) : Condition()
    data class DamageAmount(val op: Comparator, val value: String) : Condition()
    data class FishingEnvironment(val type: com.hsc.compiler.ir.action.FishingEnvironment) : Condition()
    data class PlaceholderNumberRequirement(
        val placeholder: String,
        val op: Comparator,
        val value: String
    ) : Condition()
    data class PlayerHealth(val op: Comparator, val value: String) : Condition()
    data class PlayerHunger(val op: Comparator, val value: String) : Condition()
    data class PlayerMaxHealth(val op: Comparator, val value: String) : Condition()
    data class RequiredTeam(val team: String) : Condition()
    data object PlayerSneaking : Condition()
    data object PlayerFlying : Condition()
    data object PvpEnabled : Condition()
    data class RequiredGamemode(val gamemode: Gamemode) : Condition()

}

object CheckNbtSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "com.hsc.ir.action.CheckNbtSerializer",
        PrimitiveKind.STRING
    )
    override fun serialize(encoder: Encoder, value: Boolean) {
        val string = if (value) "item_metadata"
        else "item_type"
        encoder.encodeString(string)
    }
    override fun deserialize(decoder: Decoder): Boolean {
        val string = decoder.decodeString()
        return string != "item_type"
    }
}

enum class Gamemode {
    Adventure,
    Survival,
    Creative;
}

enum class DamageCause {
    Entity,
    Poison;
}

enum class PortalType {
    Nether,
    End;
}

enum class FishingEnvironment {
    Water,
    Lava;
}

enum class Comparator {
    Eq,
    Gt,
    Ge,
    Lt,
    Le;
}

enum class InventoryLocation {
    Anywhere,
    Hand,
    Armor,
    Hotbar,
    Inventory;
}