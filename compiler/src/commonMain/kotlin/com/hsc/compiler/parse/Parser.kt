@file:Suppress("MemberVisibilityCanBePrivate")

package com.hsc.compiler.parse

import com.hsc.compiler.driver.CompileSess
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import com.hsc.compiler.ir.action.ItemStack
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.span.Span
import com.hsc.compiler.ir.ast.Lit
import kotlinx.serialization.decodeFromString
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt
import kotlin.reflect.KClass

class Parser(
    val stream: TokenStream,
    val sess: CompileSess,
) {

    private val snbt by lazy {
        StringifiedNbt { }
    }

    private var _token: Token? = null
    val token: Token get() = if (_token == null) {
        bump0()
        _token!!
    } else _token!!
    var prev: Token = Token.dummy()
    var fid: Int = 0

    init {
        fid = token.span.fid // Fid of first token
    }

    fun dcx(): DiagCtx = sess.dcx()

    fun parseCompletely(ast: Ast) {
        var item: Item? = null
        while (parseItem()?.also { item = it } != null) {
            ast.items.add(item!!)
        }
    }

    fun parseItem(): Item? {
        return if (check(TokenKind.Eof::class)) {
            null
        } else if (check(TokenKind.Ident::class)) {
            parseProcessorFnItem()
        } else if (eat(TokenKind.Kw(Keyword.Fn))) {
            parseFnItem()
        } else if (eat(TokenKind.Kw(Keyword.Enum))) {
            parseEnumItem()
        } else if (eat(TokenKind.Kw(Keyword.Const))) {
            parseConstItem()
        } else {
            tryRecoverInvalidItem()
        }
    }

    fun parseProcessorFnItem(): Item {
        val lo = prev.span.lo
        val processors = if (check(TokenKind.OpenDelim(Delimiter.Bracket))) {
            parseDelimitedCommaSequence(Delimiter.Bracket) {
                removeMacroPrefix(parseIdent())
            }.getOrElse { e ->
                if (e !is Diagnostic) throw e
                e.note(Level.Hint, "processor lists are declared `#[...]`")
                e.emit()
                emptyList()
            }
        } else {
            listOf(removeMacroPrefix(parseIdent()))
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

    private fun removeMacroPrefix(ident: Ident): String {
        if (!ident.name.startsWith("#")) {
            throw sess.dcx().err("expected item, found ident", prev.span)
        }
        return ident.name.drop(1)
    }

    fun tryRecoverInvalidItem(): Item {
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
                    throw err
                }
            }
        }
        val err = dcx().err("expected item, found ${token.kind}")
        err.span(token.span)
        throw err
    }

    fun parseFnItem(processors: Processors? = null): Item {

        val lo = prev.span.lo
        val ident = parseIdent()
        val sig = parseFnSig()
        val block = parseBlock()
        val hi = prev.span.hi
        return Item(Span(lo, hi, fid), ident, ItemKind.Fn(Fn(processors, sig, block)))
    }

    fun parseEnumItem(): Item {
        val lo = prev.span.lo
        val ident = parseIdent()
        expect(TokenKind.OpenDelim(Delimiter.Brace))
        val map = buildMap {
            while (!eat(TokenKind.CloseDelim(Delimiter.Brace))) {
                val name = parseRawIdent()
                expect(TokenKind.Eq)
                val value = parseI64()
                put(name, value)
            }
        }
        val hi = prev.span.hi
        return Item(Span(lo, hi, fid), ident, ItemKind.Enum(Enum(map)))
    }

    fun parseConstItem(): Item {
        val lo = prev.span.lo
        val ident = parseIdent()
        expect(TokenKind.Eq)
        val expr = parseExpr()
        val hi = prev.span.hi
        return Item(Span(lo, hi, fid), ident, ItemKind.Const(expr))
    }

    fun parseBlock(): Block {
        val startSpan = token.span
        val stmts = parseDelimitedSequence(Delimiter.Brace) { parseStmt() }.getOrThrow()


        val hi = prev.span.hi

        return Block(Span(startSpan.lo, hi, fid), stmts.toMutableList())
    }

    fun parseFnSig(): FnSig {
        val lo = token.span.lo
        val args = parseDelimitedCommaSequence(Delimiter.Parenthesis) {
            parseIdent()
        }.getOrThrow()
        val hi = prev.span.hi
        return FnSig(Span(lo, hi, fid), args)
    }

    fun parseStmt(): Stmt {
        val lo = token.span.lo

        if (eat(TokenKind.Kw(Keyword.For))) {
            return parseForLoop()
        } else if (eat(TokenKind.Kw(Keyword.While))) {
            return parseWhileLoop()
        } else if (check(TokenKind.Ident::class) || check(TokenKind.At::class)) {
            val ident = parseIdent()
            if (eat(TokenKind.Eq::class)) {
                val expr = parseExpr()
                return Stmt(Span(lo, prev.span.hi, fid), StmtKind.Assign(ident, expr))
            } else if (eat(TokenKind.BinOpEq::class)) {
                val kind = when (prev.kind.binOpEq.type) {
                    BinOpToken.Plus -> BinOpKind.Add
                    BinOpToken.Minus -> BinOpKind.Sub
                    BinOpToken.Star -> BinOpKind.Mul
                    BinOpToken.Slash -> BinOpKind.Div
                    BinOpToken.Percent -> BinOpKind.Rem
                    BinOpToken.Caret -> BinOpKind.Pow
                }
                val expr = parseExpr()
                return Stmt(Span(lo, prev.span.hi, fid), StmtKind.AssignOp(kind, ident, expr))
            } else if (check(TokenKind.OpenDelim(Delimiter.Parenthesis))) {
                val loArgs = token.span.lo
                val exprs = parseDelimitedCommaSequence(Delimiter.Parenthesis) {
                    parseExpr()
                }.getOrThrow().toMutableList()
                val hiArgs = prev.span.hi

                val expr = Expr(Span(lo, prev.span.hi, fid), ExprKind.Call(ident, Args(Span(loArgs, hiArgs, fid), exprs)))
                return Stmt(Span(lo, prev.span.hi, fid), StmtKind.Expr(expr))
            } else {
                val wholeExpr = parseExpr(Expr(prev.span, ExprKind.Var(ident)))
                return Stmt(wholeExpr.span, StmtKind.Expr(wholeExpr))
            }
        } else if (eat(TokenKind.Kw(Keyword.Break))) {
            return Stmt(Span(lo, token.span.hi, fid), StmtKind.Break)
        } else if (eat(TokenKind.Kw(Keyword.Continue))) {
            return Stmt(Span(lo, token.span.hi, fid), StmtKind.Continue)
        } else if (eat(TokenKind.Kw(Keyword.Return))) {
            if (check(TokenKind.CloseDelim(Delimiter.Brace))) {
                return Stmt(Span(lo, token.span.hi, fid), StmtKind.Ret(null))
            } else {
                val expr = parseExpr()
                return Stmt(Span(lo, token.span.hi, fid), StmtKind.Ret(expr))
            }
        } else if (eat(TokenKind.Kw(Keyword.Random))) {
            return parseRandom()
        }
        val expr = parseExpr()
        return Stmt(Span(lo, prev.span.hi, fid), StmtKind.Expr(expr))
    }

    fun parseForLoop(): Stmt {
        val lo = prev.span.lo

        expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val ident = parseIdent()
        expect(TokenKind.Kw(Keyword.In))
        val range = parseRange()
        expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        val block = parseBlock()
        return Stmt(
            Span(lo, prev.span.hi, fid),
            StmtKind.For(ident, range, block)
        )
    }

    fun parseWhileLoop(): Stmt {
        val lo = prev.span.lo

        expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val cond = parseExpr()
        expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        val block = parseBlock()
        return Stmt(
            Span(lo, prev.span.hi, fid),
            StmtKind.While(cond, block)
        )
    }

    fun parseExpr(lookahead: Expr? = null, parseCondition: Boolean = true): Expr {
        val expr = parseExpr0(lookahead, parseCondition = parseCondition)
        if (eat(TokenKind.Kw(Keyword.In))) {
            val other = parseExpr(parseCondition = parseCondition)
            return Expr(
                Span(expr.span.lo, other.span.hi, fid),
                ExprKind.Binary(BinOpKind.In, expr, other)
            )
        }
        if (eat(TokenKind.DotDot)) {
            val other = parseExpr0(parseCondition = parseCondition) // We should stop chained ranges, probably
            val range = Range(expr, other)
            return Expr(
                Span(expr.span.lo, other.span.hi, fid),
                ExprKind.Range(range)
            )
        } else {
            return expr
        }
    }

    private fun parseExpr0(lookahead: Expr? = null, parseCondition: Boolean = true): Expr {
        // Simple shunting-yard impl
        val exprStack = mutableListOf(lookahead ?: parseExpr00())
        val opStack = mutableListOf<Pair<BinOpKind, Int>>()

        fun popStack() {
            val kind = opStack.removeLast().first
            val expr2 = exprStack.removeLast()
            val expr1 = exprStack.removeLast()
            val newExpr = Expr(
                Span(expr1.span.lo, expr2.span.hi, fid),
                ExprKind.Binary(kind, expr1, expr2)
            )
            exprStack.add(newExpr)
        }

        while (checkBinOp(parseCondition)) {
            val op = parseBinOp()
            while ((opStack.lastOrNull()?.second ?: -1) >= op.second) {
                popStack()
            }
            exprStack.add(parseExpr00())
            opStack.add(op)
        }
        while (opStack.isNotEmpty()) {
            popStack()
        }

        return exprStack.single()
    }

    // parseExpr000 when
    private fun parseExpr00(): Expr {
        val lo = token.span.lo

        if (eat(TokenKind.BinOp(BinOpToken.Minus))) {
            val expr = parseExpr00()
            return Expr(Span(lo, prev.span.hi, fid), ExprKind.Unary(UnaryOpKind.Neg, expr))
        }
        if (check(TokenKind.Ident::class)) { // We are trolling with this one :-)
            if (token.kind.ident.value == "Item") {
                val lit = parseLiteral()
                return Expr(Span(lo, prev.span.hi, fid), ExprKind.Lit(lit))
            }
        } else if (check(TokenKind.Literal::class) || check(TokenKind.Lt::class)) { // <
            val lit = parseLiteral()
            return Expr(Span(lo, prev.span.hi, fid), ExprKind.Lit(lit))
        }
        if (check(TokenKind.At::class) || check(TokenKind.Ident::class)) {
            val ident = parseIdent()
            return if (check(TokenKind.OpenDelim(Delimiter.Parenthesis))) {
                val loArgs = token.span.lo
                val exprs = parseDelimitedCommaSequence(Delimiter.Parenthesis) {
                    parseExpr()
                }.getOrThrow().toMutableList()
                val hiArgs = prev.span.hi

                Expr(Span(lo, prev.span.hi, fid), ExprKind.Call(ident, Args(Span(loArgs, hiArgs, fid), exprs)))
            } else {
                Expr(Span(lo, prev.span.hi, fid), ExprKind.Var(ident))
            }
        }
        else if (eat(TokenKind.Kw(Keyword.If))) {
            return parseIf()
        } else if (eat(TokenKind.Kw(Keyword.Match))) {
            return parseMatch()
        } else if (check(TokenKind.OpenDelim(Delimiter.Brace))) {
            val block = parseBlock()
            return Expr(Span(lo, prev.span.hi, fid), ExprKind.Block(block))
        } else if (eat(TokenKind.OpenDelim(Delimiter.Parenthesis))) {
            val expr = parseExpr()
            expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
            return expr
        }
        throw dcx().err("expected expression", token.span)
    }

    fun checkBinOp(parseCondition: Boolean): Boolean {
        // Don't handle in here, it sucks :(
        return check(TokenKind.BinOp::class)
                || check(TokenKind.AndAnd)
                || check(TokenKind.OrOr)
                || ((check(TokenKind.Gt) || check(TokenKind.Ge)
                || check(TokenKind.EqEq)
                || check(TokenKind.Lt) || check(TokenKind.Le))
                && parseCondition)
    }

    fun parseBinOp(): Pair<BinOpKind, Int> {
        val kind = when (token.kind) {
            is TokenKind.BinOp -> when (token.kind.binOp.type) {
                BinOpToken.Plus -> Pair(BinOpKind.Add, 4)
                BinOpToken.Minus -> Pair(BinOpKind.Sub, 4)
                BinOpToken.Star -> Pair(BinOpKind.Mul, 5)
                BinOpToken.Slash -> Pair(BinOpKind.Div, 5)
                BinOpToken.Percent -> Pair(BinOpKind.Rem, 5)
                BinOpToken.Caret -> { Pair(BinOpKind.Pow, 6) }
            }

            TokenKind.AndAnd -> Pair(BinOpKind.And, 1)
            TokenKind.OrOr -> Pair(BinOpKind.Or, 0)
            TokenKind.Gt -> Pair(BinOpKind.Gt, 2)
            TokenKind.Ge -> Pair(BinOpKind.Ge, 2)
            TokenKind.EqEq -> Pair(BinOpKind.Eq, 2)
            TokenKind.Lt -> Pair(BinOpKind.Lt, 2)
            TokenKind.Le -> Pair(BinOpKind.Le, 2)
            else -> error("unreachable bin op")
        }
        bump()
        return kind
    }

    fun parseIf(): Expr {
        val lo = prev.span.lo
        var cond: Expr? = null
        runCatching {
            expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        }.onSuccess {
            cond = parseExpr()
            try {
                expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
            } catch (e: Diagnostic) {
                if (check(TokenKind.Eq)) {
                    e.note(Level.Hint, "did you mean `==`?")
                } else if (check(TokenKind.Ident("and"))) {
                    e.note(Level.Hint, "did you mean `&&`?")
                } else if (check(TokenKind.Ident("or"))) {
                    e.note(Level.Hint, "did you mean `||`?")
                } else {
                    throw e
                }
                e.emit()
                eatUntil(TokenKind.CloseDelim(Delimiter.Parenthesis))
                bump() // bump )
            }
        }.onFailure { e ->
            if (e !is Diagnostic) throw e
            val start = token.span.lo
            var end = start
            eatUntil(TokenKind.OpenDelim(Delimiter.Brace)) {
                end = it.span.hi
            }
            e.spans.clear()
            e.spanLabel(Span(start, end, token.span.fid), "missing parenthesis")
            e.emit()
            // Recover
        }
        val block = if (check(TokenKind.OpenDelim(Delimiter.Brace))) {
            parseBlock()
        } else { // single stmt body
            val stmt = parseStmt()
            Block(stmt.span, mutableListOf(stmt))
        }
        val other = if (eat(TokenKind.Kw(Keyword.Else))) {
            if (check(TokenKind.OpenDelim(Delimiter.Brace))) {
                parseBlock()
            } else {
                val stmt = parseStmt()
                Block(stmt.span, mutableListOf(stmt))
            }
        } else null
        return Expr(Span(lo, prev.span.hi, fid),
            ExprKind.If(cond ?: Expr(Span(0, 0, 0), ExprKind.Lit(Lit.I64(0))), block, other)
        )
    }

    fun parseMatch(): Expr {
        val lo = prev.span.lo
        expect(TokenKind.OpenDelim(Delimiter.Parenthesis))
        val expr = parseExpr()
        expect(TokenKind.CloseDelim(Delimiter.Parenthesis))
        val arms = parseDelimitedSequence(Delimiter.Brace) {
            parseMatchArm()
        }.getOrThrow()
        return Expr(Span(lo, prev.span.hi, fid), ExprKind.Match(expr, arms))
    }

    fun parseMatchArm(): Arm {
        val exprs = mutableListOf<Expr>()
        while (true) {
            exprs.add(parseExpr())
            if (!eat(TokenKind.Comma::class)) {
                expect(TokenKind.FatArrow)
                break
            }
            if (eat(TokenKind.FatArrow::class)) break
        }
        val expr = parseExpr()
        return Arm(exprs, expr)
    }

    fun parseRandom(): Stmt {
        val lo = prev.span.lo
        val block = parseBlock()
        val hi = prev.span.hi
        return Stmt(Span(lo, hi, fid), StmtKind.Random(block))
    }

    fun parseRange(): Range {
        val start = token.span.lo
        try {
            val lo = parseExpr()
            expect(TokenKind.DotDot)
            val hi = parseExpr()
            return Range(lo, hi)
        } catch (e: Diagnostic) {
            // Ignore integer limit errors. We are only interested in transforming vague parse errors
            if (e.message != "invalid integer") {
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
                throw err
            } else throw e
        }
    }

    fun parseRawIdent(): String {
        if (check(TokenKind.At)) {
            sess.dcx().err("expected name, found global ident").emit()
        }
        expect(TokenKind.Ident::class)
        val name = prev.kind.ident.value
        return name
    }

    fun parseIdent(): Ident {
        val global = eat(TokenKind.At::class)
        expect(TokenKind.Ident::class)
        val first = prev.kind.ident.value
        return if (!global) {
            if (eat(TokenKind.Dot::class)) {
                expect(TokenKind.Ident::class)
                val name = prev.kind.ident.value
                Ident.Team(first, name)
            } else Ident.Player(first)
        } else Ident.Global(first)
    }

    fun parseLiteral(): Lit {
        when (token.kind) {
            is TokenKind.Literal -> {
                val lit = token.kind.literal.lit
                return when (lit.kind) {
                    // :clueless: surely the lexer will give me a valid boolean
                    LitKind.Bool -> Lit.Bool(lit.value.toBooleanStrict()).also { bump() }
                    LitKind.I64 -> Lit.I64(parseI64())
                    LitKind.F64 -> Lit.F64(parseFloat())
                    LitKind.Str -> Lit.Str(lit.value).also { bump() }
                    LitKind.Item -> runCatching {
                        Lit.Item(
                            ItemStack(snbt.decodeFromString<NbtCompound>(lit.value))
                        )
                    }.getOrElse {
                        throw dcx().err("error parsing item JSON: ${it.message}", token.span)
                    }.also { bump() }
                    LitKind.Null -> Lit.Null
                }
            }
            else -> {
                if (token.kind == TokenKind.Lt) {
                    return parseLocation()
                }
            }
        }
        // We recover these 😎😎 (this could cause bad things...)
        val err = dcx().err("expected literal, found ${token.kind}")
        err.span(token.span)
        err.emit()
        return Lit.I64(0)
    }

    fun parseI64(): Long = parseLiteral(LitKind.I64).runCatching {
        toLong()
    }.getOrElse {
        val err = dcx().err("invalid integer")
        val hint = if (prev.kind.literal.lit.value.startsWith("-")) {
            "below 64-bit integer limit"
        } else "above 64-bit integer limit"
        err.spanLabel(prev.span, hint)
        throw err
    }
    fun parseFloat(): Double = parseLiteral(LitKind.F64).runCatching {
        toDouble()
    }.getOrElse {
        val err = dcx().err("invalid float")
        err.span(prev.span)
        throw err
    } // We do some trolling... this now parses an int :-)
    fun parseString(): String = parseLiteral(LitKind.Str)
    fun parseNumber(): Double = try { parseI64().toDouble() } catch (e: Diagnostic) { parseFloat() }

    fun <T> parseDelimitedCommaSequence(delimiter: Delimiter, producer: () -> T): Result<List<T>> {
        expect(TokenKind.OpenDelim(delimiter))
        val list = mutableListOf<T>()
        while (true) {
            if (token.kind == TokenKind.CloseDelim(delimiter)) {
                bump()
                break
            } else if (token.kind == TokenKind.Eof) {
                unexpected()
            } else {
                try {
                    list.add(producer())
                } catch (e: Diagnostic) {
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

    fun parseLocation(): Lit {
        bump() // bump <
        var cont = false
        val xyz = (0..2).map { i ->
            Pair(
                eat(TokenKind.Tilde),
                runCatching { parseExpr(parseCondition = false) }.getOrNull(),
            ).also {
                if (i != 2) expect(TokenKind.Comma)
                else cont = eat(TokenKind.Comma)
            }
        }

        if (!cont) return Lit.Location(
            xyz[0].first, xyz[1].first, xyz[2].first,
            xyz[0].second, xyz[1].second, xyz[2].second,
            null, null,
        ).also { expect(TokenKind.Gt) } // >

        val pitchYaw = (0..1).map { i ->
            runCatching { parseExpr(parseCondition = false) }.getOrNull().also {
                if (i == 0) expect(TokenKind.Comma)
                else expect(TokenKind.Gt) // >
            }
        }

        return Lit.Location(
            xyz[0].first, xyz[1].first, xyz[2].first,
            xyz[0].second, xyz[1].second, xyz[2].second,
            pitchYaw[0], pitchYaw[1],
        )
    }

    fun <T> parseDelimitedSequence(delimiter: Delimiter, producer: () -> T): Result<List<T>> {
        expect(TokenKind.OpenDelim(delimiter))
        val list = mutableListOf<T>()
        while (true) {
            if (token.kind == TokenKind.CloseDelim(delimiter)) {
                bump()
                break
            } else if (token.kind == TokenKind.Eof) {
                unexpected()
            } else {
                try {
                    list.add(producer())
                } catch (e: Diagnostic) {
                    // Parsing failed, find end of delimited section
                    eatUntil(TokenKind.CloseDelim(delimiter))
                    return Result.failure(e)
                }
            }
        }
        // Bracket closed
        return Result.success(list)
    }

    fun parseLiteral(kind: LitKind): String {
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
        throw dcx().err("expected ${kind}, found ${token.kind}", token.span)
    }

    fun unexpected(): Nothing {
        throw dcx().err("unexpected token ${token.kind}", token.span)
    }

    /**
     * Raises an error if the current token doesn't match a given [kind].
     */
    fun <T : TokenKind> assert(kind: KClass<T>) {
        if (!kind.isInstance(token.kind)) {
            throw dcx().err("expected ${kind.name}, found ${token.kind}", token.span)
        }
    }

    /**
     * Raises an error if the current token doesn't match a given [kind].
     */
    fun assert(kind: TokenKind) {
        if (token.kind != kind) {
            throw dcx().err("expected ${kind}, found ${token.kind}", token.span)
        }
    }

    /**
     * Checks if the current token matches [kind].
     */
    fun <T : TokenKind> check(kind: KClass<T>): Boolean =
        kind.isInstance(token.kind)

    /**
     * Checks if the current token matches [kind].
     */
    fun check(kind: TokenKind): Boolean = token.kind == kind

    /**
     * Eats a token and raises an error if it doesn't match a given [kind].
     */
    fun <T : TokenKind> expect(kind: KClass<T>) {
        if (!kind.isInstance(token.kind)) {
            val err = dcx().err("expected ${kind.name}, found ${token.kind}")
            err.spanLabel(token.span, "unexpected token")
            throw err
        }
        bump()
    }

    /**
     * Eats a token and raises an error if it doesn't match a given [kind].
     */
    fun expect(kind: TokenKind) {
        if (token.kind != kind) {
            val err = dcx().err("expected ${kind}, found ${token.kind}")
            err.spanLabel(token.span, "unexpected token")
            throw err
        }
        bump()
    }

    fun <T : TokenKind> eatUntil(kind: KClass<T>, consumer: (Token) -> Unit = {}) {
        while (!kind.isInstance(token.kind) && token.kind != TokenKind.Eof) {
            consumer(token)
            bump()
        }
    }

    fun eatUntil(kind: TokenKind, consumer: (Token) -> Unit = {}) {
        while (token.kind != kind && token.kind != TokenKind.Eof) {
            consumer(token)
            bump()
        }
    }

    /**
     * Eats a token if it matches [kind] and returns whether it ate a token.
     */
    fun <T : TokenKind> eat(kind: KClass<T>): Boolean =
        kind.isInstance(token.kind).also { if (it) bump() }


    /**
     * Eats a token if it matches [kind] and returns whether it ate a token.
     */
    fun eat(kind: TokenKind): Boolean =
        (token.kind == kind).also {
            if (it) bump()
        }

    /**
     * Lazily bumps the current [Token].
     */
    fun bump() {
        prev = token
        _token = null
    }

    private fun bump0() {
        _token =
            if (stream.hasNext()) {
                stream.next()
            } else {
                // This is hacky, but This will have to do for now
                Token(TokenKind.Eof, Span.single(prev.span.hi + 1, prev.span.fid))
            }
    }

}