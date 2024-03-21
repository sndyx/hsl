package com.hsc.compiler.pretty

import com.github.ajalt.mordant.terminal.Terminal
import kotlin.reflect.full.declaredMemberProperties

fun prettyPrintActionsWebview(functions: List<com.hsc.compiler.ir.action.Function>) {
    val t = Terminal()
    functions.sortedBy { it.name }.forEachIndexed { idx, it ->
        t.println("<h2><b><span class=\"kw\">function</span> ${it.name}</b></h2>")
        it.actions.forEach { action ->
            t.print("<h3>&nbsp;&nbsp;")
            t.println("${(actionMap[action::class]!!)}</h4>")
            action.javaClass.kotlin.declaredMemberProperties.forEachIndexed { idx, field ->
                if (idx == action.javaClass.kotlin.declaredMemberProperties.size - 1) {
                    t.print("<p class=\"small\">&nbsp;&nbsp;<span class=\"dim\">\\</span>&nbsp;")
                } else {
                    t.print("<p class=\"small\">&nbsp;&nbsp;<span class=\"dim\">|</span>&nbsp;")
                }
                t.print("<span class=\"dim\">${field.name}")
                t.print(" =</span> ")
                t.println("<i>${field.get(action)}</i></p>")
            }
        }
        if (idx != functions.size - 1) {
            t.println("<hr>")
        }
    }
}