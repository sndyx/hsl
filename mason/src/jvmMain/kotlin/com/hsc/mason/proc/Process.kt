package com.hsc.mason.proc

actual fun process(command: String): String {
    return Runtime.getRuntime()
        .exec(command)
        .inputStream
        .bufferedReader()
        .readText()
}

actual fun printProcess(command: String) {
    val reader = Runtime.getRuntime()
        .exec(command).inputReader()
    var line: String? = ""
    while (reader.readLine().also { line = it } != null) {
        println(line)
    }
}

