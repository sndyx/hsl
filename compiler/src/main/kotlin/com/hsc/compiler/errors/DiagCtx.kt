package com.hsc.compiler.errors

class DiagCtx(
    private val emitter: Emitter,
) {

    fun bug(message: String): Diagnostic = diagnostic(Level.Bug, message)

    fun bug(exception: Exception): Diagnostic =
        diagnostic(Level.Bug, exception.message ?: "internal error", exception.stackTrace.toList())

    fun bug(error: Error): Diagnostic =
        diagnostic(Level.Bug, error.message ?: "internal error", error.stackTrace.toList())

    fun err(message: String): Diagnostic = diagnostic(Level.Error, message)

    fun warn(message: String): Diagnostic = diagnostic(Level.Warning, message)

    fun hint(message: String): Diagnostic = diagnostic(Level.Hint, message)

    fun note(message: String): Diagnostic = diagnostic(Level.Note, message)


    private fun diagnostic(
        level: Level,
        message: String,
        stackTrace: List<StackTraceElement>? = null
    ): Diagnostic = Diagnostic(this, level, message, stackTrace, mutableListOf(), mutableListOf())

    internal fun emit(diagnostic: Diagnostic) {
        emitter.emit(diagnostic)
    }

}