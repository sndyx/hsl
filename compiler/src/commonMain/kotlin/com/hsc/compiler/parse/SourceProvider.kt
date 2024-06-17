package com.hsc.compiler.parse

import com.hsc.compiler.parse.macros.*
import com.hsc.compiler.span.SourceFile
import com.hsc.compiler.span.Span

class SourceProvider(private val file: SourceFile) {

    private val macros = mutableListOf(
        ForMacroProvider,
        DefineMacroProvider,
        IfMacroProvider, ElifMacroProvider, ElseMacroProvider
    )

    private val srcStack = mutableListOf(file.provider())

    val fid = file.fid
    val pos get() = (srcStack.lastOrNull()?.let { it.srcOffset + it.pos }) ?: -1

    private var spanStack = mutableListOf<Span>()
    val isVirtual: Boolean get() = srcStack.size > 1
    val virtualSpan: Span? get() = spanStack.lastOrNull()

    fun bump(): Char? {
        if (srcStack.lastOrNull()?.hasNext() == false) {
            srcStack.removeLast()
            spanStack.removeLastOrNull()
            if (isEmpty()) {
                return null
            } // out of src
            return srcStack.lastOrNull()?.nextOrNull()
        } else {
            return srcStack.lastOrNull()?.nextOrNull()
        }
    }

    // stupid logic for first & second here.... lookahead will only support these values!
    fun first(): Char = srcStack.foldRight('\u0000') { provider, it ->
        if (it != '\u0000') it
        else provider.lookahead(0) ?: '\u0000'
    }.takeIf { it > '\u0000' } ?: ' '

    fun second(): Char = srcStack.foldRight('\u0000') { provider, it ->
        if (it != '\u0000' && it != '\u0001') it
        else if (it == '\u0000') {
            provider.lookahead(1) ?: if (provider.lookahead(0) == null) '\u0000' else '\u0001'
        } else {
            provider.lookahead(0) ?: '\u0001'
        }
    }.takeIf { it > '\u0001' } ?: ' '

    fun isEmpty(): Boolean = srcStack.isEmpty()

    fun addLine(pos: Int) {
        if (!isVirtual) { // do NOT let the macros add lines XD
            file.addLine(pos)
        }
    }

    fun isMacro(ident: String): Boolean {
        return macros.any { it.name == ident }
    }

    fun addMacro(provider: MacroProvider) {
        macros.add(provider)
    }

    fun enterMacro(lexer: Lexer, span: Span, ident: String) {
        spanStack.add(span)
        val provider = macros.first { it.name == ident }.invoke(lexer)
        srcStack.add(provider)
    }

}