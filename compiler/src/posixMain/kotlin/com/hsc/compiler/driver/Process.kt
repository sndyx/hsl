package com.hsc.compiler.driver

import platform.posix._SC_NPROCESSORS_ONLN
import platform.posix.sysconf

actual fun exitProcess(status: Int): Nothing = kotlin.system.exitProcess(status)

actual fun availableProcessors(): Int = sysconf(_SC_NPROCESSORS_ONLN).toInt()