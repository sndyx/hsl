import com.hsc.compiler.codegen.passes.AstPass
import com.hsc.compiler.codegen.passes.BlockAwareVisitor
import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Stmt
import kotlin.reflect.KClass

object LimitCheckPass : AstPass {

    override fun run(sess: CompileSess) {
        val functions = sess.map.query<Item>().filter { it.kind is ItemKind.Fn }
        functions.forEach {
            LimitCheckVisitor().visitItem(it)
        }
    }

}

private class LimitCheckVisitor : BlockAwareVisitor() {

    val quantities: MutableList<MutableMap<KClass<*>, String>> = mutableListOf()
    override fun visitBlock(block: Block) {
        quantities.add(mutableMapOf())
        super.visitBlock(block)
        quantities.removeLast()
    }

    override fun visitStmt(stmt: Stmt) {

    }

}