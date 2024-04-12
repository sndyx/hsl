package com.hsc.compiler.parse

interface CharProvider {
    val srcOffset: Int
    var pos: Int
    fun next(): Char
    fun hasNext(): Boolean
    fun lookahead(count: Int): Char?

    fun nextOrNull(): Char? = if (hasNext()) next() else null
}