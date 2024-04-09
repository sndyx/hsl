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
    @SerialName("GT") Gt,
    @SerialName("GE") Ge,
    @SerialName("LT") Lt,
    @SerialName("LE") Le;
}