@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package com.hsc.compiler.ir.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed class Condition(
    @Transient val conditionName: String = ""
) {

    var inverted = false

    companion object {
        val builtins = setOf(
            "in_group",
            "has_permission",
            "in_region",
            "has_item",
            "in_parkour",
            "potion_effect",
            "sneaking",
            "flying",
            "gamemode",
            "in_team",
            "pvp_enabled",
            "fishing_environment",
            "portal_type",
            "damage_cause",
            "block_type",
            "is_item"
        )
    }

    @Serializable
    @SerialName("IN_GROUP")
    data class RequiredGroup(
        @SerialName("required_group")
        val group: String,
        @SerialName("include_higher_groups")
        val includeHigherGroups: Boolean,
    ) : Condition("IN_GROUP")
    @Serializable
    @SerialName("PLAYER_STAT")
    data class PlayerStatRequirement(
        val stat: String,
        @SerialName("mode") val op: Comparison,
        val value: StatValue,
    ) : Condition("PLAYER_STAT")
    @Serializable
    @SerialName("GLOBAL_STAT")
    data class GlobalStatRequirement(
        val stat: String,
        @SerialName("mode") val op: Comparison,
        val value: StatValue,
    ) : Condition("GLOBAL_STAT")
    @Serializable
    @SerialName("TEAM_STAT")
    data class TeamStatRequirement(
        val stat: String,
        val team: String,
        @SerialName("mode") val op: Comparison,
        val value: StatValue,
    ) : Condition("TEAM_STAT")
    @Serializable
    @SerialName("HAS_PERMISSION")
    data class HasPermission(
        @SerialName("required_permission")
        val permission: Permission,
    ) : Condition("HAS_PERMISSION")
    @Serializable
    @SerialName("IN_REGION")
    data class InRegion(
        val region: String,
    ) : Condition("IN_REGION")
    @Serializable
    @SerialName("HAS_ITEM")
    data class HasItem(
        val item: ItemStack,
        @SerialName("what_to_check") val whatToCheck: ItemCheck,
        @SerialName("where_to_check") val whereToCheck: InventoryLocation,
        @SerialName("required_amount") val amount: ItemAmount,
    ) : Condition("HAS_ITEM")
    @Serializable
    @SerialName("IN_PARKOUR")
    data object InParkour : Condition("IN_PARKOUR")
    @Serializable
    @SerialName("POTION_EFFECT")
    data class RequiredEffect(
        val effect: PotionEffect,
    ) : Condition("POTION_EFFECT")
    @Serializable
    @SerialName("SNEAKING")
    data object PlayerSneaking : Condition("SNEAKING")
    @Serializable
    @SerialName("FLYING")
    data object PlayerFlying : Condition("FLYING")
    @Serializable
    @SerialName("HEALTH")
    data class RequiredHealth(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("HEALTH")
    @Serializable
    @SerialName("MAX_HEALTH")
    data class RequiredMaxHealth(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("MAX_HEALTH")
    @Serializable
    @SerialName("HUNGER_LEVEL")
    data class RequiredHungerLevel(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("HUNGER_LEVEL")
    @Serializable
    @SerialName("GAMEMODE")
    data class RequiredGameMode(
        @SerialName("required_gamemode")
        val gameMode: GameMode
    ) : Condition("GAMEMODE")
    @Serializable
    @SerialName("PLACEHOLDER_NUMBER")
    data class RequiredPlaceholderNumber(
        val placeholder: String,
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("PLACEHOLDER_NUMBER")
    @Serializable
    @SerialName("IN_TEAM")
    data class RequiredTeam(
        @SerialName("required_team")
        val team: String,
    ) : Condition("IN_TEAM")
    @Serializable
    @SerialName("PVP_ENABLED")
    data object PvpEnabled : Condition("PVP_ENABLED")
    @Serializable
    @SerialName("FISHING_ENVIRONMENT")
    data class FishingEnvironment(
        val environment: com.hsc.compiler.ir.action.FishingEnvironment
    ) : Condition("FISHING_ENVIRONMENT")
    @Serializable
    @SerialName("PORTAL_TYPE")
    data class PortalType(
        @SerialName("portal_type")
        val type: com.hsc.compiler.ir.action.PortalType
    ) : Condition("PORTAL_TYPE")
    @Serializable
    @SerialName("DAMAGE_CAUSE")
    data class DamageCause(
        val cause: com.hsc.compiler.ir.action.DamageCause
    ) : Condition("DAMAGE_CAUSE")
    @Serializable
    @SerialName("DAMAGE_AMOUNT")
    data class RequiredDamageAmount(
        val mode: Comparison,
        val amount: StatValue,
    ) : Condition("DAMAGE_AMOUNT")
    @Serializable
    @SerialName("BLOCK_TYPE")
    data class BlockType(
        val item: ItemStack,
        @SerialName("match_type_only")
        val matchTypeOnly: Boolean,
    ) : Condition("BLOCK_TYPE")
    @Serializable
    @SerialName("IS_ITEM")
    data class IsItem(
        val item: ItemStack,
        @SerialName("what_to_check") val whatToCheck: ItemCheck,
        @SerialName("where_to_check") val whereToCheck: InventoryLocation,
        @SerialName("required_amount") val amount: ItemAmount,
    ) : Condition("IS_ITEM")
}

enum class Comparison {
    @SerialName("EQUAL") Eq,
    @SerialName("GREATER_THAN") Gt,
    @SerialName("GREATER_THAN_OR_EQUAL") Ge,
    @SerialName("LESS_THAN") Lt,
    @SerialName("LESS_THAN_OR_EQUAL") Le;
}

@Serializable(with = KeyedSerializer::class)
enum class Permission(override val key: String) : Keyed {
    Fly("Fly"),
    WoodDoor("Wood Door"),
    IronDoor("Iron Door"),
    WoodTrapDoor("Wood Trap Door"),
    IronTrapDoor("Iron Trap Door"),
    FenceGate("Fence Gate"),
    Button("Button"),
    Lever("Lever"),
    UseLaunchPads("Use Launch Pads"),
    Tp("/tp"),
    TpOtherPlayers("/tp Other Players"),
    Jukebox("Jukebox"),
    Kick("Kick"),
    Ban("Ban"),
    Mute("Mute"),
    PetSpawning("Pet Spawning"),
    Build("Build"),
    OfflineBuild("Offline Build"),
    Fluid("Fluid"),
    ProTools("Pro Tools"),
    UseChests("Use Chests"),
    UseEnderChests("Use Ender Chests"),
    ItemEditor("Item Editor"),
    SwitchGameMode("Switch Game Mode"),
    EditStats("Edit Stats"),
    ChangePlayerGroup("Change Player Group"),
    ChangeGameRules("Change Gamerules"),
    HousingMenu("Housing Menu"),
    TeamChatSpy("Team Chat Spy"),
    EditActions("Edit Actions"),
    EditRegions("Edit Regions"),
    EditScoreboard("Edit Scoreboard"),
    EditEventActions("Edit Event Actions"),
    EditCommands("Edit Commands"),
    EditFunctions("Edit Functions"),
    EditInventoryLayouts("Edit Inventory Layouts"),
    EditTeams("Edit Teams"),
    EditCustomMenus("Edit Custom Menus"),
    ItemMailbox("Item: Mailbox"),
    ItemEggHunt("Item: Egg Hunt"),
    ItemTeleportPad("Item: Teleport Pad"),
    ItemLaunchPad("Item: Launch Pad"),
    ItemActionPad("Item: Action Pad"),
    ItemHologram("Item: Hologram"),
    ItemNPC("Item: NPCs"),
    ItemActionButton("Item: Action Button"),
    ItemLeaderboard("Item: Leaderboard"),
    ItemTrashCan("Item: Trash Can"),
    ItemBiomeStick("Item: Biome Stick");
}

@Serializable(with = KeyedSerializer::class)
enum class ItemCheck(override val key: String) : Keyed {
    ItemType("Item Type"),
    Metadata("Metadata");
}

@Serializable(with = KeyedSerializer::class)
enum class ItemAmount(override val key: String) : Keyed {
    Any("Any Amount"),
    Ge("Equal or Greater Amount");
}

@Serializable(with = KeyedSerializer::class)
enum class InventoryLocation(override val key: String) : Keyed {
    Hand("Hand"),
    Armor("Armor"),
    HotBar("Hotbar"),
    Inventory("Inventory"),
    Anywhere("Anywhere");
}

@Serializable(with = KeyedSerializer::class)
enum class FishingEnvironment(override val key: String) : Keyed {
    Water("Water"),
    Lava("Lava");
}

@Serializable(with = KeyedSerializer::class)
enum class PortalType(override val key: String) : Keyed {
    EndPortal("End Portal"),
    NetherPortal("Nether Portal")
}

@Serializable(with = KeyedSerializer::class)
enum class DamageCause(override val key: String) : Keyed {
    EntityAttack("Entity Attack"),
    Projectile("Projectile"),
    Suffocation("Suffocation"),
    Fall("Fall"),
    Lava("Lava"),
    Fire("Fire"),
    FireTick("Fire Tick"),
    Drowning("Drowning"),
    Starvation("Starvation"),
    Poison("Poison"),
    Thorns("Thorns");
}