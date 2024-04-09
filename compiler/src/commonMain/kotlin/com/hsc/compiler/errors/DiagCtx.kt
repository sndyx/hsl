package com.hsc.compiler.errors

import com.hsc.compiler.span.Span

class DiagCtx(
    private val emitter: Emitter,
) {

    fun bug(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Bug, message, span)

    fun bug(exception: Exception, span: Span? = null): Diagnostic =
        diagnostic(Level.Bug, exception.message ?: "internal error", span)

    fun bug(error: Error, span: Span? = null): Diagnostic =
        diagnostic(Level.Bug, error.message ?: "internal error", span)

    fun err(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Error, message, span)

    fun warn(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Warning, message, span)

    fun hint(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Hint, message, span)

    fun note(message: String, span: Span? = null): Diagnostic = diagnostic(Level.Note, message, span)


    private fun diagnostic(
        level: Level,
        message: String,
        span: Span?
    ): Diagnostic = Diagnostic(this, level, message, mutableListOf(), mutableListOf())
        .also {  diag ->
            span?.let { diag.span(it) }
        }

    internal fun emit(diagnostic: Diagnostic) {
        emitter.emit(diagnostic)
    }

}