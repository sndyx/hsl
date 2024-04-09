package com.hsc.compiler.pretty

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.ir.action.Action

val actionMap = mapOf(
    Action.ApplyInventoryLayout::class to "apply inventory layout",
    Action.ApplyPotionEffect::class to "apply potion effect",
    Action.BalancePlayerTeam::class to "balance player team",
    Action.CancelEvent::class to "cancel event",
    Action.ChangeGlobalStat::class to "change global stat",
    Action.ChangeHealth::class to "change health",
    Action.ChangeHungerLevel::class to "change hunger level",
    Action.ChangeMaxHealth::class to "change max health",
    Action.ChangePlayerGroup::class to "change player group",
    Action.ChangePlayerStat::class to "change player stat",
    Action.Conditional::class to "conditional",
    Action.ExecuteFunction::class to "execute function",
    Action.SendMessage::class to "send message",
    Action.FailParkour::class to "fail parkour",
)

fun prettyPrintActions(functions: List<com.hsc.compiler.ir.action.Function>) {
    val t = Terminal(ansiLevel = AnsiLevel.ANSI256)
    functions.forEach {
        t.println("${blue("function")} ${bold(it.name)}")
        it.actions.forEach { action ->
            t.print("  ")
            t.println((actionMap[action::class]!!))
        }
        t.println()
    }
}