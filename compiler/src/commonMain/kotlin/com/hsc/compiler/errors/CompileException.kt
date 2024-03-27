package com.hsc.compiler.errors

class CompileException(public val diag: Diagnostic) : Exception()