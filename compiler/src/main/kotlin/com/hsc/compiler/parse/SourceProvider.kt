package com.hsc.compiler.parse

import com.hsc.compiler.span.SourceFile
import com.hsc.compiler.span.Span

class SourceProvider(private val file: SourceFile) {

    private val macros = mutableListOf<MacroProvider>()

    private val srcStack = mutableListOf(file.src)
    private val posStack = mutableListOf<Int>()

    val fid = file.fid

    var pos = 0

    val src: String get() = srcStack.last()
    val isVirtual: Boolean get() = srcStack.size > 1
    var virtualSpan: Span? = null

    fun bump(): Char? {
        if (srcStack.lastOrNull()?.length?.let { it <= pos} == true) {
            srcStack.removeLast()
            if (isEmpty()) {
                return null
            } // out of src
            pos = posStack.removeLast()
            return srcStack.last().getOrNull(pos) ?: ' '
        } else {
            return srcStack.lastOrNull()?.getOrNull(pos++)
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

    fun isMacro(ident: String): Boolean {
        return macros.any { it.name == ident }
    }

    fun macroArgCount(ident: String): Int {
        return macros.first { it.name == ident }.args.size
    }

    fun addMacro(provider: MacroProvider) {
        macros.add(provider)
    }

    fun enterMacro(span: Span, name: String, args: List<String>) {
        if (!isVirtual) {
            virtualSpan = span
        }
        posStack.add(pos)
        srcStack.add(macros.first { it.name == name }.invoke(args))
        pos = 0
    }

}