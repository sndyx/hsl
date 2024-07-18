package com.hsc.compiler.errors

import com.hsc.compiler.driver.Driver
import com.hsc.compiler.parse.SourceProvider
import com.hsc.compiler.span.Span

class DiagCtx(private val emitter: Driver, val srcp: SourceProvider? = null) {

    fun bug(
        message: String,
        span: Span? = null,
        throwable: Throwable = RuntimeException()
    ): Diagnostic = diagnostic(Level.Bug, message, span, throwable)
    fun err(
        message: String,
        span: Span? = null,
        throwable: Throwable? = null
    ): Diagnostic = diagnostic(Level.Error, message, span, throwable)
    fun warn(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Warning, message, span)
    fun hint(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Hint, message, span)
    fun note(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Note, message, span)


    private fun diagnostic(
        level: Level,
        message: String,
        span: Span?,
        throwable: Throwable? = null
    ): Diagnostic = Diagnostic(this, level, message, mutableListOf(), mutableListOf(), throwable)
        .also {  diag -> span?.let { diag.span(it) } }

    fun emit(diagnostic: Diagnostic) {
        if (srcp != null && srcp.isVirtual) {
            diagnostic.reference(srcp.virtualSpan!!, "macro expansion here")
        }
        emitter.emitDiagnostic(diagnostic)
    }

}