package com.hsc.compiler.ir.action

import kotlinx.serialization.Serializable

@Serializable
data class Function(val name: String, val actions: List<Action>) {

    override fun toString(): String {
        return "function ${name}:\n  " + actions.joinToString("\n  ") { it.toString() }
    }

}