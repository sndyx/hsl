package com.hsc.compiler.parse

object EmptyCharProvider : CharProvider {
    override val srcOffset: Int = 0
    override var pos: Int = 0
    override fun next(): Char = error("")
    override fun hasNext(): Boolean = false
    override fun lookahead(count: Int): Char? = null
}