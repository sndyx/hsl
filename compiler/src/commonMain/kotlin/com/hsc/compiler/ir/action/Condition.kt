package com.hsc.compiler.ir.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Condition {

    @Serializable
    @SerialName("PLAYER_STAT")
    data class PlayerStatRequirement(
        val stat: String,
        @SerialName("mode") val op: Comparison,
        val value: StatValue,
    ) : Condition()
    @Serializable
    @SerialName("GLOBAL_STAT")
    data class GlobalStatRequirement(
        val stat: String,
        @SerialName("mode") val op: Comparison,
        val value: StatValue,
    ) : Condition()
    @Serializable
    @SerialName("SNEAKING")
    data object PlayerSneaking : Condition()
}

enum class Comparison {
    @SerialName("EQUAL") Eq,
    @SerialName("GREATER_THAN") Gt,
    @SerialName("GREATER_THAN_OR_EQUAL") Ge,
    @SerialName("LESS_THAN") Lt,
    @SerialName("LESS_THAN_OR_EQUAL") Le;
}