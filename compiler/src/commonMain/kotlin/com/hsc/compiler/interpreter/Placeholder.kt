package com.hsc.compiler.interpreter

import com.hsc.compiler.VERSION
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random

fun Player.placeholderString(placeholder: String): String {
    val args = placeholder.substringAfter('/').split(" ")
    return when (placeholder.substringBefore('/')) {
        "server.name" -> "miniXYZ"
        "server.shortname" -> "mXYZ"
        "player.name" -> name
        "player.ping" -> "100ms"
        "player.health" -> "$health"
        "player.maxhealth" -> "$maxHealth"
        "player.hunger" -> "$hunger"
        "player.experience" -> "$experience"
        "player.level" -> "$level"
        "player.version" -> "1.8.X"
        "player.protocol" -> "47"
        "player.gamemode" -> gamemode.string
        "player.region.name" -> "None"
        "player.location.x" -> "${x.toInt()}"
        "player.location.y" -> "${y.toInt()}"
        "player.location.z" -> "${z.toInt()}"
        "player.location.pitch" -> "${pitch.toInt()}"
        "player.location.yaw" -> "${yaw.toInt()}"
        "player.group.name" -> group ?: "Guest"
        "player.group.tag" -> "[${group?.uppercase() ?: "GUEST"}]"
        "player.group.priority" -> "0"
        "player.group.color" -> ""
        "player.team.name" -> team ?: "No Team"
        "player.team.tag" -> "[${team?.uppercase() ?: ""}]"
        "player.team.color" -> ""
        "player.team.players" -> VirtualHousing.players.count { it.team == args[0] }.toString()
        "house.name" -> "HSL Runtime v${VERSION}"
        "house.guests" -> VirtualHousing.players.size.toString()
        "house.cookies" -> "0"
        "house.visitingrules" -> "PRIVATE"
        "house.players" -> VirtualHousing.players.size.toString()
        "date.day" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).date.dayOfMonth.toString()
        "date.month" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).date.monthNumber.toString()
        "date.year" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).date.year.toString()
        "date.hour" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).time.hour.toString()
        "date.minute" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).time.hour.toString()
        "date.seconds" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).time.second.toString()
        "date.unix" -> Clock.System.now().epochSeconds.toString()
        "random.int" -> Random.nextInt(args[0].toInt(), args[1].toInt()).toString()
        "stat.player" -> stats[args[0]]?.toString() ?: "0"
        "stat.global" -> VirtualHousing.globalStats[args[0]]?.toString() ?: "0"
        "stat.team" -> VirtualHousing.teamStats[args[1]]?.get(args[0])?.toString() ?: "0"
        else -> "%$placeholder%"
    }
}

fun Player.placeholderValue(placeholder: String): Long {
    val args = placeholder.substringAfter('/').split(" ")
    return when (placeholder.substringBefore('/')) {
        "player.ping" -> 100L
        "player.health" -> health.toLong()
        "player.maxhealth" -> maxHealth.toLong()
        "player.hunger" -> hunger.toLong()
        "player.experience" -> experience.toLong()
        "player.level" -> level.toLong()
        "player.protocol" -> 47L
        "player.location.x" -> x.toLong()
        "player.location.y" -> y.toLong()
        "player.location.z" -> z.toLong()
        "player.location.pitch" -> pitch.toLong()
        "player.location.yaw" -> yaw.toLong()
        "player.group.priority" -> 0L
        "player.team.players" -> VirtualHousing.players.count { it.team == args[0] }.toLong()
        "house.guests" -> VirtualHousing.players.size.toLong()
        "house.cookies" -> 0L
        "house.players" -> VirtualHousing.players.size.toLong()
        "date.day" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).date.dayOfMonth.toLong()
        "date.month" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).date.monthNumber.toLong()
        "date.year" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).date.year.toLong()
        "date.hour" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).time.hour.toLong()
        "date.minute" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).time.hour.toLong()
        "date.seconds" -> Clock.System.now().toLocalDateTime(TimeZone.of(args[0])).time.second.toLong()
        "date.unix" -> Clock.System.now().epochSeconds
        "random.int" -> Random.nextInt(args[0].toInt(), args[1].toInt()).toLong()
        "stat.player" -> stats[args[0]]?.toLong() ?: 0L
        "stat.global" -> VirtualHousing.globalStats[args[0]]?.toLong() ?: 0L
        "stat.team" -> VirtualHousing.teamStats[args[1]]?.get(args[0])?.toLong() ?: 0L
        else -> 0L
    }
}