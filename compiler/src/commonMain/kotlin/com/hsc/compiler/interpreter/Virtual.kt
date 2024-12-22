package com.hsc.compiler.interpreter

import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.ir.ast.Item

class VirtualHousing {

    val functions: MutableList<Item> = mutableListOf()

    val players: MutableList<Player> = mutableListOf()

    val globalStats: MutableMap<String, Long> = mutableMapOf()
    val teamStats: MutableMap<String, MutableMap<String, Long>> = mutableMapOf()

    var terminal: Terminal? = null

    fun tick() {
        for (player in players.shuffled()) {
            player.tick()
        }
    }

    fun isActive(): Boolean = players.any { it.scheduled.isNotEmpty() }

}