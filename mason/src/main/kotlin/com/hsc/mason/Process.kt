package com.hsc.mason

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import java.io.InputStream

suspend fun process(cmd: String): CommandResult = coroutineScope {
    val proc = Runtime.getRuntime().exec(cmd)
    CommandResult(proc)
}

data class CommandResult(
    private val proc: Process,
) {

    val result: InputStream = proc.inputStream
    val error: InputStream = proc.errorStream

    suspend fun await(): CompletedCommandResult = coroutineScope {
        while (proc.isAlive) delay(10)
        CompletedCommandResult(result.reader().readText(), error.reader().readText())
    }

}

data class CompletedCommandResult(val result: String, val error: String)