package com.hsc.compiler.parse

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Level
import com.hsc.compiler.errors.CompileException
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span
import com.hsc.compiler.ir.ast.Lit
import kotlin.reflect.KClass

class Parser(
    private val stream: TokenStream,
    private val sess: CompileSess,
) {

    private var token: Token = Token.dummy()
    private var prev: Token = Token.dummy()
    private var fid: Int = 0

    init {
        bump()
        fid = token.span.fid // Fid of first token
    }

    private fun dcx(): DiagCtx = sess.dcx()

    fun parseCompletely(ast: Ast) {
        var item: Item? = null
        while (parseItem()?.also { item = it } != null) {
            ast.items.add(item!!)
        }
    }

    private fun parseItem(): Item? {
        return if (check(TokenKind.Eof::class)) {
            null
        } else if (eat(TokenKind.Kw(Keyword.Fn))) {
            parseFnItem()
        } else if (eat(TokenKind.Pound)) {
            parseProcessorFnItem()
        } else {
            tryRecoverInvalidItem()
        }
    }

    private fun parseProcessorFnItem(): Item {
        val lo = prev.span.lo
        val processors = if (check(TokenKind.OpenDelim(Delimiter.Bracket))) {
            parseDelimited(Delimiter.Bracket) {
                parseIdent().name
            }.getOrElse { e ->
                if (e !is CompileException) throw e
                e.diag.note(Level.Hint, "processor lists are declared `#[...]`")
                e.diag.emit()
                emptyList()
            }
        } else {
            listOf(parseIdent().name)
        }
        val hi = prev.span.hi
        return if (eat(TokenKind.Kw(Keyword.Fn))) {
            parseFnItem(Processors(Span(lo, hi, fid), processors))
        } else if (check(TokenKind.Ident::class)) {
            tryRecoverInvalidItem()
        } else {
            unexpected()
        }
    }

    private fun tryRecoverInvalidItem(): Item {
        if (token.kind is TokenKind.Ident) {
            val value = token.kind.ident.value
            val err = dcx().err("expected item, found ident")
            err.span(token.span)
            // Check for simple mistakes
            when (value) {
                "fun", "func", "function" -> {
                    err.note(Level.Hint, "functions are declared using `fn`")
                    err.emit()
                    // We can safely assume they wanted to declare a function and recover.
                    bump()
                    return parseFnItem()
                }
                "let", "val", "var" -> {
                    err.note(Level.Hint, "did you mean `#define`?")
                    // Don't recover this.
                    throw CompileException(err)
                }
            }
        }
        val err = dcx().err("expected item, found ${token.kind}")
        err.span(token.span)
        throw CompileException(err)
    }

    private fun parseFnItem(processors: Processors? = null): Item {
        val id = NodeId.from(sess.rootId)

        val lo = prev.span.lo
        val ident = parseIdent()
        val sig = parseFnSig()
        val block = parseBlock(id)
        val hi = prev.span.hi
        return Item(id, Span(lo, hi, fid), ident, ItemKind.Fn(Fn(processors, sig, block)))
    }

    private fun parseBlock(parent: NodeId): Block {
        val stmts = mutableListOf<Stmt>()
        val id = NodeId.from(parent)
        expect(TokenKind.OpenDelim(Delimiter.Brace))
        val startSpan = prev.span
        while (true) {
            if (eat(TokenKind.CloseDelim(Delimiter.Brace))) {
                break
            } else if (eat(TokenKind.Eof)) {
                val err = dcx().err("unclosed function body")
                err.span(startSpan)
                throw CompileException(err) // No reason to recover this
            } else {
                stmts.add(parseStmt(id))
            }
        }
        val hi = prev.span.hi
        return Block(id, Span(startSpan.lo, hi, fid), stmts)
    }

    private fun parseFnSig(): FnSig {
        val lo = token.span.lo
        val args = parseDelimited(Delimiter.Parenthesis) {
            parseIdent()
        }.getOrThrow()
        val hi = prev.span.hi
        return FnSig(Span(lo, hi, fid), args)
    }

    private fun parseStmt(parent: NodeId): Stmt {
        val id = NodeId.from(parent)
        val lo = token.span.lo

        if (eat(TokenKind.Kw(Keyword.For))) {
            return parseForLoop(id)
        } else if (eat(TokenKind.Kw(Keyword.While))) {
            return parseWhileLoop(id)
        } else if (check(TokenKind.Ident::class) || check(TokenKind.At::class)) {
            val ident = parseIdent()
            if (eat(TokenKind.Eq::class)) {
                val expr = parseExpr(id)
                return Stmt(id, Span(lo, prev.span.hi, fid), StmtKind.Assign(ident, expr))
            } else if (eat(TokenKind.BinOpEq::class)) {
                val kind = when (prev.kind.binOpEq.type) {
                    BinOpToken.Plus -> BinOpKind.Add
                    BinOpToken.Minus -> BinOpKind.Sub
                    BinOpToken.Star -> BinOpKind.Mul
                    BinOpToken.Slash -> BinOpKind.Div
                    BinOpToken.Percent -> BinOpKind.Rem
                    BinOpToken.Caret -> BinOpKind.Pow
                }
                val expr = parseExpr(id)
                return Stmt(id, Span(lo, prev.span.hi, fid), StmtKind.AssignOp(kind, ident, expr))
            } else if (check(TokenKind.OpenDelim(Delimiter.Parenthesis))) {
                val loArgs = token.span.lo
                val id2 = NodeId.from(id)
                val exprs = parseDelimited(Delimiter.Parenthesis) {
                    parseExpr(id2)
                }.getOrThrow().toMutableList()
                val hiArgs = prev.span.hi

                val expr = Expr(id2, Span(lo, prev.span.hi, fid), ExprKind.Call(ident, Args(Span(loArgs, hiArgs, fid), exprs)))
                return Stmt(id, Span(lo, prev.span.hi, fid), StmtKind.Expr(expr))
            } else {
                return Stmt(id, prev.span, StmtKind.Expr(Expr(NodeId.from(id), prev.span, ExprKind.Var(ident))))
            }
        } else if (eat(TokenKind.Kw(Keyword.Break))) {
            return Stmt(id, Span(lo, token.span.hi, fid), StmtKind.Break)
        } else if (eat(TokenKind.Kw(Keyword.Continue))) {
            return Stmt(id, Span(lo, token.span.hi, fid), StmtKind.Continue)
        } else if (eat(TokenKind.Kw(Keyword.Return))) {
            if (check(TokenKind.CloseDelim(Delimiter.Brace))) {
                return Stmt(id, Span(lo, token.span.hi, fid), StmtKind.Ret(null))
            } else {
                val expr = parseExpr(id)
                return Stmt(id, Span(lo, token.span.hi, fid), StmtKind.Ret(expr))
            }
        }
        val expr = parseExpr(id)
        return Stmt(id, Span(lo, prev.span.hi, fid), StmtKind.Expr(expr))
    }

    private fun parseForLoop(parent: NodeId): Stmt {
        val lo = prev.span.lo

        expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val ident = parseIdent()
        expect(TokenKind.Kw(Keyword.In))
        val range = parseRange(parent)
        expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        val block = parseBlock(parent)
        return Stmt(
            NodeId.from(parent), Span(lo, prev.span.hi, fid),
            StmtKind.For(ident, range, block)
        )
    }

    private fun parseWhileLoop(parent: NodeId): Stmt {
        val lo = prev.span.lo

        expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val cond = parseExpr(parent)
        expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        val block = parseBlock(parent)
        return Stmt(
            NodeId.from(parent), Span(lo, prev.span.hi, fid),
            StmtKind.While(cond, block)
        )
    }

    private fun parseExpr(parent: NodeId): Expr {
        // Simple shunting-yard impl
        val exprStack = mutableListOf(parseExpr0(parent))
        val opStack = mutableListOf<Pair<BinOpKind, Int>>()

        fun popStack() {
            val kind = opStack.removeLast().first
            val expr2 = exprStack.removeLast()
            val expr1 = exprStack.removeLast()
            val newExpr = Expr(
                NodeId.from(parent),
                Span(expr1.span.lo, expr2.span.hi, fid),
                ExprKind.Binary(kind, expr1, expr2)
            )
            exprStack.add(newExpr)
        }

        while (checkBinOp()) {
            val op = parseBinOp()
            while ((opStack.lastOrNull()?.second ?: -1) >= op.second) {
                popStack()
            }
            exprStack.add(parseExpr0(parent))
            opStack.add(op)
        }
        while (opStack.isNotEmpty()) {
            popStack()
        }

        return exprStack.single() // scary
    }

    private fun parseExpr0(parent: NodeId): Expr {
        val id = NodeId.from(parent)
        val lo = token.span.lo

        if (eat(TokenKind.BinOp(BinOpToken.Minus))) {
            val expr = parseExpr(id)
            return Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Unary(UnaryOpKind.Neg, expr))
        }
        if (check(TokenKind.Ident::class)) { // We are trolling with this one :-)
            if (token.kind.ident.value == "Item") {
                val lit = parseLiteral()
                return Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Lit(lit))
            }
        } else if (check(TokenKind.Literal::class)) {
            val lit = parseLiteral()
            return Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Lit(lit))
        }
        if (check(TokenKind.At::class) || check(TokenKind.Ident::class)) {
            val ident = parseIdent()
            return if (check(TokenKind.OpenDelim(Delimiter.Parenthesis))) {
                val loArgs = token.span.lo
                val exprs = parseDelimited(Delimiter.Parenthesis) {
                    parseExpr(id)
                }.getOrThrow().toMutableList()
                val hiArgs = prev.span.hi

                Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Call(ident, Args(Span(loArgs, hiArgs, fid), exprs)))
            } else {
                Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Var(ident))
            }
        }
        else if (eat(TokenKind.Kw(Keyword.If))) {
            return parseIf(id)
        } else if (eat(TokenKind.Kw(Keyword.Match))) {
            return parseMatch(id)
        } else if (check(TokenKind.OpenDelim(Delimiter.Brace))) {
            val block = parseBlock(id)
            return Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Block(block))
        } else if (eat(TokenKind.OpenDelim(Delimiter.Parenthesis))) {
            val expr = parseExpr(id)
            expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
            return Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Paren(expr))
        }
        val err = dcx().err("expected expression")
        err.span(token.span)
        throw CompileException(err)
    }

    private fun checkBinOp(): Boolean {
        return check(TokenKind.BinOp::class)
                || check(TokenKind.AndAnd)
                || check(TokenKind.OrOr)
                || check(TokenKind.Kw(Keyword.In))
                || check(TokenKind.Gt) || check(TokenKind.Ge)
                || check(TokenKind.EqEq)
                || check(TokenKind.Lt) || check(TokenKind.Le)
    }

    private fun parseBinOp(): Pair<BinOpKind, Int> {
        val kind = when (token.kind) {
            is TokenKind.BinOp -> when (token.kind.binOp.type) {
                BinOpToken.Plus -> Pair(BinOpKind.Add, 2)
                BinOpToken.Minus -> Pair(BinOpKind.Sub, 2)
                BinOpToken.Star -> Pair(BinOpKind.Mul, 3)
                BinOpToken.Slash -> Pair(BinOpKind.Div, 3)
                BinOpToken.Percent -> Pair(BinOpKind.Rem, 3)
                BinOpToken.Caret -> { Pair(BinOpKind.Pow, 4) }
            }

            TokenKind.AndAnd -> Pair(BinOpKind.And, 1)
            TokenKind.OrOr -> Pair(BinOpKind.Or, 0)
            TokenKind.Gt -> Pair(BinOpKind.Gt, 2)
            TokenKind.Ge -> Pair(BinOpKind.Ge, 2)
            TokenKind.EqEq -> Pair(BinOpKind.Eq, 2)
            TokenKind.Lt -> Pair(BinOpKind.Lt, 2)
            TokenKind.Le -> Pair(BinOpKind.Le, 2)
            else -> { error("unreachable") }
        }
        bump()
        return kind
    }

    private fun parseIf(id: NodeId): Expr {
        val lo = prev.span.lo
        var cond: Expr? = null
        runCatching {
            expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        }.onSuccess {
            cond = parseExpr(id)
            expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        }.onFailure { e ->
            if (e !is CompileException) throw e
            val start = token.span.lo
            var end = start
            eatUntil(TokenKind.OpenDelim(Delimiter.Brace)) {
                end = it.span.hi
            }
            e.diag.spans.clear()
            e.diag.spanLabel(Span(start, end, token.span.fid), "missing parenthesis")
            e.diag.emit()
            // Recover
        }
        val block = parseBlock(id)
        val other = if (eat(TokenKind.Kw(Keyword.Else))) {
            parseBlock(id)
        } else null
        return Expr(id, Span(lo, prev.span.hi, fid),
            ExprKind.If(cond ?: Expr(id, Span(0, 0, 0), ExprKind.Lit(Lit.I64(0))), block, other)
        )
    }

    private fun parseMatch(id: NodeId): Expr {
        val lo = prev.span.lo
        expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val expr = parseExpr(id)
        expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        val arms = parseDelimited(Delimiter.Brace) {
            parseMatchArm(id)
        }.getOrThrow()
        return Expr(id, Span(lo, prev.span.hi, fid), ExprKind.Match(expr, arms))
    }

    private fun parseMatchArm(id: NodeId): Arm {
        val exprs = mutableListOf<Expr>()
        while (true) {
            exprs.add(parseExpr(id))
            if (!eat(TokenKind.Comma::class)) {
                expect(TokenKind.FatArrow)
                break
            }
            if (eat(TokenKind.FatArrow::class)) break
        }
        val expr = parseExpr(id)
        return Arm(exprs, expr)
    }

    private fun parseRange(id: NodeId): Range {
        val start = token.span.lo
        try {
            val lo = parseExpr(id)
            expect(TokenKind.DotDot)
            val hi = parseExpr(id)
            return Range(lo, hi)
        } catch (e: CompileException) {
            // Ignore integer limit errors. We are only interested in transforming vague parse errors
            if (e.diag.message != "invalid integer") {
                // This is an overly complex way to compute the span. but I'm doing it anyway, in the case
                // that somebody uses something like python range syntax eg: `range(1, 100)`
                var end = start
                var depth = 0
                while (token.kind != TokenKind.Eof
                    && (token.kind != TokenKind.CloseDelim(Delimiter.Parenthesis) || depth > 0)
                ) {
                    if (token.kind == TokenKind.OpenDelim(Delimiter.Parenthesis)) depth++
                    if (token.kind == TokenKind.CloseDelim(Delimiter.Parenthesis)) depth--
                    end = token.span.hi
                    bump()
                }
                val err = dcx().err("expected range")
                err.span(Span(start, end, token.span.fid))
                err.note(Level.Hint, "ranges are declared `<low>..<high>`")
                throw CompileException(err)
            } else throw e
        }
    }

    private fun parseIdent(): Ident {
        val global = eat(TokenKind.At::class)
        expect(TokenKind.Ident::class)
        val name = prev.kind.ident.value
        return Ident(global, name)
    }

    private fun parseLiteral(): Lit {
        when (token.kind) {
            is TokenKind.Literal -> {
                val lit = token.kind.literal.lit
                return when (lit.kind) {
                    // :clueless: surely the lexer will give me a valid boolean
                    LitKind.Bool -> Lit.Bool(lit.value.toBooleanStrict()).also { bump() }
                    LitKind.I64 -> Lit.I64(parseI64())
                    LitKind.F64 -> Lit.I64(parseFloat())
                    LitKind.Str -> Lit.Str(lit.value).also { bump() }
                    LitKind.Null -> Lit.Null
                }
            }
            is TokenKind.Ident -> {
                if (token.kind.ident.value == "Item") {
                    bump()
                    return Lit.Item(parseItemStack())
                }
            }
            else -> { /* Ignore */ }
        }
        // We recover these 😎😎 (this could cause bad things...)
        val err = dcx().err("expected literal, found ${token.kind}")
        err.span(token.span)
        err.emit()
        return Lit.I64(0)
    }

    /**
     * Parses an item stack
     */
    private fun parseItemStack(): ItemStack {
        val span = prev.span

        expect(TokenKind.OpenDelim(Delimiter.Brace))
        var material: String? = null
        var name: String? = null
        var count: Int? = null
        var lore: List<String>? = null

        fun duplicateKey(name: String): Nothing {
            val err = dcx().err("duplicate key '${name}'")
            err.span(token.span)
            throw CompileException(err)
        }

        while (true) {
            when (token.kind) {
                is TokenKind.Ident -> {
                    bump()
                    val ident = prev.kind.ident.value // Who the FUCK cares if its global
                    expect(TokenKind.Colon::class)
                    when (ident) {
                        "material" -> {
                            if (material != null) duplicateKey("material")
                            material = parseString()
                        }
                        "name" -> {
                            if (name != null) duplicateKey("name")
                            name = parseString()
                        }
                        "count" -> {
                            if (count != null) duplicateKey("count")
                            count = parseI64().toInt()
                            if (count > 65 || count < 1) {
                                count = 1 // I'm a gangster for this one
                                val err = dcx().err("count must be in range 1..64")
                                err.span(token.span)
                                err.emit()
                            }
                        }
                        "lore" -> {
                            if (lore != null) duplicateKey("lore")
                            lore = parseDelimited(Delimiter.Bracket) {
                                parseString()
                            }.getOrThrow()
                        }
                        else -> {
                            val err = dcx().err("unknown key '${ident}'")
                            err.span(token.span)
                            err.note(Level.Hint, "valid keys are: ['material', 'name', 'count', 'lore']")
                            err.emit()
                        }
                    }
                }
                is TokenKind.CloseDelim -> {
                    if (token.kind.closeDelim.type == Delimiter.Brace) {
                        bump()
                        break
                    }
                    unexpected() // Why could they have put this here guys? Why?
                }
                else -> unexpected()
            }
        }

        if (material == null) {
            val err = dcx().err("missing required key 'material'")
            err.span(span) // Saved `Item` ident span
            err.note(Level.Hint, "material identifies the item type (eg: minecraft:diamond)")
            err.emit()
            material = ""
        }

        return ItemStack(material, name, count, lore)
    }

    private fun parseI64(): Long = parseLiteral(LitKind.I64).runCatching {
        toLong()
    }.getOrElse {
        val err = dcx().err("invalid integer")
        val hint = if (prev.kind.literal.lit.value.startsWith("-")) {
            "below 64-bit integer limit"
        } else "above 64-bit integer limit"
        err.spanLabel(prev.span, hint)
        throw CompileException(err)
    }
    private fun parseFloat(): Long = parseLiteral(LitKind.F64).runCatching {
        replace(".", "").toLong()
    }.getOrElse {
        val err = dcx().err("invalid float")
        err.span(prev.span)
        throw CompileException(err)
    } // We do some trolling... this now parses an int :-)
    private fun parseString(): String = parseLiteral(LitKind.Str)

    private fun <T> parseDelimited(delimiter: Delimiter, producer: () -> T): Result<List<T>> {
        expect(TokenKind.OpenDelim(delimiter))
        val list = mutableListOf<T>()
        while (true) {
            if (token.kind == TokenKind.CloseDelim(delimiter)) {
                bump()
                break
            } else {
                try {
                    list.add(producer())
                } catch (e: CompileException) {
                    // Parsing failed, find end of delimited section
                    eatUntil(TokenKind.CloseDelim(delimiter))
                    return Result.failure(e)
                }
                if (!eat(TokenKind.Comma::class)) { // If no comma, list must end
                    assert(TokenKind.CloseDelim(delimiter))
                }
            }
        }
        // Bracket closed
        return Result.success(list)
    }

    private fun parseLiteral(kind: LitKind): String {
        // Muh muh parse literal muh muh
        // mfw when i stumble back here looking for the bump call again:
        // https://www.youtube.com/watch?v=lmbJP1yObZc
        when (token.kind) {
            is TokenKind.Literal -> {
                if (token.kind.literal.lit.kind == kind) {
                    return token.kind.literal.lit.value.also {
                        bump()
                    }
                }
            }
            else -> { /* Ignore */ }
        }
        val err = dcx().err("expected ${kind}, found ${token.kind}")
        err.span(token.span)
        throw CompileException(err)
    }

    private fun unexpected(): Nothing {
        val err = dcx().err("unexpected token ${token.kind}")
        err.span(token.span)
        throw CompileException(err)
    }

    /**
     * Raises an error if the current token doesn't match a given [kind].
     */
    private fun <T : TokenKind> assert(kind: KClass<T>) {
        if (!kind.isInstance(token.kind)) {
            val err = dcx().err("expected ${kind.name}, found ${token.kind}")
            err.span(token.span)
            throw CompileException(err)
        }
    }

    /**
     * Raises an error if the current token doesn't match a given [kind].
     */
    private fun assert(kind: TokenKind) {
        if (token.kind != kind) {
            val err = dcx().err("expected ${kind}, found ${token.kind}")
            err.span(token.span)
            throw CompileException(err)
        }
    }

    /**
     * Checks if the current token matches [kind].
     */
    private fun <T : TokenKind> check(kind: KClass<T>): Boolean =
        kind.isInstance(token.kind)

    /**
     * Checks if the current token matches [kind].
     */
    private fun check(kind: TokenKind): Boolean = token.kind == kind

    /**
     * Eats a token and raises an error if it doesn't match a given [kind].
     */
    private fun <T : TokenKind> expect(kind: KClass<T>) {
        if (!kind.isInstance(token.kind)) {
            val err = dcx().err("expected ${kind.name}, found ${token.kind}")
            err.spanLabel(token.span, "unexpected token")
            throw CompileException(err)
        }
        bump()
    }

    /**
     * Eats a token and raises an error if it doesn't match a given [kind].
     */
    private fun expect(kind: TokenKind) {
        if (token.kind != kind) {
            val err = dcx().err("expected ${kind}, found ${token.kind}")
            err.spanLabel(token.span, "unexpected token")
            throw CompileException(err)
        }
        bump()
    }

    private fun <T : TokenKind> eatUntil(kind: KClass<T>, consumer: (Token) -> Unit = {}) {
        while (!kind.isInstance(token.kind) && token.kind != TokenKind.Eof) {
            consumer(token)
            bump()
        }
    }

    private fun eatUntil(kind: TokenKind, consumer: (Token) -> Unit = {}) {
        while (token.kind != kind && token.kind != TokenKind.Eof) {
            consumer(token)
            bump()
        }
    }

    /**
     * Eats a token if it matches [kind] and returns whether it ate a token.
     */
    private fun <T : TokenKind> eat(kind: KClass<T>): Boolean =
        kind.isInstance(token.kind).also { if (it) bump() }


    /**
     * Eats a token if it matches [kind] and returns whether it ate a token.
     */
    private fun eat(kind: TokenKind): Boolean =
        (token.kind == kind).also {
            if (it) bump()
        }

    private fun bump() {
        prev = token
        token = if (stream.hasNext()) {
            stream.next()
        } else {
            // This is hacky, but This will have to do for now
            Token(TokenKind.Eof, Span.single(prev.span.hi + 1, prev.span.fid))
        }
    }

}