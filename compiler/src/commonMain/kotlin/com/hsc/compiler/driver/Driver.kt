package com.hsc.compiler.driver

import com.hsc.compiler.codegen.AstToActionTransformer
import com.hsc.compiler.errors.*
import com.hsc.compiler.ir.ast.AstMap
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.parse.*
import com.hsc.compiler.pretty.prettyPrintActionsWebview
import com.hsc.compiler.pretty.prettyPrintAst
import com.hsc.compiler.span.SourceMap
import kotlinx.datetime.Clock
import kotlinx.io.files.Path
import kotlinx.serialization.json.Json

class Driver(private val opts: CompileOptions) {

    private val sourceMap: SourceMap = SourceMap()

    private val passes = passesForMode(opts.mode)

    private val emitter: Emitter = when(opts.output) {
        Output.Terminal -> HumanEmitter(sourceMap)
        Output.Minecraft -> HumanEmitter(sourceMap)
        Output.Internal -> DiagEmitter(sourceMap)
        Output.Webview -> WebviewEmitter
    }
    private val dcx: DiagCtx = DiagCtx(emitter)

    private val json = Json { prettyPrint = true }

    fun run(files: List<Path>) {
        val startTime = Clock.System.now()
        val projectName = "example 0.0.1"

        emitter.start(projectName)

        var success = false
        try {
            val map = AstMap()
            val sess = CompileSess(dcx, opts, sourceMap, map)

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

                var item = parser.parseItem()
                while (item != null) {
                    // println(pprint(item))
                    item = parser.parseItem()
                }
                // emitter.pass("Parse", System.currentTimeMillis() - ptime)

            }

            passes.forEach {
                if (!emitter.emittedError) {
                    it.run(sess)
                }
            }

            success = !emitter.emittedError

            if (success) {
                val items = map.query<Item>()
                prettyPrintAst(items)

                val pass2 = AstToActionTransformer(sess)
                val functions = pass2.run()

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