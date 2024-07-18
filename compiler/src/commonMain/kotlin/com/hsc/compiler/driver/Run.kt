package com.hsc.compiler.driver

import kotlinx.coroutines.*

fun runCompiler(opts: CompileOptions, fp: FileProvider) = runBlocking {
    val driver = when (opts.driver) {
        DriverMode.Diagnostics -> DiagnosticDriver(opts)
        DriverMode.Interpreter -> InterpreterDriver(opts)
        DriverMode.Output -> OutputDriver(opts)
    }

    driver.runCompiler(fp)
}