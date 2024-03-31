package com.hsc.compiler.driver

import com.hsc.compiler.codegen.AstToActionTransformer
import com.hsc.compiler.errors.*
import com.hsc.compiler.ir.ast.Ast
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.lower
import com.hsc.compiler.parse.*
import com.hsc.compiler.pretty.prettyPrintActionsWebview
import com.hsc.compiler.pretty.prettyPrintAst
import com.hsc.compiler.span.SourceMap
import kotlinx.datetime.Clock
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json

class Driver(private val opts: CompileOptions) {

    private val sourceMap: SourceMap = SourceMap()
    private val emitter: Emitter = when(opts.emitter) {
        EmitterType.Terminal -> HumanEmitter(sourceMap)
        EmitterType.Minecraft -> HumanEmitter(sourceMap)
        EmitterType.Internal -> DiagEmitter(sourceMap)
        EmitterType.Webview -> WebviewEmitter
    }
    private val dcx: DiagCtx = DiagCtx(emitter)
    private val json = Json { prettyPrint = true }

    fun run(files: List<Path>) {
        val startTime = Clock.System.now()
        val projectName = "example 0.0.1"

        emitter.start(projectName)

        var success = false
        try {
            val sess = CompileSess(dcx, opts, sourceMap)
            val ast = Ast()

            files.forEach { path ->
                val file = sourceMap.loadFile(path)
                val provider = SourceProvider(file)

                val pre = Preprocessor(sess, provider)
                pre.run()
                // MUST run before the Lexer is even created
                // due to how the Lexer is initialized, this is fine I suppose

                val lexer = Lexer(sess, provider)

                val tokenStream = TokenStream(lexer.iterator())
                val parser = Parser(tokenStream, sess)

                parser.parseCompletely(ast)
                // emitter.pass("Parse", bumpTime())
            }

            val lcx = LoweringCtx(ast, sess)
            lower(lcx)

            success = !emitter.emittedError

            if (success) {
                prettyPrintAst(ast)

                val transformer = AstToActionTransformer(lcx)
                val functions = transformer.run()

                if (!emitter.emittedError) {
                    val elapsed = Clock.System.now() - startTime
                    emitter.complete(projectName, elapsed)

                    if (emitter is WebviewEmitter) {
                        prettyPrintActionsWebview(functions)
                    }
                }
            }
        } catch (e: CompileException) {
            e.diag.emit()
        } catch (e: Exception) {
            val err = dcx.bug(e)
            err.emit()
        } catch (e: Error) {
            val err = dcx.bug(e)
            err.emit()
        } finally {
            if (!success) {
                emitter.failed(projectName)
            }
            emitter.close()
        }
    }

}