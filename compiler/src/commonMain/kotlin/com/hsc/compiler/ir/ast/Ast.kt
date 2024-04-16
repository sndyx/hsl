package com.hsc.compiler.ir.ast

import com.hsc.compiler.ir.action.ItemStack
import com.hsc.compiler.span.Span


data class Ast(
    val items: MutableList<Item>,
) {

    constructor() : this(mutableListOf())

}

enum class IdentKind {
    Global,
    Team,
    Player,
}

sealed class Ident(
    open var name: String
) {
    val isGlobal: Boolean get() = this is Global
    val isTeam: Boolean get() = this is Team
    val isPlayer: Boolean get() = this is Player

    data class Global(override var name: String) : Ident(name)
    data class Team(val team: String, override var name: String) : Ident(name)
    data class Player(override var name: String) : Ident(name)
}

data class Item(
    val span: Span,
    val ident: Ident,
    val kind: ItemKind,
) {
    fun deepCopy(): Item = copy(kind = kind.deepCopy())
}
sealed class ItemKind {

    abstract fun deepCopy(): ItemKind
    
    data class Fn(val fn: com.hsc.compiler.ir.ast.Fn) : ItemKind() {
        override fun deepCopy(): ItemKind = copy(fn = fn.deepCopy())
    }

    data class Enum(val enum: com.hsc.compiler.ir.ast.Enum) : ItemKind() {
        override fun deepCopy(): ItemKind = copy(enum = enum.deepCopy())
    }

    data class Const(val value: Expr) : ItemKind() {
        override fun deepCopy(): ItemKind = copy(value = value.deepCopy())
    }

    data class Artificial(val value: Stmt) : ItemKind() {
        override fun deepCopy(): ItemKind = copy(value = value.deepCopy())
    }

}

data class Fn(
    var processors: Processors?,
    var sig: FnSig,
    var block: Block,
) {
    fun deepCopy(): Fn = copy(processors = processors?.deepCopy(), sig = sig.deepCopy(), block = block.deepCopy())
}

data class Processors(
    val span: Span,
    val list: List<String>,
) {
    fun deepCopy(): Processors = copy(list = list.toList())
}

data class FnSig(
    val span: Span,
    var args: List<Ident>,
) {
    fun deepCopy(): FnSig = copy(args = args.toList())
}

data class Args(val span: Span, var args: MutableList<Expr>) {
    fun deepCopy(): Args = copy(args = args.toMutableList())

    fun isEmpty() = args.isEmpty()
}

data class Enum(
    val values: Map<String, Long>
) {
    fun deepCopy(): Enum = copy(values = values.toMap())
}

data class Block(
    val span: Span,
    var stmts: MutableList<Stmt>,
) {
    fun deepCopy(): Block = copy(stmts = stmts.map { it.deepCopy() }.toMutableList())
}

data class Stmt(
    val span: Span,
    var kind: StmtKind,
) {
    fun deepCopy(): Stmt = copy(kind = kind.deepCopy())
}

sealed class StmtKind {
    abstract fun deepCopy(): StmtKind

    data class Action(val action: com.hsc.compiler.ir.action.Action) : StmtKind() {
        override fun deepCopy(): StmtKind = copy()
    }

    data class For(var label: Ident, var range: Range, var block: Block) : StmtKind() {
        override fun deepCopy(): StmtKind = copy(range = range.deepCopy(), block = block.deepCopy())
    }
    data class While(var cond: com.hsc.compiler.ir.ast.Expr, var block: Block) : StmtKind() {
        override fun deepCopy(): StmtKind = copy(cond = cond.deepCopy(), block = block.deepCopy())
    }
    data class Assign(var ident: Ident, var expr: com.hsc.compiler.ir.ast.Expr) : StmtKind() {
        override fun deepCopy(): StmtKind = copy(expr = expr.deepCopy())
    }
    data class AssignOp(var kind: BinOpKind, var ident: Ident, var expr: com.hsc.compiler.ir.ast.Expr) : StmtKind() {
        override fun deepCopy(): StmtKind = copy(expr = expr.deepCopy())
    }
    data class Expr(var expr: com.hsc.compiler.ir.ast.Expr) : StmtKind() {
        override fun deepCopy(): StmtKind = copy(expr = expr.deepCopy())
    }
    data object Break : StmtKind() {
        override fun deepCopy(): StmtKind = this
    }
    data object Continue : StmtKind() {
        override fun deepCopy(): StmtKind = this
    }
    data class Ret(var expr: com.hsc.compiler.ir.ast.Expr?) : StmtKind() {
        override fun deepCopy(): StmtKind = copy(expr = expr?.deepCopy())
    }
}

data class Expr(
    var span: Span,
    var kind: ExprKind,
) {
    fun deepCopy(): Expr = copy(kind = kind.deepCopy())
}

sealed class ExprKind  {
    abstract fun deepCopy(): ExprKind
    data class Var(var ident: Ident) : ExprKind() {
        override fun deepCopy(): ExprKind = copy()
    }
    data class Call(var ident: Ident, var args: Args) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(args = args.deepCopy())
    }
    data class Binary(var kind: BinOpKind, var a: Expr, var b: Expr) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(a = a.deepCopy(), b = b.deepCopy())
    }
    data class Unary(var kind: UnaryOpKind, var expr: Expr) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(expr = expr.deepCopy())
    }
    data class Lit(var lit: com.hsc.compiler.ir.ast.Lit) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(lit = lit.deepCopy())
    }
    data class Range(var range: com.hsc.compiler.ir.ast.Range) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(range = range.deepCopy())
    }
    data class If(var expr: Expr, var block: com.hsc.compiler.ir.ast.Block, var other: com.hsc.compiler.ir.ast.Block?) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(expr = expr.deepCopy(), block = block.deepCopy(), other = other?.deepCopy())
    }
    data class Match(var expr: Expr, var arms: List<Arm>) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(expr = expr.deepCopy(), arms = arms.map { it.deepCopy() })
    }
    data class Block(var block: com.hsc.compiler.ir.ast.Block) : ExprKind() {
        override fun deepCopy(): ExprKind = copy(block = block.deepCopy())
    }
    data class Condition(val condition: com.hsc.compiler.ir.action.Condition) : ExprKind() {
        override fun deepCopy(): ExprKind = copy()
    }

    fun str(): String =
        when (this) {
            is Condition -> "condition"
            is Var -> "var"
            is Call -> "function call"
            is Binary -> "binary op"
            is Unary -> "unary op"
            is Range -> "range"
            is Lit -> when (this.lit) {
                is com.hsc.compiler.ir.ast.Lit.Bool -> "bool"
                is com.hsc.compiler.ir.ast.Lit.I64 -> "integer"
                is com.hsc.compiler.ir.ast.Lit.Str -> "string"
                is com.hsc.compiler.ir.ast.Lit.Item -> "item"
                else -> "literal"
            }
            is If -> "if"
            is Match -> "match"
            is Block -> "block"
        }

}

sealed class Lit {
    abstract fun deepCopy(): Lit
    data class Bool(val value: Boolean) : Lit() {
        override fun deepCopy(): Lit = copy()
    }
    
    data class Str(val value: String) : Lit() {
        override fun deepCopy(): Lit = copy()
    }
    
    data class I64(val value: Long) : Lit() {
        override fun deepCopy(): Lit = copy()
    }
    
    data class Item(val value: ItemStack) : Lit() {
        override fun deepCopy(): Lit = copy(value = value.copy())
    }
    
    data object Null : Lit() {
        override fun deepCopy(): Lit = this
    }

    fun str(): String =
        when (this) {
            is Str -> "string"
            is I64 -> "integer"
            is Item -> "item"
            is Bool -> "boolean"
            is Null -> "null"
        }

}

enum class BinOpKind(val value: String) {
    Add("+"), Sub("-"),
    Mul("*"), Div("/"),
    Pow("^"), Rem("%"),
    And("&&"), Or("||"),
    Eq("=="), Ne("!="),
    Lt("<"), Le("<="),
    Ge(">="), Gt(">"),
    In("in");
    override fun toString(): String = value
}

enum class UnaryOpKind {
    Not, Neg;
}

data class Arm(var query: List<Expr>, var value: Expr) {
    fun deepCopy(): Arm = copy(query = query.map { it.deepCopy() }, value = value.deepCopy())
}
data class Range(val lo: Expr, val hi: Expr) {
    fun deepCopy(): Range = copy(lo = lo.deepCopy(), hi = hi.deepCopy())
}