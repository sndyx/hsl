package com.hsc.compiler.parse

interface MacroProvider {

    val name: String

    fun invoke(lexer: Lexer): CharProvider

}