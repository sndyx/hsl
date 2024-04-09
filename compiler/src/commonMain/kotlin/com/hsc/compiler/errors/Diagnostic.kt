package com.hsc.compiler.errors

import com.hsc.compiler.span.Span

data class Diagnostic(
    private val dcx: DiagCtx,
    val level: Level,
    override val message: String,
    val spans: MutableList<Triple<Span, Boolean, String?>>,
    val notes: MutableList<Pair<Level, String>>,
) : RuntimeException() {

    fun span(span: Span) {
        spans.add(Triple(span, false, null))
    }

    fun spanLabel(span: Span, label: String) {
        spans.add(Triple(span, false, label))
    }

    fun reference(span: Span, label: String) {
        spans.add(Triple(span, true, label))
    }

    fun note(level: Level, message: String) {
        notes.add(Pair(level, message))
    }

    fun emit() {
        dcx.emit(this)
    }

}