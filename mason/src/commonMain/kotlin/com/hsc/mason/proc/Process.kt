package com.hsc.mason.proc

expect fun exitProcess(code: Int): Nothing

expect fun process(command: String): String

expect fun printProcess(command: String)