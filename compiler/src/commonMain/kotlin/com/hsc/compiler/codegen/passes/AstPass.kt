package com.hsc.compiler.codegen.passes

import com.hsc.compiler.driver.CompileSess

interface AstPass {

    fun run(sess: CompileSess)

}