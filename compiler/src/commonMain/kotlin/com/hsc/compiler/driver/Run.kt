package com.hsc.compiler.driver

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.codegen.ActionTransformer
import com.hsc.compiler.codegen.generateHtsl
import com.hsc.compiler.errors.*
import com.hsc.compiler.ir.ast.Ast
import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.lower
import com.hsc.compiler.parse.Lexer
import com.hsc.compiler.parse.Parser
import com.hsc.compiler.parse.SourceProvider
import com.hsc.compiler.parse.TokenStream
import com.hsc.compiler.span.SourceMap
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.Path.Companion.toPath

private val json = Json { prettyPrint = true }
private val snbtp = StringifiedNbt { prettyPrint = true }
private val snbt = StringifiedNbt { }

fun runCompiler(opts: CompileOptions, files: List<Path>): Compiler = runBlocking {
    val compiler = Compiler(opts)
    val startTime = Clock.System.now()
    compiler.emitter.start(opts.houseName)

    val ast = compiler.enter {
        val ast = Ast()
        runBlocking {
            files.map { path ->
                launch(compiler.scope) {
                    val file = sm.loadFile(path)
                    val srcp = SourceProvider(file)
                    val dcx = DiagCtx(emitter, srcp)

                    val sess = CompileSess(dcx, opts, sm)

                    val lexer = Lexer(sess, srcp)

                    val tokenStream = TokenStream(lexer.iterator())
                    val parser = Parser(tokenStream, sess)

                    parser.parseCompletely(ast)
                }
            }
        }
        ast
    }

    val dcx = DiagCtx(compiler.emitter)
    val sess = CompileSess(dcx, opts, compiler.sm)
    compiler.enter {
        val lcx = LoweringCtx(ast, sess)
        lower(lcx)
    }

    val functions = compiler.enter {
        val transformer = ActionTransformer(sess)
        transformer.transform(ast)
    }

    compiler.enter<Unit> {
        opts.output?.let { outDir ->
            when (opts.target) {
                Target.Json -> {
                    if (FileSystem.SYSTEM.exists("$outDir/json".toPath())) {
                        FileSystem.SYSTEM.list("$outDir/json".toPath()).forEach { path ->
                            FileSystem.SYSTEM.delete(path)
                        }
                    }
                    FileSystem.SYSTEM.createDirectories("$outDir/json".toPath())
                    functions.map { fn -> Pair(json.encodeToString(fn.actions), fn.name) }.forEach { (text, name) ->
                        val path = "$outDir/json/$name.json".toPath()
                        FileSystem.SYSTEM.write(path) {
                            writeUtf8(text)
                        }
                    }
                    ast.items.filter { it.kind is ItemKind.Const }.forEach {
                        val path = "$outDir/json/${it.ident.name}.snbt".toPath()
                        val item = (((it.kind as ItemKind.Const).value.kind as ExprKind.Lit).lit as Lit.Item).value
                        FileSystem.SYSTEM.write(path) {
                            writeUtf8(snbtp.encodeToString(NbtCompound.serializer(), item.nbt))
                        }
                    }
                }
                Target.Htsl -> {
                    if (FileSystem.SYSTEM.exists("$outDir/htsl".toPath())) {
                        FileSystem.SYSTEM.list("$outDir/htsl".toPath()).forEach { path ->
                            FileSystem.SYSTEM.delete(path)
                        }
                    }
                    FileSystem.SYSTEM.createDirectories("$outDir/htsl".toPath())
                    functions.map { fn -> Pair(generateHtsl(sess, fn), fn.name) }.forEach { (text, name) ->
                        val path = "$outDir/htsl/$name.htsl".toPath()
                        FileSystem.SYSTEM.write(path) {
                            writeUtf8(text)
                        }
                    }
                    object : AstVisitor { // serialize & write all items
                        override fun visitLit(lit: Lit) {
                            if (lit !is Lit.Item) return
                            val str = snbt.encodeToString(NbtCompound.serializer(), lit.value.nbt)
                            val path = "$outDir/htsl/${lit.value.name}.json".toPath()
                            val jsonObject = buildJsonObject {
                                put("item", str)
                            }
                            FileSystem.SYSTEM.write(path) {
                                writeUtf8(Json.encodeToString(JsonObject.serializer(), jsonObject))
                            }
                        }
                    }.visitAst(ast)
                }
            }
        }
    }

    compiler.enter {
        val elapsed = Clock.System.now() - startTime
        emitter.complete(opts.houseName, elapsed)
    }

    compiler.emitter.close()

    compiler
}

class Compiler(opts: CompileOptions) {

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    val scope: CoroutineDispatcher = newSingleThreadContext("compiler")

    private val terminal: Terminal = when (opts.color) {
        Color.Auto -> Terminal()
        Color.Always -> Terminal(AnsiLevel.ANSI256)
        Color.Never -> Terminal(AnsiLevel.NONE)
    }

    val sm = SourceMap()

    val emitter: Emitter = when(opts.emitter) {
        EmitterType.Terminal -> HumanEmitter(terminal, sm)
        EmitterType.Minecraft -> HumanEmitter(terminal, sm)
        EmitterType.Internal -> DiagEmitter(sm)
        EmitterType.Webview -> WebviewEmitter
    }

    val dcx = DiagCtx(emitter)

    suspend fun <T : Any> enter(block: suspend Compiler.() -> T): T {
        try {
            val result = block()
            if (emitter.emittedError) fatal()
            return result
        } catch (diag: Diagnostic) {
            diag.emit()
            fatal()
        } catch (error: Throwable) {
            handleError(error, dcx)
            fatal()
        }
    }

    private fun fatal(): Nothing {
        exitProcess(0)
    }
}

private fun handleError(error: Throwable, dcx: DiagCtx) {
    error::class.simpleName?.formatError?.let { name ->
        when (name) {
            "error_stack_overflow" -> {
                if (dcx.srcp != null) {
                    dcx.err("stack overflow during macro expansion", dcx.srcp.virtualSpan, error).emit()
                } else {
                    dcx.err("stack overflow during compilation", null, error).emit()
                }
            }
            "error_out_of_memory" -> {
                dcx.err("out of memory").emit()
            }
            else -> {
                val message =
                    if (error.message != null) "$name: ${error.message!!.first().lowercase()}${error.message!!.drop(1)}"
                    else name
                dcx.bug(message, null, error).emit()
            }
        }
    }
}

private val String.formatError: String get() {
    val sb = StringBuilder()
    var first = true
    forEach {
        if (it.isUpperCase() && !first) sb.append('_')
        sb.append(it.lowercase())
        first = false
    }
    val string = sb.toString()
    val parts = string.split('_')
    return if (parts.lastOrNull() == "exception" || parts.lastOrNull() == "error") {
        "error_" + parts.dropLast(1).joinToString("_")
    } else string
}