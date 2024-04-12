@file:Suppress("MemberVisibilityCanBePrivate")

package com.hsc.compiler.parse

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.span.Span

class Lexer(
    val sess: CompileSess,
    val srcp: SourceProvider,
): Iterable<Token> {

    val fid = srcp.fid

    val pos: Int get() = srcp.pos

    fun isIdStart(char: Char): Boolean = char.isLetter() || char == '_' || char == '#'
    fun isIdContinue(char: Char): Boolean = char.isDigit() || char.isLetter() || char == '_'

    fun advanceToken(): Token {
        eatWhile { it.isWhitespace() }

        val currentChar = bump() ?: return Token(TokenKind.Eof, Span.single(pos, fid))

        if (currentChar == '/') {
            when (first()) {
                '/' -> return eatLineComment()
                '*' -> return eatBlockComment()
            }
        }

        if (currentChar == '-' && first() == '>') {
            bump()
            return Token(TokenKind.Arrow, Span(pos - 1, pos, fid))
        }
        if (currentChar == '&' && first() == '&') {
            bump()
            return Token(TokenKind.AndAnd, Span(pos - 1, pos, fid))
        }
        if (currentChar == '|' && first() == '|') {
            bump()
            return Token(TokenKind.OrOr, Span(pos - 1, pos, fid))
        }
        if (currentChar == '.' && first() == '.') {
            bump()
            return Token(TokenKind.DotDot, Span(pos - 1, pos, fid))
        }

        when(currentChar) {
            '+' -> BinOpToken.Plus
            '-' -> BinOpToken.Minus
            '*' -> BinOpToken.Star
            '/' -> BinOpToken.Slash
            '%' -> BinOpToken.Percent
            '^' -> BinOpToken.Caret
            else -> null
        }?.let { type ->
            val pair = if (first() == '=') {
                bump()
                Pair(TokenKind.BinOpEq(type), Span(pos - 1, pos, fid))
            } else {
                Pair(TokenKind.BinOp(type), Span.single(pos, fid))
            }
            return Token(pair.first, pair.second)
        }

        val startPos = pos
        val kind = when(currentChar) {
            '(' -> TokenKind.OpenDelim(Delimiter.Parenthesis)
            ')' -> TokenKind.CloseDelim(Delimiter.Parenthesis)
            '{' -> TokenKind.OpenDelim(Delimiter.Brace)
            '}' -> TokenKind.CloseDelim(Delimiter.Brace)
            '[' -> TokenKind.OpenDelim(Delimiter.Bracket)
            ']' -> TokenKind.CloseDelim(Delimiter.Bracket)
            ',' -> TokenKind.Comma
            ':' -> TokenKind.Colon
            '@' -> TokenKind.At
            '=' -> {
                when (first()) {
                    '=' -> {
                        bump()
                        TokenKind.EqEq
                    }
                    '>' -> {
                        bump()
                        TokenKind.FatArrow
                    }
                    else -> TokenKind.Eq
                }
            }
            '<' -> {
                if (first() == '=') {
                    bump()
                    TokenKind.Le
                } else {
                    TokenKind.Lt
                }
            }
            '>' -> {
                if (first() == '=') {
                    bump()
                    TokenKind.Ge
                } else {
                    TokenKind.Gt
                }
            }
            '"' -> string()
            in '0'..'9' -> number(currentChar)
            else -> {
                if (isIdStart(currentChar)) {
                    identKwBoolMacroItem(currentChar)
                } else {
                    TokenKind.Unknown(currentChar)
                }
            }
        }

        return Token(kind, Span(startPos, pos, fid))
    }

    fun eatLineComment(): Token {
        bump()
        eatWhile { it != '\n' }
        return advanceToken()
    }

    fun eatBlockComment(): Token {
        bump()
        var depth = 1
        var c: Char? = bump()
        while (c != null) {
            if (c == '/' && first() == '*') {
                bump()
                depth++
            }
            else if (c == '*' && first() == '/') {
                bump()
                depth--
                if (depth == 0) break
            }
            c = bump()
        }
        return advanceToken()
    }

    // Yes it is absolutely necessary to include all of this functionality in one function. :-)
    fun identKwBoolMacroItem(start: Char): TokenKind {
        val startPos = pos

        val sb = StringBuilder("$start")

        while (true) {
            if (isIdContinue(first())) {
                sb.append(first())
                bump()
            } else {
                break
            }
        }

        return when (sb.toString()) {
            "else" -> TokenKind.Kw(Keyword.Else)
            "enum" -> TokenKind.Kw(Keyword.Enum)
            "fn" -> TokenKind.Kw(Keyword.Fn)
            "for" -> TokenKind.Kw(Keyword.For)
            "if" -> TokenKind.Kw(Keyword.If)
            "in" -> TokenKind.Kw(Keyword.In)
            "match" -> TokenKind.Kw(Keyword.Match)
            "while" -> TokenKind.Kw(Keyword.While)
            "break" -> TokenKind.Kw(Keyword.Break)
            "continue" -> TokenKind.Kw(Keyword.Continue)
            "return" -> TokenKind.Kw(Keyword.Return)
            "const" -> TokenKind.Kw(Keyword.Const)
            "true" -> TokenKind.Literal(Lit(LitKind.Bool, "true"))
            "false" -> TokenKind.Literal(Lit(LitKind.Bool, "false"))
            // Can just parse null as 0. On the other hand, booleans should stay bool literals because of
            // later on parsing around built-in functions...
            "null" -> TokenKind.Literal(Lit(LitKind.I64, "0"))

            "item" -> {
                eatWhile { it.isWhitespace() }
                if (bump() != '{') {
                    throw sess.dcx().err("expected {", Span.single(pos, fid))
                }

                val sb2 = StringBuilder("{")
                var depth = 1

                var c: Char? = bump()
                while (c != null) {
                    if (c == '{') {
                        depth++
                    }
                    else if (c == '}') {
                        depth--
                        if (depth == 0) {
                            sb2.append('}')
                            break
                        }
                    }
                    sb2.append(c)
                    c = bump()
                }
                TokenKind.Literal(Lit(LitKind.Item, sb2.toString()))
            }

            else -> {
                val ident = sb.toString()

                if (srcp.isMacro(ident)) {
                    // Handle macro invocation
                    srcp.enterMacro(this, Span(startPos, pos, fid), ident)
                    advanceToken().kind // We discard the span, I guess?
                } else {
                    // Just handle ident normally
                    TokenKind.Ident(ident)
                }
            }
        }
    }

    fun string(): TokenKind {
        val sb = StringBuilder()

        var c: Char? = bump()
        while (c != null) {
            if (c == '"') {
                break // Break for now, later check unterminated ?
            }
            sb.append(c)
            c = bump()
        }

        // Eof reached...
        return TokenKind.Literal(Lit(LitKind.Str, sb.toString()))
    }

    fun number(firstDigit: Char): TokenKind {
        val num = StringBuilder("$firstDigit")
        num.append(digits())
        return if (first() == '.' && second() != '.') {
            num.append('.')
            bump()
            num.append(digits())
            TokenKind.Literal(Lit(LitKind.F64, num.toString()))
        } else {
            TokenKind.Literal(Lit(LitKind.I64, num.toString()))
        }
    }

    fun digits(): String {
        val sb = StringBuilder()
        while (true) {
            if (first() == '_') {
                bump()
            } else if (first() in '0'..'9') {
                sb.append(first())
                bump()
            } else {
                break
            }
        }
        return sb.toString()
    }

    fun bump(): Char? {
        return srcp.bump()
    }

    fun first(): Char = srcp.first()
    fun second(): Char = srcp.second()

    fun eatWhile(predicate: (Char) -> Boolean) {
        while (!isEof() && predicate(first())) {
            if (bump() == '\n') srcp.addLine(pos)
        }
    }

    fun isEof(): Boolean = srcp.isEmpty()

    var iteratorObj: Iterator<Token>? = null
    override fun iterator(): Iterator<Token> {
        return iteratorObj ?: object : Iterator<Token> {
            override fun hasNext(): Boolean = !isEof()
            override fun next(): Token = advanceToken()
        }.also { iteratorObj = it }
    }

}