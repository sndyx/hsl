package com.hsc.compiler.driver

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.hsc.compiler.codegen.ActionTransformer
import com.hsc.compiler.errors.printDiagnostic
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.interpreter.InterpreterSession
import com.hsc.compiler.ir.ast.*
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.lower
import com.hsc.compiler.parse.Lexer
import com.hsc.compiler.parse.Parser
import com.hsc.compiler.parse.TokenStream
import com.hsc.compiler.span.SourceMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class InterpreterDriver(opts: CompileOptions) : Driver(opts) {

    private val sm = SourceMap()

    override fun runCompiler(fp: FileProvider) = measureTimedValue { runBlocking {

        val ast = enter {
            val ast = Ast()
            runBlocking {
                fp.getFiles().map { sf ->
                    launch(scope) {
                        val srcp = sm.loadFile(sf)
                        dcx = DiagCtx(this@InterpreterDriver, srcp)

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

        dcx = DiagCtx(this@InterpreterDriver)
        val sess = CompileSess(dcx, opts, sm)
        enter {
            val lcx = LoweringCtx(ast, sess)
            lower(lcx)
        }

        enter {
            val transformer = ActionTransformer(sess)
            transformer.transform(ast)
        }

        if (buildFailed) return@runBlocking
        enter {
            val interpreter = InterpreterSession(sess, ast, t)
            interpreter.run()
        }

    } }.let { time ->
        complete(time.duration)
    }

    override fun handleDiagnostic(diag: Diagnostic) {
        t.printDiagnostic(diag, sm)
    }

    private fun complete(duration: Duration) {
        if (buildFailed) {
            t.println("${(red + bold)("failed:")} ${bold("could not compile ${opts.houseName}")}")
        } else {
            t.print("${(gray + bold)("complete:")} ")
            t.println(gray("${opts.houseName} successfully ran in ${italic("${duration.inWholeMilliseconds}ms")}"))
        }
    }

}