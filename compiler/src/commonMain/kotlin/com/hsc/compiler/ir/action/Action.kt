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
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt

@Serializable
sealed class Action(
    @Transient val actionName: String = ""
) {

    companion object {
        val builtins = setOf(
            "set_layout",
            "effect",
            "balance_team",
            "cancel_event",
            "change_health",
            "change_hunger_level",
            "change_max_health",
            "set_group",
            "clear_effects",
            "close_menu",
            "action_bar",
            "open_menu",
            "title",
            "enchant_held_item",
            "exit",
            "fail_parkour",
            "heal",
            "give_exp_levels",
            "give_item",
            "spawn",
            "kill",
            "parkour_checkpoint",
            "pause",
            "sound",
            "message",
            "reset_inventory",
            "remove_item",
            "set_team",
            "remove_held_item",
            "set_gamemode",
            "set_compass_target",
            "tp",
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
        @SerialName("health") val value: StatValue
    ) : Action("CHANGE_HEALTH")
    @Serializable
    @SerialName("CHANGE_HUNGER_LEVEL")
    data class ChangeHungerLevel(
        @SerialName("mode") val op: StatOp,
        @SerialName("hunger") val value: StatValue
    ) : Action("CHANGE_HUNGER_LEVEL")
    @Serializable
    @SerialName("CHANGE_MAX_HEALTH")
    data class ChangeMaxHealth(
        @SerialName("mode") val op: StatOp,
        @SerialName("max_health") val value: StatValue,
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
        val volume: Double,
        val pitch: Double,
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

interface KeyedLabeled : Keyed {
    val label: String
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
    var name: String? = null,
)

@Serializable
sealed class Location() {

    @Serializable
    @SerialName("custom_coordinates")
    class Custom(
        val relX: Boolean,
        val relY: Boolean,
        val relZ: Boolean,
        val x: Double?,
        val y: Double?,
        val z: Double?,
        val pitch: Float?,
        val yaw: Float?,
    ) : Location()

    @Serializable
    @SerialName("house_spawn")
    object HouseSpawn : Location()

    @Serializable
    @SerialName("current_location")
    object CurrentLocation : Location()

    @Serializable
    @SerialName("invokers_location")
    object InvokersLocation : Location()

}

enum class PotionEffect(override val key: String) : Keyed {
    Speed("Speed"),
    Slowness("Slowness"),
    Haste("Haste"),
    MiningFatigue("Mining Fatigue"),
    Strength("Strength"),
    InstantHealth("Instant Health"),
    InstantDamage("Instant Damage"),
    JumpBoost("Jump Boost"),
    Nausea("Nausea"),
    Regeneration("Regeneration"),
    Resistance("Resistance"),
    FireResistance("Fire Resistance"),
    WaterBreathing("Water Breathing"),
    Invisibility("Invisibility"),
    Blindness("Blindness"),
    NightVision("Night Vision"),
    Hunger("Hunger"),
    Weakness("Weakness"),
    Poison("Poison"),
    Wither("Wither"),
    HealthBoost("Health Boost"),
    Absorption("Absorption");
}

enum class Enchantment(override val key: String) : Keyed {
    Protection("Protection"),
    FireProtection("Fire Protection"),
    FeatherFalling("Feather Falling"),
    BlastProtection("Blast Protection"),
    ProjectileProtection("Projectile Protection"),
    Respiration("Respiration"),
    AquaAffinity("Aqua Affinity"),
    Thorns("Thorns"),
    DepthStrider("Depth Strider"),
    Sharpness("Sharpness"),
    Smite("Smite"),
    BaneOfArthropods("Bane Of Arthropods"),
    Knockback("Knockback"),
    FireAspect("Fire Aspect"),
    Looting("Looting"),
    Efficiency("Efficiency"),
    SilkTouch("Silk Touch"),
    Unbreaking("Unbreaking"),
    Fortune("Fortune"),
    Power("Power"),
    Punch("Punch"),
    Flame("Flame"),
    Infinity("Infinity");
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
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ItemStack", PrimitiveKind.STRING)

    private val snbt = StringifiedNbt { }
    override fun deserialize(decoder: Decoder): ItemStack { error("not implemented!") }

    override fun serialize(encoder: Encoder, value: ItemStack) {
        encoder.encodeString(snbt.encodeToString(value.nbt))
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
enum class Sound(override val label: String, override val key: String) : KeyedLabeled {
    AmbienceCave("Ambience Cave", "ambient.cave.cave"),
    AmbienceRain("Ambience Rain", "ambient.weather.rain"),
    AmbienceThunder("Ambience Thunder", "ambient.weather.thunder"),
    AnvilBreak("Anvil Break", "random.anvil_break"),
    AnvilLand("Anvil Land", "random.anvil_land"),
    AnvilUse("Anvil Use", "random.anvil_use"),
    ArrowHit("Arrow Hit", "random.bowhit"),
    Burp("Burp", "random.burp"),
    ChestClose("Chest Close", "random.chestclosed"),
    ChestOpen("Chest Open", "random.chestopen"),
    Click("Click", "random.click"),
    DoorClose("Door Close", "random.door_close"),
    DoorOpen("Door Open", "random.door_open"),
    Drink("Drink", "random.drink"),
    Eat("Eat", "random.eat"),
    Explode("Explode", "random.explode"),
    FallBig("Fall Big", "game.player.hurt.fall.big"),
    FallSmall("Fall Small", "game.player.hurt.fall.small"),
    Fizz("Fizz", "random.fizz"),
    Fuse("Fuse", "game.tnt.primed"),
    Glass("Glass", "dig.glass"),
    HurtFlesh("Hurt Flesh", "game.player.hurt"),
    ItemBreak("Item Break", "random.break"),
    ItemPickup("Item Pickup", "random.pop"),
    LavaPop("Lava Pop", "liquid.lavapop"),
    LevelUp("Level Up", "random.levelup"),
    NoteBass("Note Bass", "note.bass"),
    NotePiano("Note Piano", "note.harp"),
    NoteBassDrum("Note Bass Drum", "note.bd"),
    NoteSticks("Note Sticks", "note.hat"),
    NoteBassGuitar("Note Bass Guitar", "note.bassattack"),
    NoteSnareDrum("Note Snare Drum", "note.snare"),
    NotePling("Note Pling", "note.pling"),
    OrbPickup("Orb Pickup", "random.orb"),
    ShootArrow("Shoot Arrow", "random.bow"),
    Splash("Splash", "game.player.swim.splash"),
    Swim("Swim", "game.player.swim"),
    WoodClick("Wood Click", "random.wood_click"),
    BatDeath("Bat Death", "mob.bat.death"),
    BatHurt("Bat Hurt", "mob.bat.hurt"),
    BatIdle("Bat Idle", "mob.bat.idle"),
    BatLoop("Bat Loop", "mob.bat.loop"),
    BatTakeoff("Bat Takeoff", "mob.bat.takeoff"),
    BlazeBreath("Blaze Breath", "mob.blaze.breathe"),
    BlazeDeath("Blaze Death", "mob.blaze.death"),
    BlazeHit("Blaze Hit", "mob.blaze.hit"),
    CatHiss("Cat Hiss", "mob.cat.hiss"),
    CatHit("Cat Hit", "mob.cat.hitt"),
    CatMeow("Cat Meow", "mob.cat.meow"),
    CatPurr("Cat Purr", "mob.cat.purr"),
    CatPurreow("Cat Purreow", "mob.cat.purreow"),
    ChickenIdle("Chicken Idle", "mob.chicken.say"),
    ChickenHurt("Chicken Hurt", "mob.chicken.hurt"),
    ChickenEggPop("Chicken Egg Pop", "mob.chicken.plop"),
    ChickenWalk("Chicken Walk", "mob.chicken.step"),
    CowIdle("Cow Idle", "mob.cow.say"),
    CowHurt("Cow Hurt", "mob.cow.hurt"),
    CowWalk("Cow Walk", "mob.cow.step"),
    CreeperHiss("Creeper Hiss", "mob.creeper.say"),
    CreeperDeath("Creeper Death", "mob.creeper.death"),
    EnderdragonDeath("Enderdragon Death", "mob.enderdragon.end"),
    EnderdragonGrowl("Enderdragon Growl", "mob.enderdragon.growl"),
    EnderdragonHit("Enderdragon Hit", "mob.enderdragon.hit"),
    EnderdragonWings("Enderdragon Wings", "mob.enderdragon.wings"),
    EndermanDeath("Enderman Death", "mob.endermen.death"),
    EndermanHit("Enderman Hit", "mob.endermen.hit"),
    EndermanIdle("Enderman Idle", "mob.endermen.idle"),
    EndermanTeleport("Enderman Teleport", "mob.endermen.portal"),
    EndermanScream("Enderman Scream", "mob.endermen.scream"),
    EndermanStare("Enderman Stare", "mob.endermen.stare"),
    GhastScream("Ghast Scream", "mob.ghast.scream"),
    GhastScream2("Ghast Scream2", "mob.ghast.affectionate_scream"),
    GhastCharge("Ghast Charge", "mob.ghast.charge"),
    GhastDeath("Ghast Death", "mob.ghast.death"),
    GhastFireball("Ghast Fireball", "mob.ghast.fireball"),
    GhastMoan("Ghast Moan", "mob.ghast.moan"),
    GuardianHit("Guardian Hit", "mob.guardian.hit"),
    GuardianIdle("Guardian Idle", "mob.guardian.idle"),
    GuardianDeath("Guardian Death", "mob.guardian.death"),
    GuardianElderHit("Guardian Elder Hit", "mob.guardian.elder.hit"),
    GuardianElderIdle("Guardian Elder Idle", "mob.guardian.elder.idle"),
    GuardianElderDeath("Guardian Elder Death", "mob.guardian.elder.death"),
    GuardianLandHit("Guardian Land Hit", "mob.guardian.land.hit"),
    GuardianLandIdle("Guardian Land Idle", "mob.guardian.land.idle"),
    GuardianLandDeath("Guardian Land Death", "mob.guardian.land.death"),
    GuardianCurse("Guardian Curse", "mob.guardian.curse"),
    GuardianAttack("Guardian Attack", "mob.guardian.attack"),
    GuardianFlop("Guardian Flop", "mob.guardian.flop"),
    IrongolemDeath("Irongolem Death", "mob.irongolem.death"),
    IrongolemHit("Irongolem Hit", "mob.irongolem.hit"),
    IrongolemThrow("Irongolem Throw", "mob.irongolem.throw"),
    IrongolemWalk("Irongolem Walk", "mob.irongolem.walk"),
    MagmacubeWalk("Magmacube Walk", "mob.magmacube.small"),
    MagmacubeWalk2("Magmacube Walk2", "mob.magmacube.big"),
    MagmacubeJump("Magmacube Jump", "mob.magmacube.jump"),
    PigIdle("Pig Idle", "mob.pig.say"),
    PigDeath("Pig Death", "mob.pig.death"),
    PigWalk("Pig Walk", "mob.pig.step"),
    RabbitAmbient("Rabbit Ambient", "mob.rabbit.idle"),
    RabbitDeath("Rabbit Death", "mob.rabbit.death"),
    RabbitHurt("Rabbit Hurt", "mob.rabbit.hurt"),
    RabbitJump("Rabbit Jump", "mob.rabbit.hop"),
    SheepIdle("Sheep Idle", "mob.sheep.say"),
    SheepShear("Sheep Shear", "mob.sheep.shear"),
    SheepWalk("Sheep Walk", "mob.sheep.step"),
    SilverfishHit("Silverfish Hit", "mob.silverfish.hit"),
    SilverfishKill("Silverfish Kill", "mob.silverfish.kill"),
    SilverfishIdle("Silverfish Idle", "mob.silverfish.say"),
    SilverfishWalk("Silverfish Walk", "mob.silverfish.step"),
    SkeletonIdle("Skeleton Idle", "mob.skeleton.say"),
    SkeletonDeath("Skeleton Death", "mob.skeleton.death"),
    SkeletonHurt("Skeleton Hurt", "mob.skeleton.hurt"),
    SkeletonWalk("Skeleton Walk", "mob.skeleton.step"),
    SlimeAttack("Slime Attack", "mob.slime.attack"),
    SlimeWalk("Slime Walk", "mob.slime.small"),
    SlimeWalk2("Slime Walk2", "mob.slime.big"),
    SpiderIdle("Spider Idle", "mob.spider.say"),
    SpiderDeath("Spider Death", "mob.spider.death"),
    SpiderWalk("Spider Walk", "mob.spider.step"),
    WitherDeath("Wither Death", "mob.wither.death"),
    WitherHurt("Wither Hurt", "mob.wither.hurt"),
    WitherIdle("Wither Idle", "mob.wither.idle"),
    WitherShoot("Wither Shoot", "mob.wither.shoot"),
    WitherSpawn("Wither Spawn", "mob.wither.spawn"),
    WolfBark("Wolf Bark", "mob.wolf.bark"),
    WolfDeath("Wolf Death", "mob.wolf.death"),
    WolfGrowl("Wolf Growl", "mob.wolf.growl"),
    WolfHowl("Wolf Howl", "mob.wolf.howl"),
    WolfHurt("Wolf Hurt", "mob.wolf.hurt"),
    WolfPant("Wolf Pant", "mob.wolf.panting"),
    WolfShake("Wolf Shake", "mob.wolf.shake"),
    WolfWalk("Wolf Walk", "mob.wolf.step"),
    WolfWhine("Wolf Whine", "mob.wolf.whine"),
    ZombieMetal("Zombie Metal", "mob.zombie.metal"),
    ZombieWood("Zombie Wood", "mob.zombie.wood"),
    ZombieWoodbreak("Zombie Woodbreak", "mob.zombie.woodbreak"),
    ZombieIdle("Zombie Idle", "mob.zombie.say"),
    ZombieDeath("Zombie Death", "mob.zombie.death"),
    ZombieHurt("Zombie Hurt", "mob.zombie.hurt"),
    ZombieInfect("Zombie Infect", "mob.zombie.infect"),
    ZombieUnfect("Zombie Unfect", "mob.zombie.unfect"),
    ZombieRemedy("Zombie Remedy", "mob.zombie.remedy"),
    ZombieWalk("Zombie Walk", "mob.zombie.step"),
    ZombiePigIdle("Zombie Pig Idle", "mob.zombiepig.zpig"),
    ZombiePigAngry("Zombie Pig Angry", "mob.zombiepig.zpigangry"),
    ZombiePigDeath("Zombie Pig Death", "mob.zombiepig.zpigdeath"),
    ZombiePigHurt("Zombie Pig Hurt", "mob.zombiepig.zpighurt"),
    FireworkBlast("Firework Blast", "fireworks.blast"),
    FireworkBlast2("Firework Blast2", "fireworks.blast_far"),
    FireworkLargeBlast("Firework Large Blast", "fireworks.largeBlast"),
    FireworkLargeBlast2("Firework Large Blast2", "fireworks.largeBlast_far"),
    FireworkTwinkle("Firework Twinkle", "fireworks.twinkle"),
    FireworkTwinkle2("Firework Twinkle2", "fireworks.twinkle_far"),
    FireworkLaunch("Firework Launch", "fireworks.launch"),
    FireworksBlast("Fireworks Blast", "fireworks.blast"),
    FireworksBlast2("Fireworks Blast2", "fireworks.blast_far"),
    FireworksLargeBlast("Fireworks Large Blast", "fireworks.largeBlast"),
    FireworksLargeBlast2("Fireworks Large Blast2", "fireworks.largeBlast_far"),
    FireworksTwinkle("Fireworks Twinkle", "fireworks.twinkle"),
    FireworksTwinkle2("Fireworks Twinkle2", "fireworks.twinkle_far"),
    FireworksLaunch("Fireworks Launch", "fireworks.launch"),
    SuccessfulHit("Successful Hit", "random.successful_hit"),
    HorseAngry("Horse Angry", "mob.horse.angry"),
    HorseArmor("Horse Armor", "mob.horse.armor"),
    HorseBreathe("Horse Breathe", "mob.horse.breathe"),
    HorseDeath("Horse Death", "mob.horse.death"),
    HorseGallop("Horse Gallop", "mob.horse.gallop"),
    HorseHit("Horse Hit", "mob.horse.hit"),
    HorseIdle("Horse Idle", "mob.horse.idle"),
    HorseJump("Horse Jump", "mob.horse.jump"),
    HorseLand("Horse Land", "mob.horse.land"),
    HorseSaddle("Horse Saddle", "mob.horse.leather"),
    HorseSoft("Horse Soft", "mob.horse.soft"),
    HorseWood("Horse Wood", "mob.horse.wood"),
    DonkeyAngry("Donkey Angry", "mob.horse.donkey.angry"),
    DonkeyDeath("Donkey Death", "mob.horse.donkey.death"),
    DonkeyHit("Donkey Hit", "mob.horse.donkey.hit"),
    DonkeyIdle("Donkey Idle", "mob.horse.donkey.idle"),
    HorseSkeletonDeath("Horse Skeleton Death", "mob.horse.skeleton.death"),
    HorseSkeletonHit("Horse Skeleton Hit", "mob.horse.skeleton.hit"),
    HorseSkeletonIdle("Horse Skeleton Idle", "mob.horse.skeleton.idle"),
    HorseZombieDeath("Horse Zombie Death", "mob.horse.zombie.death"),
    HorseZombieHit("Horse Zombie Hit", "mob.horse.zombie.hit"),
    HorseZombieIdle("Horse Zombie Idle", "mob.horse.zombie.idle"),
    VillagerDeath("Villager Death", "mob.villager.death"),
    VillagerHaggle("Villager Haggle", "mob.villager.haggle"),
    VillagerHit("Villager Hit", "mob.villager.hit"),
    VillagerIdle("Villager Idle", "mob.villager.idle"),
    VillagerNo("Villager No", "mob.villager.no"),
    VillagerYes("Villager Yes", "mob.villager.yes");
}