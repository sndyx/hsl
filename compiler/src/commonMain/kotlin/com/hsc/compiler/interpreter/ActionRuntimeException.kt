package com.hsc.compiler.interpreter

class ActionRuntimeException(override val message: String, trace: List<StackElement>) : RuntimeException()