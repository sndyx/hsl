package com.hsc.compiler.pretty

import com.github.ajalt.mordant.terminal.Terminal

fun prettyPrintActionsWebview(functions: List<com.hsc.compiler.ir.action.Function>) {
    val t = Terminal()
    functions.sortedBy { it.name }.forEachIndexed { idx, it ->
        t.println("<h2><b><span class=\"kw\">function</span> ${it.name}</b></h2>")
        it.actions.forEach { action ->
            t.print("<h3>&nbsp;&nbsp;")
            t.println("${(actionMap[action::class]!!)}</h4>")
        }
        if (idx != functions.size - 1) {
            t.println("<hr>")
        }
    }
}