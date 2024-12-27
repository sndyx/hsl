package com.hsc.compiler.ir.action

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Function(val name: String, val actions: List<Action>, val processors: List<FunctionProcessor>) {

    override fun toString(): String {
        return "function ${name}:\n  " + actions.joinToString("\n  ") { it.toString() }
    }

}

@Serializable
data class FunctionProcessor(val name: String, val args: List<JsonElement>)