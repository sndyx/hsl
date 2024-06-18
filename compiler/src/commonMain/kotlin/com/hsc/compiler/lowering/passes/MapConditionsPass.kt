package com.hsc.compiler.lowering.passes

import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.*
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.ArgParser
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.similar

object MapConditionsPass : AstPass {

    override fun run(ctx: LoweringCtx) {
        val visitor = MapConditionsVisitor(ctx)
        ctx.query<Expr>().forEach {
            visitor.visitExpr(it)
        }
    }

}

private class MapConditionsVisitor(val ctx: LoweringCtx) : AstVisitor {

    lateinit var p: ArgParser

    override fun visitExpr(expr: Expr) {
        when (val kind = expr.kind) {
            is ExprKind.Call -> {
                if (kind.ident.isGlobal || !Condition.builtins.contains(kind.ident.name)) return
                p = ArgParser(ctx, kind.args)
                val condition = when (kind.ident.name) {
                    "in_group" -> parseInGroup()
                    "in_team" -> parseInTeam()
                    "has_permission" -> parseHasPermission()
                    "in_region" -> parseInRegion()
                    "has_item" -> parseHasItem()
                    "in_parkour" -> Condition.InParkour
                    "required_effect" -> parseRequiredEffect()
                    "sneaking" -> Condition.PlayerSneaking
                    "flying" -> Condition.PlayerFlying
                    "gamemode" -> parseGamemode()
                    else -> null
                }
                if (condition != null) {
                    expr.kind = ExprKind.Condition(condition)
                }
            }
            else -> {}
        }
    }

    fun parseInGroup(): Condition {
        p.assertLength(1, "in_group(<group>)")
        val group = p.nextStringLit()
        val includeHigherGroups = p.nextBooleanLit()
        return Condition.RequiredGroup(
            group, includeHigherGroups
        )
    }

    fun parseInTeam(): Condition {
        p.assertLength(1, "in_team(<team>)")
        val team = p.nextStringLit()
        return Condition.RequiredTeam(team)
    }

    fun parseHasPermission(): Condition {
        p.assertLength(1, "has_permission(<permission>)")
        val permissionString = p.nextStringLit()
        val permission = Permission.entries.find { it.key.lowercase() == permissionString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid permission `$permissionString`", p.args.span)
            val options = Permission.entries.map { it.key }
            similar(permissionString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        return Condition.HasPermission(permission)
    }

    fun parseInRegion(): Condition {
        p.assertLength(1, "in_region(<region>)")
        val region = p.nextStringLit()
        return Condition.InRegion(region)
    }

    fun parseHasItem(): Condition {
        p.assertLength(4, "has_item(<item>, <what_to_check>, <where_to_check>, <required_amount>)")
        val item = p.nextItemLit()
        val whatToCheckString = p.nextStringLit()
        val whatToCheck = ItemCheck.entries.find { it.key.lowercase() == whatToCheckString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid item check `$whatToCheckString`", p.args.span)
            val options = ItemCheck.entries.map { it.key }
            similar(whatToCheckString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        val whereToCheckString = p.nextStringLit()
        val whereToCheck = InventoryLocation.entries.find { it.key.lowercase() == whereToCheckString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid inventory location `$whereToCheckString`", p.args.span)
            val options = InventoryLocation.entries.map { it.key }
            similar(whereToCheckString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        val requiredAmountString = p.nextStringLit()
        val requiredAmount = ItemAmount.entries.find { it.key.lowercase() == requiredAmountString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid amount `$requiredAmountString`", p.args.span)
            val options = ItemAmount.entries.map { it.key }
            similar(requiredAmountString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        return Condition.HasItem(item, whatToCheck, whereToCheck, requiredAmount)
    }

    fun parseRequiredEffect(): Condition {
        p.assertLength(1, "required_effect(<effect>)")
        val effectString = p.nextStringLit()
        val effect = PotionEffect.entries.find { it.key.lowercase() == effectString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid effect `$effectString`", p.args.span)
            val options = PotionEffect.entries.map { it.key }
            similar(effectString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        return Condition.RequiredEffect(effect)
    }

    fun parseGamemode(): Condition {
        p.assertLength(1, "gamemode(<gamemode>)")
        val gamemodeString = p.nextStringLit()
        val gamemode = GameMode.entries.find { it.key.lowercase() == gamemodeString.lowercase() } ?: run {
            val err = ctx.dcx().err("invalid gamemode `$gamemodeString`", p.args.span)
            val options = GameMode.entries.map { it.key }
            similar(gamemodeString, options).forEach {
                err.note(Level.Hint, "did you mean `$it`?")
            }
            throw err
        }
        return Condition.RequiredGameMode(gamemode)
    }

    /**
     *     @Serializable
     *     @SerialName("PLACEHOLDER_NUMBER")
     *     data class RequiredPlaceholderNumber(
     *         val placeholder: String,
     *         val mode: Comparison,
     *         val amount: StatValue,
     *     )
     *     @Serializable
     *     @SerialName("IN_TEAM")
     *     data class RequiredTeam(
     *         @SerialName("required_team")
     *         val team: String,
     *     )
     *     @Serializable
     *     @SerialName("PVP_ENABLED")
     *     data object PvpEnabled : Condition()
     *     @Serializable
     *     @SerialName("FISHING_ENVIRONMENT")
     *     data class FishingEnvironment(
     *         val environment: com.hsc.compiler.ir.action.FishingEnvironment
     *     ) : Condition()
     *     @Serializable
     *     @SerialName("PORTAL_TYPE")
     *     data class PortalType(
     *         @SerialName("portal_type")
     *         val type: com.hsc.compiler.ir.action.PortalType
     *     )
     *     @Serializable
     *     @SerialName("DAMAGE_CAUSE")
     *     data class DamageCause(
     *         val cause: com.hsc.compiler.ir.action.DamageCause
     *     )
     *     @Serializable
     *     @SerialName("DAMAGE_AMOUNT")
     *     data class RequiredDamageAmount(
     *         val mode: Comparison,
     *         val amount: StatValue,
     *     ) : Condition()
     *     @Serializable
     *     @SerialName("BLOCK_TYPE")
     *     data class BlockType(
     *         val item: ItemStack,
     *         @SerialName("match_type_only")
     *         val matchTypeOnly: Boolean,
     *     )
     *     @Serializable
     *     @SerialName("IS_ITEM")
     *     data class IsItem(
     *         val item: ItemStack,
     *         @SerialName("what_to_check") val whatToCheck: ItemCheck,
     *         @SerialName("where_to_check") val whereToCheck: InventoryLocation,
     *         @SerialName("required_amount") val amount: ItemAmount,
     *     ) : Condition()
     * }
     */

}