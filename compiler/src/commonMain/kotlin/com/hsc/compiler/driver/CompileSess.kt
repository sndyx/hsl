package com.hsc.compiler.driver

import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.NodeId
import com.hsc.compiler.span.SourceMap

class CompileSess (
    private val dcx: DiagCtx,
    val opts: CompileOptions,
    val sourceMap: SourceMap,
) {

    val rootId: NodeId = NodeId(0uL, 0uL)

    fun dcx(): DiagCtx = dcx

}