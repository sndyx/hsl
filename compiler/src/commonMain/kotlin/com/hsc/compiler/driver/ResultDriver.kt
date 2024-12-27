package com.hsc.compiler.driver

import com.hsc.compiler.codegen.ActionTransformer
import com.hsc.compiler.codegen.generateHtsl
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import com.hsc.compiler.errors.printDiagnostic
import com.hsc.compiler.ir.ast.Ast
import com.hsc.compiler.ir.ast.AstVisitor
import com.hsc.compiler.ir.ast.ExprKind
import com.hsc.compiler.ir.ast.ItemKind
import com.hsc.compiler.ir.ast.Lit
import com.hsc.compiler.lowering.LoweringCtx
import com.hsc.compiler.lowering.lower
import com.hsc.compiler.parse.Lexer
import com.hsc.compiler.parse.Parser
import com.hsc.compiler.parse.TokenStream
import com.hsc.compiler.span.SourceMap
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement

class ResultDriver(opts: CompileOptions) : Driver(opts) {

    private val json = Json { prettyPrint = true }

    val sm = SourceMap()

    override fun runCompiler(fp: FileProvider) = runBlocking {
        val ast = enter { runBlocking {
            Ast().apply {
                fp.getFiles().map { sf ->
                    launch(scope) {
                        val srcp = sm.loadFile(sf)
                        dcx = DiagCtx(this@ResultDriver, srcp)

                        val sess = CompileSess(dcx, opts, sm)

                        val lexer = Lexer(sess, srcp)

                        val tokenStream = TokenStream(lexer.iterator())
                        val parser = Parser(tokenStream, sess)

                        parser.parseCompletely(this@apply)
                    }
                }
            }
        }}

        dcx = DiagCtx(this@ResultDriver)
        val sess = CompileSess(dcx, opts, sm)
        enter {
            val lcx = LoweringCtx(ast, sess)
            lower(lcx)
        }

        val functions = enter {
            val transformer = ActionTransformer(sess)
            transformer.transform(ast)
        }

        if (buildFailed) return@runBlocking

        enter {
            val arr = buildJsonArray {
                when (opts.target) {
                    Target.Json -> {
                        functions.forEach { fn ->
                            add(json.encodeToJsonElement(fn))
                        }
                    }
                    Target.Htsl -> {
                        error("unsupported")
                    }
                }
            }
            t.println(Json.encodeToString(arr))
        }
    }

    override fun handleDiagnostic(diag: Diagnostic) {
        if (diag.level == Level.Error || diag.level == Level.Bug) t.printDiagnostic(diag, sm)
    }

}