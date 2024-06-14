package com.hsc.mason.proc

actual fun exitProcess(code: Int): Nothing {
    kotlin.system.exitProcess(code)
}

actual fun process(command: String): String {
    var cmd = command
    if (cmd.startsWith("hsc")) {
        // FOR TESTING PURPOSES ONLY.
        cmd = "java -jar ../compiler/build/libs/compiler-jvm-1.1.0.jar" + cmd.removePrefix("hsc")
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
        cmd = "java -jar ../compiler/build/libs/compiler-jvm-1.1.0.jar" + cmd.removePrefix("hsc")
    }
    println(cmd)
    val reader = Runtime.getRuntime()
        .exec(cmd).inputStream
    val buffer = ByteArray(256)
    while (true) {
        val bytesRead: Int = reader.read(buffer)
        if (bytesRead == -1) break
        System.out.write(buffer, 0, bytesRead)
    }
}

