package com.hsc.compiler.driver

import com.hsc.compiler.codegen.passes.AstToActionPass
import com.hsc.compiler.errors.*
import com.hsc.compiler.ir.ast.AstMap
import com.hsc.compiler.pretty.prettyPrintActions
import com.hsc.compiler.ir.ast.Item
import com.hsc.compiler.parse.*
import com.hsc.compiler.pretty.prettyPrintActionsWebview
import com.hsc.compiler.pretty.prettyPrintAst
import com.hsc.compiler.span.SourceMap
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class Driver(val opt: CompileOptions) {

    val sourceMap: SourceMap = SourceMap()

    private val passes = passesForMode(opt.mode)

    val emitter: Emitter = when(opt.output) {
        Output.Terminal -> HumanEmitter(sourceMap)
        Output.Minecraft -> HumanEmitter(sourceMap)
        Output.Internal -> DiagEmitter(sourceMap)
        Output.Webview -> WebviewEmitter
    }
    private val dcx: DiagCtx = DiagCtx(emitter)

    private val json = Json { prettyPrint = true }

    fun run(files: List<Path>) {
        val path = files.single()
        val startTime = System.currentTimeMillis()
        val projectName = "Project v1.0"

        emitter.start(projectName, path.absolutePathString())

        val file = sourceMap.loadFile(path)
        val provider = SourceProvider(file)

        Preprocessor(dcx, provider).run()

        val lexer = Lexer(provider)
        val tokenStream = TokenStream(lexer.iterator())
        val map = AstMap()

        val sess = CompileSess(dcx, sourceMap, map)

        val parser = Parser(tokenStream, sess)

        var success = false
        try {
            var ptime = System.currentTimeMillis()
            var item = parser.parseItem()
            while (item != null) {
                // println(pprint(item))
                item = parser.parseItem()
            }
            // emitter.pass("Parse", System.currentTimeMillis() - ptime)

            ptime = System.currentTimeMillis()
            passes.forEach {
                if (!emitter.emittedError) {
                    it.run(sess)
                    if (!emitter.emittedError) {
                        val elapsed = System.currentTimeMillis() - ptime
                        val name = it::class.simpleName!!.removeSuffix("Pass")
                        if (opt.verbose) emitter.pass(name, elapsed)
                        val items = map.query<Item>()
                        if (opt.verbose) prettyPrintAst(items)
                        ptime = System.currentTimeMillis()
                    }
                }
            }

            success = !emitter.emittedError

            if (success) {
                val items = map.query<Item>()
                //prettyPrintAst(items)

                val pass2 = AstToActionPass(sess)
                val functions = pass2.run()

                if (!emitter.emittedError) {
                    val elapsed = System.currentTimeMillis() - startTime
                    emitter.complete(projectName, elapsed)

                    if (emitter is WebviewEmitter) {
                        prettyPrintActionsWebview(functions)
                    }
                }
            }
        } catch (e: CompileException) {
            e.diag.stackTrace = e.stackTrace.toList()
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
            } else {
            }
            emitter.close()
        }
    }

}