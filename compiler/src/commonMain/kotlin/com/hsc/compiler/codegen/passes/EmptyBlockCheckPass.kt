package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.Block

object EmptyBlockCheckPass : AstPass {

    override fun run(sess: CompileSess) {
        sess.map.query<Block>().forEach {
            if (it.stmts.isEmpty()) {
                val warn = sess.dcx().warn("empty block")
                warn.span(it.span.loSpan)
                warn.emit()
            }
        }
    }

}