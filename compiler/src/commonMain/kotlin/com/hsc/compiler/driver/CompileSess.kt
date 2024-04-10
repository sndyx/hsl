package com.hsc.compiler.driver

import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.span.SourceMap

class CompileSess (
    private val dcx: DiagCtx,
    val opts: CompileOptions,
    val sourceMap: SourceMap,
) {

    fun dcx(): DiagCtx = dcx

}