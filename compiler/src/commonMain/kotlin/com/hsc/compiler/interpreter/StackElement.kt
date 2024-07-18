package com.hsc.compiler.interpreter

import com.hsc.compiler.span.Span

data class StackElement(val function: String, val span: Span)