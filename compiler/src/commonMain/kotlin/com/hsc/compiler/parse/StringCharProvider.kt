package com.hsc.compiler.parse

class StringCharProvider(
    private val src: String,
    override val srcOffset: Int = 0
) : CharProvider {

    override var pos: Int = 0

    override fun next(): Char = src[pos++]

    override fun hasNext(): Boolean = pos < src.length

    override fun lookahead(count: Int): Char? = src.getOrNull(pos + count)
}