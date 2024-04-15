package com.hsc.compiler.driver

import platform.posix.pthread_num_processors_np

actual fun exitProcess(status: Int): Nothing = kotlin.system.exitProcess(status)

actual fun availableProcessors(): Int = pthread_num_processors_np()