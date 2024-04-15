package com.hsc.compiler.parse

import com.hsc.compiler.span.Span

fun Lexer.expectNumber(): Long {
    val lo = pos + 1
    val negative = eat('-')
    val number = runCatching {
        if (first() == '0' && second() !in '0'..'9') "0".also { bump() }
        else expect { it in '1'..'9' } + takeWhile { it.isDigit() || it == '_' }
    }.getOrElse { throw sess.dcx().err("invalid integer", Span(lo, pos, fid)) }
    val long = number.replace("_", "").toLongOrNull()
    if (long == null) {
        val err = sess.dcx().err("invalid integer")
        val hint = if (negative) {
            "below 64-bit integer limit"
        } else "above 64-bit integer limit"
        err.spanLabel(Span(lo, pos, fid), hint)
        throw err
    }
    return if (negative) long * -1L else long
}

fun Lexer.eat(char: Char): Boolean {
    if (first() == char) {
        bump()
        return true
    }
    return false
}

fun Lexer.expect(char: Char) {
    val found = bump()
    if (found != char) {
        throw sess.dcx().err("expected $char, found $found", Span.single(pos, fid))
    }
}

fun Lexer.expect(predicate: (Char) -> Boolean): Char {
    val found = bump()
    if (found == null || !predicate(found)) {
        throw sess.dcx().err("unexpected", Span.single(pos, fid))
    }
    return found
}

fun Lexer.takeWhile(predicate: (Char) -> Boolean): String {
    val sb = StringBuilder()
    while (!isEof() && predicate(first())) {
        sb.append(bump().also { if (it == '\n') srcp.addLine(pos) })
    }
    return sb.toString()
}

fun Lexer.eatWhitespace() = eatWhile {
    it.isWhitespace()
}

fun <T : Any> Lexer.floating(block: Lexer.() -> T): T {
    eatWhitespace()
    val result = block(this)
    eatWhitespace()
    return result
}