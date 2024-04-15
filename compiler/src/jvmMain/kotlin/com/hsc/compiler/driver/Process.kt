package com.hsc.compiler.driver

actual fun exitProcess(status: Int): Nothing = kotlin.system.exitProcess(status)

actual fun availableProcessors(): Int = Runtime.getRuntime().availableProcessors()