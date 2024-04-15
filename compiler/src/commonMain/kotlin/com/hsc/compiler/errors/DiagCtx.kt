package com.hsc.compiler.errors

import com.hsc.compiler.parse.SourceProvider
import com.hsc.compiler.span.Span

class DiagCtx(private val emitter: Emitter, val srcp: SourceProvider? = null) {

    fun bug(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Bug, message, span)
    fun err(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Error, message, span)
    fun warn(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Warning, message, span)
    fun hint(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Hint, message, span)
    fun note(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Note, message, span)


    private fun diagnostic(
        level: Level,
        message: String,
        span: Span?
    ): Diagnostic = Diagnostic(this, level, message, mutableListOf(), mutableListOf())
        .also {  diag -> span?.let { diag.span(it) } }

    fun emit(diagnostic: Diagnostic) {
        if (srcp != null && srcp.isVirtual) {
            diagnostic.reference(srcp.virtualSpan!!, "macro expansion here")
        }
        emitter.emit(diagnostic)
    }

}