package com.hsc.compiler.ir.action

import kotlinx.serialization.Serializable

@Serializable
data class Function(public val name: String, public val actions: List<com.hsc.compiler.ir.action.Action>) {

    override fun toString(): String {
        return "function ${name}:\n  " + actions.joinToString("\n  ") { it.toString() }
    }

}