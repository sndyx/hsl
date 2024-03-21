package com.hsc.compiler.codegen.passes

import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.Block
import com.hsc.compiler.ir.ast.Stmt

open class BlockAwareVisitor : AstVisitor {

    private val blocks = mutableListOf<Block>()
    private val positions = mutableListOf<Int>()

    // These should not fail, probably
    val currentBlock: Block get() = blocks.last()
    val currentPosition: Int get() = positions.last()

    override fun visitBlock(block: Block) {
        blocks.add(block)
        positions.add(0)
        super.visitBlock(block)
        blocks.removeLast()
        positions.removeLast()
    }

    /**
     * ALWAYS call super.visitStmt() when overriding!
     */
    override fun visitStmt(stmt: Stmt) {
        // If this ever fails... It's not our fault
        super.visitStmt(stmt)
        positions.add(positions.removeLast() + 1)
    }

    fun added(amount: Int) {
        positions.add(positions.removeLast() + amount)
    }

}