import com.hsc.compiler.codegen.limits
import com.hsc.compiler.codegen.passes.AstPass
import com.hsc.compiler.codegen.passes.BlockAwareVisitor
import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind

object LimitCheckPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            LimitCheckVisitor(sess).visitItem(it)
        }
    }

}

private class LimitCheckVisitor(val sess: CompileSess) : BlockAwareVisitor() {

    override fun visitBlock(block: Block) {
        val action = limits(block).entries.find { it.value < 0 }?.key
        if (action != null) {
            val err = sess.dcx().err("action limit surpassed: `$action`")
            err.spanLabel(block.span, "in this scope")
            if (sess.opts.mode != Mode.Strict) {
                err.note(Level.Error, "could not optimize out actions")
            }
            err.emit()
        }
        super.visitBlock(block)
    }

}