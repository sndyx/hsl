package com.hsc.compiler.driver

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext

abstract class Driver(val opts: CompileOptions) {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val scope: CoroutineDispatcher = newSingleThreadContext("compiler")

    val t: Terminal = when (opts.color) {
        Color.Auto -> Terminal()
        Color.Always -> Terminal(AnsiLevel.ANSI256)
        Color.Never -> Terminal(AnsiLevel.NONE)
    }

    var buildFailed = false
    var dcx: DiagCtx = DiagCtx(this)

    abstract fun runCompiler(fp: FileProvider)

    abstract fun handleDiagnostic(diag: Diagnostic)

    fun emitDiagnostic(diag: Diagnostic) {
        if (diag.level == Level.Error || diag.level == Level.Bug) buildFailed = true
        handleDiagnostic(diag)
    }

    suspend fun <T : Any> enter(block: suspend () -> T): T {
        try {
            val result = block()
            if (buildFailed) fatal()
            return result
        } catch (diag: Diagnostic) {
            diag.emit()
            fatal()
        } catch (error: Throwable) {
            handleError(error, dcx)
            fatal()
        }
    }

    private fun fatal(): Nothing {
        exitProcess(0)
    }

    private fun handleError(error: Throwable, dcx: DiagCtx) {
        error::class.simpleName?.formatError?.let { name ->
            when (name) {
                "error_stack_overflow" -> {
                    if (dcx.srcp != null) {
                        dcx.err("stack overflow during macro expansion", dcx.srcp.virtualSpan, error).emit()
                    } else {
                        dcx.err("stack overflow during compilation", null, error).emit()
                    }
                }
                "error_out_of_memory" -> {
                    dcx.err("out of memory").emit()
                }
                else -> {
                    val message =
                        if (error.message != null) "$name: ${error.message!!.first().lowercase()}${error.message!!.drop(1)}"
                        else name
                    dcx.bug(message, null, error).emit()
                }
            }
        }
    }

    private val String.formatError: String get() {
        val sb = StringBuilder()
        var first = true
        forEach {
            if (it.isUpperCase() && !first) sb.append('_')
            sb.append(it.lowercase())
            first = false
        }
        val string = sb.toString()
        val parts = string.split('_')
        return if (parts.lastOrNull() == "exception" || parts.lastOrNull() == "error") {
            "error_" + parts.dropLast(1).joinToString("_")
        } else string
    }

}