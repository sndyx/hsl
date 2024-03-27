package com.hsc.compiler.ir.ast

import kotlin.random.Random
import kotlin.random.nextULong

class NodeId(
    val ownerId: ULong,
    val id: ULong
) {

    companion object {
        fun from(ownerId: NodeId): NodeId = NodeId(ownerId.id, Random.nextULong())
        fun from(ownerId: ULong): NodeId = NodeId(ownerId, Random.nextULong())
    }

    override fun toString(): String = "$id -> $ownerId"

}