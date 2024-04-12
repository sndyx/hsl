package com.hsc.compiler.parse.macros

import com.hsc.compiler.parse.*

object IfMacroProvider : MacroProvider {

    override val name: String = "#if"

    override fun invoke(lexer: Lexer): CharProvider {
        val stream = TokenStream(lexer.iterator())
        val parser = Parser(stream, lexer.sess)

        with(parser) {
            expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
            val expr = parseExpr()
            expect(TokenKind.CloseDelim(Delimiter.Parenthesis))

            val src = if (eat(TokenKind.OpenDelim(Delimiter.Brace))) {
                var depth = 1
                lexer.takeWhile {
                    if (it == '{') depth++
                    if (it == '}') depth--
                    !(it == '}' && depth == 0) // exit
                }
            } else {
                lexer.takeWhile { it != '\n' }
            }


        }

        TODO()
    }

}