package com.hsc.mason.proc

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.*
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString

actual fun exitProcess(code: Int): Nothing {
    kotlin.system.exitProcess(code)
}

@OptIn(ExperimentalForeignApi::class)
actual fun process(command: String): String {
    val fp = popen(command, "r") ?: error("Failed to run command: dir")

    val stdout = buildString {
        val buffer = ByteArray(4096)
        while (true) {
            val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
            append(input.toKString())
        }
    }

    pclose(fp)
    return stdout
}

@OptIn(ExperimentalForeignApi::class)
actual fun printProcess(command: String) {
    val fp = popen(command, "r") ?: error("Failed to run command: dir")
    val buffer = ByteArray(4096)
    while (true) {
        val input = fgets(buffer.refTo(0), buffer.size, fp) ?: break
        print(input.toKString())
    }
    pclose(fp)
}