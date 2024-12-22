package com.hsc.compiler.interpreter

import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyle
import com.hsc.compiler.ir.action.Sound

fun Player.logMessage(message: String) {
    val formatted = formatText(message.replace(Regex("%([^%]+)%")) { placeholderString(it.groupValues[1]) })

    if (housing.players.size == 1) t.println(formatted)
    else t.println("${bold(name)}: $formatted")
}

fun Player.logActionBar(message: String) {
    if (housing.players.size == 1) t.println(message)
    else t.println("${bold(name)}: $message")
}

fun Player.logTitle(title: String, subtitle: String) {

}

fun Player.logSound(sound: Sound) {

}

fun Player.logDisplayMenu(menu: String) {

}

fun formatText(text: String): String {
    val parts = mutableListOf<Pair<String, TextStyle>>()

    var section = StringBuilder()
    var currentStyle: TextStyle = brightWhite

    var i = 0
    while (i < text.length) {

        if (text[i] != '&') {
            section.append(text[i++])
            continue
        }

        when (text[i + 1]) {
            'k', 'l', 'm', 'n', 'o', 'r', in '0'..'9', in 'a'..'f' -> {
                parts += Pair(section.toString(), currentStyle)
                section = StringBuilder()
                currentStyle = colorFromCode(currentStyle, text[i + 1])
                i++
            }
            else -> {
                section.append('&')
            }
        }

        i++
    }

    parts += Pair(section.toString(), currentStyle)
    section = StringBuilder()

    return parts.map { it.second(it.first) }.joinToString("")
}

fun colorFromCode(currentStyle: TextStyle, code: Char): TextStyle {
    return when (code) {
        '0' -> black
        '1' -> blue
        '2' -> green
        '3' -> cyan
        '4' -> red
        '5' -> magenta
        '6' -> yellow
        '7' -> white
        '8' -> gray
        '9' -> brightBlue
        'a' -> brightGreen
        'b' -> brightCyan
        'c' -> brightRed
        'd' -> brightMagenta
        'e' -> brightYellow
        'f' -> brightWhite

        'k' -> currentStyle + dim
        'l' -> currentStyle + bold
        'm' -> currentStyle + strikethrough
        'n' -> currentStyle + underline
        'o' -> currentStyle + italic
        'r' -> brightWhite
        else -> error("unreachable")
    }
}