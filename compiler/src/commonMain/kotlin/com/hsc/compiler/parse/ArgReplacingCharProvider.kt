package com.hsc.compiler.parse

class ArgReplacingCharProvider(
    private val args: Map<String, String>,
    private var src: String,
    override val srcOffset: Int = 0
) : CharProvider {

    private val anomalies: MutableMap<Int, Int> = mutableMapOf()

    init {
        var (name, actual, idx) = lowestArgIndex()
        while (idx != null) {
            src = src.replaceFirst(actual!!, args[name]!!) // gahh
            anomalies[idx + args[name]!!.length - 1] = actual.length - args[name]!!.length

            // stupid:
            val tmp = lowestArgIndex()
            name = tmp.first
            actual = tmp.second
            idx = tmp.third
        }
    }

    private fun lowestArgIndex(): Triple<String?, String?, Int?> = args.keys
        .flatMap { listOf(Pair(it, "\$$it"), Pair(it, "\${$it}")) }
        .map { Triple(it.first, it.second, src.indexOf(it.second)) }
        .filter { it.third != -1 }
        .minByOrNull { it.third } ?: Triple(null, null, null)

    private var apos = 0
    override var pos: Int = 1

    override fun next(): Char = src[apos++].also {
        pos++
        if (anomalies[apos - 1] != null) pos += anomalies[apos - 1]!!
    }

    override fun hasNext(): Boolean = apos < src.length

    override fun lookahead(count: Int): Char? = src.getOrNull(apos + count)
}