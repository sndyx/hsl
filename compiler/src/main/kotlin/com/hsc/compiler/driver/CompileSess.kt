package com.hsc.compiler.driver

import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.ir.ast.AstMap
import com.hsc.compiler.ir.ast.NodeId
import com.hsc.compiler.span.SourceMap

class CompileSess (
    private val dcx: DiagCtx,
    val sourceMap: SourceMap,
    val map: AstMap,
) {

    val rootId: NodeId = NodeId(0uL, 0uL)
    var failed = false

    fun dcx(): DiagCtx = dcx

}