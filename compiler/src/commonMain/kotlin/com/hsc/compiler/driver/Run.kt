package com.hsc.compiler.driver

import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.compiler.codegen.ActionTransformer
import com.hsc.compiler.codegen.generateHtsl
import com.hsc.compiler.errors.*
import com.hsc.compiler.ir.ast.Ast
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.lower
import com.hsc.compiler.parse.Lexer
import com.hsc.compiler.parse.Parser
import com.hsc.compiler.parse.SourceProvider
import com.hsc.compiler.parse.TokenStream
import com.hsc.compiler.span.SourceMap
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true }

fun runCompiler(opts: CompileOptions, files: List<Path>) = runBlocking {
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
        opts.output?.let {
            when (opts.target) {
                Target.Json -> {
                    if (SystemFileSystem.exists(Path("$it/json"))) {
                        SystemFileSystem.list(Path("$it/json")).forEach { path ->
                            SystemFileSystem.delete(path)
                        }
                    }
                    SystemFileSystem.createDirectories(Path("$it/json"))
                    functions.map { fn -> Pair(json.encodeToString(fn.actions), fn.name) }.forEach { (text, name) ->
                        val path = Path("$it/json/$name.json")
                        val buffer = Buffer()
                        buffer.write(text.encodeToByteArray())
                        SystemFileSystem.sink(path).write(buffer, buffer.size)
                    }
                }
                Target.Htsl -> {
                    if (SystemFileSystem.exists(Path("$it/htsl"))) {
                        SystemFileSystem.list(Path("$it/htsl")).forEach { path ->
                            SystemFileSystem.delete(path)
                        }
                    }
                    SystemFileSystem.createDirectories(Path("$it/htsl"))
                    functions.map { fn -> Pair(generateHtsl(sess, fn), fn.name) }.forEach { (text, name) ->
                        val path = Path("$it/htsl/$name.htsl")
                        val buffer = Buffer()
                        buffer.write(text.encodeToByteArray())
                        SystemFileSystem.sink(path).write(buffer, buffer.size)
                    }
                }
            }
        }
    }

    compiler.enter {
        val elapsed = Clock.System.now() - startTime
        emitter.complete(opts.houseName, elapsed)
    }

    compiler.emitter.close()
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
                    dcx.err("stack overflow during macro expansion", dcx.srcp.virtualSpan).emit()
                } else {
                    dcx.err("stack overflow during compilation").emit()
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