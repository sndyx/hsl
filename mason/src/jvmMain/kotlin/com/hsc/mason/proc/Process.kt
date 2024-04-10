package com.hsc.mason.proc

actual fun exitProcess(code: Int): Nothing {
    kotlin.system.exitProcess(code)
}

actual fun process(command: String): String {
    var cmd = command
    if (cmd.startsWith("hsc")) {
        // FOR TESTING PURPOSES ONLY.
        cmd = "java -jar C:\\Users\\Sandy\\IdeaProjects\\hsc\\compiler\\build\\libs\\compiler-jvm-0.0.1.jar" + cmd.removePrefix("hsc")
    }
    println(cmd)
    return Runtime.getRuntime()
        .exec(cmd)
        .inputStream
        .bufferedReader()
        .readText()
}

actual fun printProcess(command: String) {
    var cmd = command
    if (cmd.startsWith("hsc")) {
        // FOR TESTING PURPOSES ONLY.
        cmd = "java -jar C:\\Users\\Sandy\\IdeaProjects\\hsc\\compiler\\build\\libs\\compiler-jvm-0.0.1.jar" + cmd.removePrefix("hsc")
    }
    println(cmd)
    val reader = Runtime.getRuntime()
        .exec(cmd).inputStream.bufferedReader()
    var line: String? = ""
    while (reader.readLine().also { line = it } != null) {
        println(line)
    }
}

