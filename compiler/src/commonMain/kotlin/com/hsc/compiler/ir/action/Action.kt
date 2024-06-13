@file:Suppress("SERIALIZER_TYPE_INCOMPATIBLE")

package com.hsc.compiler.ir.action

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import net.benwoodworth.knbt.NbtCompound

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
        @SerialName("inventory_slot") val inventorySlot: StatValue,
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

interface Keyed {
    val key: String
}

object KeyedSerializer : KSerializer<Keyed> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Keyed", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Keyed { error("not implemented!") }

    override fun serialize(encoder: Encoder, value: Keyed) {
        encoder.encodeString(value.key)
    }
}

@Serializable(with = ItemStackSerializer::class)
data class ItemStack(
    val nbt: NbtCompound,
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

enum class PotionEffect(override val key: String) : Keyed {
    Strength("STRENGTH"),
    Regeneration("REGENERATION");
}

enum class Enchantment {
    @SerialName("protection")
    Protection;
}

enum class GameMode(override val key: String) : Keyed {
    Adventure("Adventure"),
    Survival("Survival"),
    Creative("Creative");
}

@Serializable(with = KeyedSerializer::class)
enum class Lobby(override val key: String) : Keyed {
    MainLobby("Main Lobby"),
    TournamentHall("Tournament Hall"),
    BlitzSG("Blitz SG"),
    TNTGames("The TNT Games"),
    MegaWalls("Mega Walls"),
    ArcadeGames("Arcade Games"),
    CopsAndCrims("Cops and Crims"),
    UHCChampions("UHC Champions"),
    Warlords("Warlords"),
    SmashHeroes("Smash Heroes"),
    Housing("Housing"),
    SkyWars("SkyWars"),
    SpeedUHC("Speed UHC"),
    ClassicGames("Classic Games"),
    Prototype("Prototype"),
    BedWars("Bed Wars"),
    MurderMystery("Murder Mystery"),
    BuildBattle("Build Battle"),
    Duels("Duels"),
    WoolWars("Wool Wars");
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
        encoder.encodeString("item")
        // JsonObject.serializer().serialize(encoder, value.nbt)
    }
}

// For the love of god, Kotlin will not choose a fucking polymorphic serializer
// for my sealed class (or tell me fucking why)!!!!! We have to do this garbage.
object StatValueBaseSerializer : KSerializer<StatValue> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("StatValueHellfireDespairPitsSerializer")

    override fun deserialize(decoder: Decoder): StatValue { error("not implemented!") }

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
    override fun deserialize(decoder: Decoder): StatValue.I64 { error("not implemented!") }
}

object StatStrSerializer : KSerializer<StatValue.Str> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor
    override fun serialize(encoder: Encoder, value: StatValue.Str) {
        encoder.encodeString(value.value)
    }
    override fun deserialize(decoder: Decoder): StatValue.Str { error("not implemented!") }
}

@Serializable(with = KeyedSerializer::class)
enum class Sound(override val key: String) : Keyed {
    AmbienceCave("Ambience Cave"),
    AmbienceRain("Ambience Rain"),
    AmbienceThunder("Ambience Thunder"),
    AnvilBreak("Anvil Break"),
    AnvilLand("Anvil Land"),
    AnvilUse("Anvil Use"),
    ArrowHit("Arrow Hit"),
    Burp("Burp"),
    ChestClose("Chest Close"),
    ChestOpen("Chest Open"),
    Click("Click"),
    DoorClose("Door Close"),
    DoorOpen("Door Open"),
    Drink("Drink"),
    Eat("Eat"),
    Explode("Explode"),
    FallBig("Fall Big"),
    FallSmall("Fall Small"),
    Fizz("Fizz"),
    Fuse("Fuse"),
    Glass("Glass"),
    HurtFlesh("Hurt Flesh"),
    ItemBreak("Item Break"),
    ItemPickup("Item Pickup"),
    LavaPop("Lava Pop"),
    LevelUp("Level Up"),
    NoteBass("Note Bass"),
    NotePiano("Note Piano"),
    NoteBassDrum("Note Bass Drum"),
    NoteSticks("Note Sticks"),
    NoteBassGuitar("Note Bass Guitar"),
    NoteSnareDrum("Note Snare Drum"),

    NotePling("Note Pling"),

    OrbPickup("Orb Pickup"),
    ShootArrow("Shoot Arrow"),
    Splash("Splash"),
    Swim("Swim"),
    WoodClick("Wood Click"),

    BatDeath("Bat Death"),
    BatHurt("Bat Hurt"),
    BatIdle("Bat Idle"),
    BatLoop("Bat Loop"),
    BatTakeoff("Bat Takeoff"),
    BlazeBreath("Blaze Breath"),
    BlazeDeath("Blaze Death"),
    BlazeHit("Blaze Hit"),
    CatHiss("Cat Hiss"),
    CatHit("Cat Hit"),
    CatMeow("Cat Meow"),
    CatPurr("Cat Purr"),
    CatPurreow("Cat Purreow"),
    ChickenIdle("Chicken Idle"),
    ChickenHurt("Chicken Hurt"),
    ChickenEggPop("Chicken Egg Pop"),
    ChickenWalk("Chicken Walk"),
    CowIdle("Cow Idle"),
    CowHurt("Cow Hurt"),
    CowWalk("Cow Walk"),
    CreeperHiss("Creeper Hiss"),
    CreeperDeath("Creeper Death"),
    EnderdragonDeath("Enderdragon Death"),
    EnderdragonGrowl("Enderdragon Growl"),
    EnderdragonHit("Enderdragon Hit"),
    EnderdragonWings("Enderdragon Wings"),
    EndermanDeath("Enderman Death"),
    EndermanHit("Enderman Hit"),
    EndermanIdle("Enderman Idle"),
    EndermanTeleport("Enderman Teleport"),
    EndermanScream("Enderman Scream"),
    EndermanStare("Enderman Stare"),

    GhastScream("Ghast Scream"),
    GhastScream2("Ghast Scream2"),
    GhastCharge("Ghast Charge"),
    GhastDeath("Ghast Death"),
    GhastFireball("Ghast Fireball"),
    GhastMoan("Ghast Moan"),

    GuardianHit("Guardian Hit"),
    GuardianIdle("Guardian Idle"),
    GuardianDeath("Guardian Death"),
    GuardianElderHit("Guardian Elder Hit"),
    GuardianElderIdle("Guardian Elder Idle"),
    GuardianElderDeath("Guardian Elder Death"),
    GuardianLandHit("Guardian Land Hit"),
    GuardianLandIdle("Guardian Land Idle"),
    GuardianLandDeath("Guardian Land Death"),
    GuardianCurse("Guardian Curse"),
    GuardianAttack("Guardian Attack"),
    GuardianFlop("Guardian Flop"),

    IrongolemDeath("Irongolem Death"),
    IrongolemHit("Irongolem Hit"),
    IrongolemThrow("Irongolem Throw"),
    IrongolemWalk("Irongolem Walk"),

    MagmacubeWalk("Magmacube Walk"),
    MagmacubeWalk2("Magmacube Walk2"),
    MagmacubeJump("Magmacube Jump"),

    PigIdle("Pig Idle"),
    PigDeath("Pig Death"),
    PigWalk("Pig Walk"),

    RabbitAmbient("Rabbit Ambient"),
    RabbitDeath("Rabbit Death"),
    RabbitHurt("Rabbit Hurt"),
    RabbitJump("Rabbit Jump"),

    SheepIdle("Sheep Idle"),
    SheepShear("Sheep Shear"),
    SheepWalk("Sheep Walk"),

    SilverfishHit("Silverfish Hit"),
    SilverfishKill("Silverfish Kill"),
    SilverfishIdle("Silverfish Idle"),
    SilverfishWalk("Silverfish Walk"),

    SkeletonIdle("Skeleton Idle"),
    SkeletonDeath("Skeleton Death"),
    SkeletonHurt("Skeleton Hurt"),
    SkeletonWalk("Skeleton Walk"),

    SlimeAttack("Slime Attack"),
    SlimeWalk("Slime Walk"),
    SlimeWalk2("Slime Walk2"),

    SpiderIdle("Spider Idle"),
    SpiderDeath("Spider Death"),
    SpiderWalk("Spider Walk"),

    WitherDeath("Wither Death"),
    WitherHurt("Wither Hurt"),
    WitherIdle("Wither Idle"),
    WitherShoot("Wither Shoot"),
    WitherSpawn("Wither Spawn"),

    WolfBark("Wolf Bark"),
    WolfDeath("Wolf Death"),
    WolfGrowl("Wolf Growl"),
    WolfHowl("Wolf Howl"),
    WolfHurt("Wolf Hurt"),
    WolfPant("Wolf Pant"),
    WolfShake("Wolf Shake"),
    WolfWalk("Wolf Walk"),
    WolfWhine("Wolf Whine"),

    ZombieMetal("Zombie Metal"),
    ZombieWood("Zombie Wood"),
    ZombieWoodbreak("Zombie Woodbreak"),
    ZombieIdle("Zombie Idle"),
    ZombieDeath("Zombie Death"),
    ZombieHurt("Zombie Hurt"),
    ZombieInfect("Zombie Infect"),
    ZombieUnfect("Zombie Unfect"),
    ZombieRemedy("Zombie Remedy"),
    ZombieWalk("Zombie Walk"),
    ZombiePigIdle("Zombie Pig Idle"),
    ZombiePigAngry("Zombie Pig Angry"),
    ZombiePigDeath("Zombie Pig Death"),
    ZombiePigHurt("Zombie Pig Hurt"),

    FireworkBlast("Firework Blast"),
    FireworkBlast2("Firework Blast2"),
    FireworkLargeBlast("Firework Large Blast"),
    FireworkLargeBlast2("Firework Large Blast2"),
    FireworkTwinkle("Firework Twinkle"),
    FireworkTwinkle2("Firework Twinkle2"),
    FireworkLaunch("Firework Launch"),

    FireworksBlast("Fireworks Blast"),
    FireworksBlast2("Fireworks Blast2"),
    FireworksLargeBlast("Fireworks Large Blast"),
    FireworksLargeBlast2("Fireworks Large Blast2"),
    FireworksTwinkle("Fireworks Twinkle"),
    FireworksTwinkle2("Fireworks Twinkle2"),
    FireworksLaunch("Fireworks Launch"),

    SuccessfulHit("Successful Hit"),

    HorseAngry("Horse Angry"),
    HorseArmor("Horse Armor"),
    HorseBreathe("Horse Breathe"),
    HorseDeath("Horse Death"),
    HorseGallop("Horse Gallop"),
    HorseHit("Horse Hit"),
    HorseIdle("Horse Idle"),
    HorseJump("Horse Jump"),
    HorseLand("Horse Land"),
    HorseSaddle("Horse Saddle"),
    HorseSoft("Horse Soft"),
    HorseWood("Horse Wood"),
    DonkeyAngry("Donkey Angry"),
    DonkeyDeath("Donkey Death"),
    DonkeyHit("Donkey Hit"),
    DonkeyIdle("Donkey Idle"),
    HorseSkeletonDeath("Horse Skeleton Death"),
    HorseSkeletonHit("Horse Skeleton Hit"),
    HorseSkeletonIdle("Horse Skeleton Idle"),
    HorseZombieDeath("Horse Zombie Death"),
    HorseZombieHit("Horse Zombie Hit"),
    HorseZombieIdle("Horse Zombie Idle"),

    VillagerDeath("Villager Death"),
    VillagerHaggle("Villager Haggle"),
    VillagerHit("Villager Hit"),
    VillagerIdle("Villager Idle"),
    VillagerNo("Villager No"),
    VillagerYes("Villager Yes");
}