package com.hsc.compiler.parse

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.errors.CompileException
import com.hsc.compiler.errors.Level
import com.hsc.compiler.span.Span

class Lexer(
    private val sess: CompileSess,
    private val srcp: SourceProvider,
): Iterable<Token> {

    private val fid = srcp.fid

    private val pos: Int get() = srcp.pos

    private fun isIdStart(char: Char): Boolean = char.isLetter() || char == '_'
    private fun isIdContinue(char: Char): Boolean = char.isDigit() || char.isLetter() || char == '_'

    fun advanceToken(): Token {
        val token = advanceToken0()
        if (srcp.isVirtual) {
            // modify span to processor usage
            return Token(token.kind, srcp.virtualSpan!!)
        }
        return token
    }

    private fun advanceToken0(): Token {
        eatWhile {
            if (it == '\n') srcp.addLine(pos + 1)
            it.isWhitespace()
        }

        val currentChar = bump() ?: return Token(TokenKind.Eof, Span.single(pos, fid))

        if (first() == '/') {
            when (second()) {
                '/' -> eatLineComment()
                '*' -> eatBlockComment()
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
            '#' -> TokenKind.Pound
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
                    identKwBoolMacro(currentChar)
                } else {
                    TokenKind.Unknown(currentChar)
                }
            }
        }

        return Token(kind, Span(startPos, pos, fid))
    }

    private fun eatLineComment(): Token {
        bump()
        eatWhile { it != '\n' }
        return advanceToken()
    }

    private fun eatBlockComment(): Token {
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
    private fun identKwBoolMacro(start: Char): TokenKind {
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

            else -> {
                val ident = sb.toString()

                if (srcp.isMacro(ident)) {
                    // Handle macro invocation
                    parseMacro(startPos, ident)
                    advanceToken0().kind // It's OK to discard the span, will become virtual.
                } else {
                    // Just handle ident normally
                    TokenKind.Ident(ident)
                }
            }
        }
    }

    private fun parseMacro(startPos: Int, ident: String) {
        val argCount = srcp.macroArgCount(ident)
        val args = mutableListOf<String>()
        if (first() == '(') { // It's fine to use parens even with 0 args
            val argStart = pos
            bump()
            eatWhile(Char::isWhitespace)
            var level = 1

            while (!(first() == ')' && level == 1) && !isEof()) {
                val arg = StringBuilder()
                while (first() != ',' && !isEof() && !(first() == ')' && level == 1)) {
                    if (first() == '(') level++
                    else if (first() == ')') level--
                    arg.append(bump())
                }

                if (arg.isEmpty()) {
                    val err = sess.dcx().err("unexpected token") // Best way to describe it I think...
                    err.span(Span.single(pos, fid))
                    throw CompileException(err)
                }

                args.add(arg.toString())
                if (first() == ',') bump() // Bump ,

                eatWhile(Char::isWhitespace)
            }
            bump() // Bump )

            if (args.size != argCount) {
                // Fuck grammar!!!
                val s1 = if (argCount == 1) "" else "s"
                val s2 = if (args.size == 1) "" else "s"
                val was = if (args.size == 1) "was" else "were"

                val err = sess.dcx().err("this macro takes $argCount parameter$s1 but ${args.size} parameter$s2 $was supplied")
                err.span(Span(argStart, pos - 1, fid))
                throw CompileException(err)
            }
        } else {
            if (argCount > 0) { // No invocation & args expected
                val err = sess.dcx().err("expected (")
                err.span(Span.single(pos - 1, fid))
                err.note(Level.Error, "expecting macro invocation")
                throw CompileException(err)
            }
        }
        srcp.enterMacro(Span(startPos, pos, fid), ident, args)
    }


    private fun string(): TokenKind {
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

    private fun number(firstDigit: Char): TokenKind {
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

    private fun digits(): String {
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

    private fun bump(): Char? {
        return srcp.bump()
    }

    private fun first(): Char = srcp.first()
    private fun second(): Char = srcp.second()

    private fun eatWhile(predicate: (Char) -> Boolean) {
        while (!isEof() && predicate(first())) {
            bump()
        }
    }

    private fun isEof(): Boolean = srcp.isEmpty()

    override fun iterator(): Iterator<Token> {
        return object : Iterator<Token> {
            override fun hasNext(): Boolean = !isEof()
            override fun next(): Token = advanceToken()
        }
    }

}
