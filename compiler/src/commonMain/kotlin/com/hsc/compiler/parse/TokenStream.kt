package com.hsc.compiler.parse

class TokenStream(
    private val producer: Iterator<Token>
) : Iterator<Token> by producer