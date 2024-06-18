package com.hsc.mason.proc

fun sanitize(string: String): String { // this should work, for now
    return string.replace(Regex("[^A-Za-z0-9-/. ]"), "")
}

fun sanitizeStrict(string: String): String { // this should work, for now
    return string.replace(Regex("[^A-Za-z0-9-/.]"), "")
}