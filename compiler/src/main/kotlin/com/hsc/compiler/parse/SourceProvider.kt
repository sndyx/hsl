package com.hsc.compiler.parse

import com.hsc.compiler.span.SourceFile
import com.hsc.compiler.span.Span

class SourceProvider(private val file: SourceFile) {

    private val virtualSrc = mutableMapOf<String, String>()

    private val srcStack = mutableListOf(file.src)
    private val posStack = mutableListOf<Int>()

    val fid = file.fid

    var pos = 0
    var prev: Char = ' '

    val src: String get() = srcStack.last()
    val isVirtual: Boolean get() = srcStack.size > 1
    var virtualSpan: Span? = null

    fun bump(): Char? {
        if (srcStack.lastOrNull()?.length == pos) {
            srcStack.removeLast()
            if (isEmpty()) return null // out of src
            pos = posStack.removeLast()
            prev = srcStack.last().getOrNull(pos) ?: ' '
            return prev
        } else {
            prev = srcStack.lastOrNull()?.getOrNull(pos++) ?: return null
            return prev
        }
    }

    fun first(): Char = srcStack.lastOrNull()?.getOrNull(pos) ?: ' '

    fun second(): Char = srcStack.lastOrNull()?.getOrNull(pos + 1) ?: ' '

    fun isEmpty(): Boolean = srcStack.isEmpty()

    fun addLine(pos: Int) {
        if (!isVirtual) { // do NOT let the macros add lines XD
            file.addLine(pos)
        }
    }

    fun isVirtualSource(ident: String): Boolean {
        return virtualSrc.containsKey(ident)
    }

    fun addSource(ident: String, src: String) {
        virtualSrc[ident] = src
    }

    fun setSource(span: Span, name: String) {
        if (!isVirtual) {
            virtualSpan = span
        }
        posStack.add(pos)
        srcStack.add(virtualSrc[name]!!)
        pos = 0
    }

}