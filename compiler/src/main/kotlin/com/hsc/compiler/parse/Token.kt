package com.hsc.compiler.parse

import com.hsc.compiler.span.Span
import kotlin.reflect.KClass

data class Token(val kind: TokenKind, val span: Span) {
    companion object {
        fun dummy(): Token = Token(TokenKind.Dummy, Span(0, 0, 0))
    }
}

sealed class TokenKind {

    data object Eq : TokenKind() {
        override fun toString(): String = "="
    }
    data object Lt : TokenKind() {
        override fun toString(): String = "<"
    }
    data object Le : TokenKind() {
        override fun toString(): String = "<="
    }
    data object EqEq : TokenKind() {
        override fun toString(): String = "=="
    }
    data object Gt : TokenKind() {
        override fun toString(): String = ">"
    }
    data object Ge : TokenKind() {
        override fun toString(): String = ">="
    }
    data object AndAnd : TokenKind() {
        override fun toString(): String = "&&"
    }
    data object OrOr : TokenKind() {
        override fun toString(): String = "||"
    }
    data object DotDot : TokenKind() {
        override fun toString(): String = ".."
    }
    data object Not : TokenKind() {
        override fun toString(): String = "!"
    }
    data object At : TokenKind() {
        override fun toString(): String = "@"
    }
    data object Dot : TokenKind() {
        override fun toString(): String = "."
    }
    data object Comma : TokenKind() {
        override fun toString(): String = ","
    }
    data object Semi : TokenKind() {
        override fun toString(): String = ";"
    }
    data object ModSep : TokenKind() {
        override fun toString(): String = "::"
    }
    data object Arrow : TokenKind() {
        override fun toString(): String = "->"
    }
    data object FatArrow : TokenKind() {
        override fun toString(): String = "=>"
    }
    data object Pound : TokenKind() {
        override fun toString(): String = "#"
    }
    data object Colon : TokenKind() {
        override fun toString(): String = ":"
    }

    data class BinOp(val type: BinOpToken) : TokenKind() {
        override fun toString(): String = type.toString()
    }
    data class BinOpEq(val type: BinOpToken) : TokenKind() {
        override fun toString(): String = type.toString()
    }
    data class OpenDelim(val type: Delimiter) : TokenKind() {
        override fun toString(): String = type.value[0].toString()
    }
    data class CloseDelim(val type: Delimiter) : TokenKind() {
        override fun toString(): String = type.value[1].toString()
    }

    data class Literal(val lit: Lit) : TokenKind() {
        override fun toString(): String = when (lit.kind) {
            LitKind.Bool, LitKind.I64, LitKind.F64, LitKind.Null -> lit.value
            LitKind.Str -> "\"${lit.value}\""
        }
    }
    data class Ident(val value: String) : TokenKind() {
        override fun toString(): String = value
    }
    data class Kw(val kw: Keyword) : TokenKind() {
        override fun toString(): String = "${kw.value} keyword"
    }

    data object Dummy : TokenKind() {
        override fun toString(): String = "<dummy>"
    }
    data object Newline : TokenKind() {
        override fun toString(): String = "<newline>"
    }
    data object Eof : TokenKind() {
        override fun toString(): String = "<eof>"
    }

    data class Unknown(val value: Char) : TokenKind() {
        override fun toString(): String = "<unknown>"
    }

    val binOp: BinOp get() = this as BinOp
    val binOpEq: BinOpEq get() = this as BinOpEq
    val openDelim: OpenDelim get() = this as OpenDelim
    val closeDelim: CloseDelim get() = this as CloseDelim
    val literal: Literal get() = this as Literal
    val ident: Ident get() = this as Ident
    val keyword: Kw get() = this as Kw

}

enum class Keyword(val value: String) {

    Else("else"),
    Enum("enum"),
    Fn("fn"),
    For("for"),
    If("if"),
    In("in"),
    Match("match"),
    While("while"),
    Break("break"),
    Continue("continue"),
    Return("return"),
    Const("const");

}

data class Lit(val kind: LitKind, val value: String)

enum class LitKind {
    Bool,
    I64,
    F64,
    Str,
    Null;

    override fun toString(): String {
        return when (this) {
            Bool -> "bool"
            I64 -> "integer"
            F64 -> "float"
            Str -> "string"
            Null -> "null"
        }
    }
}

enum class BinOpToken(private val value: String) {
    Plus("+"),
    Minus("-"),
    Star("*"),
    Slash("/"),
    Percent("%"),
    Caret("^");

    override fun toString(): String = value
}

enum class Delimiter(internal val value: String) {
    Parenthesis("()"),
    Brace("{}"),
    Bracket("[]");
}

val <T : TokenKind> KClass<T>.name: String
    get() = when (this) {
        TokenKind.Eq::class -> "="
        TokenKind.Lt::class -> "<"
        TokenKind.Le::class -> "<="
        TokenKind.EqEq::class -> "=="
        TokenKind.Gt::class -> ">"
        TokenKind.Ge::class -> ">="
        TokenKind.AndAnd::class -> "&&"
        TokenKind.OrOr::class -> "||"
        TokenKind.Not::class -> "!"
        TokenKind.At::class -> "@"
        TokenKind.Dot::class -> "."
        TokenKind.Comma::class -> ","
        TokenKind.Semi::class -> ";"
        TokenKind.ModSep::class -> "::"
        TokenKind.Arrow::class -> "->"
        TokenKind.FatArrow::class -> "=>"
        TokenKind.Pound::class -> "#"
        TokenKind.BinOp::class -> "binary operator"
        TokenKind.BinOpEq::class -> "binary operator equals"
        TokenKind.OpenDelim::class -> "opening delimiter"
        TokenKind.CloseDelim::class -> "closing delimiter"
        TokenKind.Literal::class -> "literal"
        TokenKind.Ident::class -> "identifier"
        TokenKind.Kw::class -> "keyword"
        TokenKind.Newline::class -> "<newline>"
        TokenKind.Eof::class -> "<eof>"
        else -> "unreachable"
    }
