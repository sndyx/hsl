package com.hsc.compiler.interpreter

import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.ir.ast.Ident
import com.hsc.compiler.ir.ast.Stmt

data class Player(
    val name: String,
    var gamemode: GameMode = GameMode.Survival,

    var x: Double = 0.0,
    var y: Double = 0.0,
    var z: Double = 0.0,
    var pitch: Float = 0.0f,
    var yaw: Float = 0.0f,

    var health: Int = 20,
    var maxHealth: Int = 20,
    var hunger: Int = 20,
    var experience: Int = 0,
    var level: Int = 0,

    var group: String? = null,
    var team: String? = null,

    val t: Terminal = VirtualHousing.terminal!!,

    val stats: MutableMap<String, Long> = mutableMapOf(),

    val cooldowns: MutableList<FunctionCooldown> = mutableListOf(),
    val scheduled: MutableList<ScheduledExecution> = mutableListOf(),
    val trace: MutableList<StackElement> = mutableListOf()
) {

    fun getStat(ident: Ident): Long {
        return when (ident) {
            is Ident.Player -> stats[ident.name] ?: 0L
            is Ident.Global -> VirtualHousing.globalStats[ident.name] ?: 0L
            is Ident.Team -> VirtualHousing.teamStats[ident.team]?.get(ident.name) ?: 0L
        }
    }

    fun setStat(ident: Ident, value: Long) {
        when (ident) {
            is Ident.Player -> stats[ident.name] = value
            is Ident.Global -> VirtualHousing.globalStats[ident.name] = value
            is Ident.Team -> VirtualHousing.teamStats.getOrPut(ident.team) { mutableMapOf() }[ident.name] = value
        }
    }

    fun schedule(delay: Long, stmts: List<Stmt>) {
        scheduled.add(ScheduledExecution(delay, stmts))
    }

    fun tick() {
        cooldowns.forEach {
            it.delay--
        }
        cooldowns.removeAll {
            it.delay == 0L
        }
        scheduled.shuffled().forEach {
            if (it.delay == 0L) {
                executeBlock(it.stmts)
                scheduled.remove(it)
            }
            it.delay -= 1
        }
    }

    enum class GameMode(val string: String) {
        Creative("CREATIVE"),
        Survival("SURVIVAL"),
        Adventure("ADVENTURE");
    }

}