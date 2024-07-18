package com.hsc.compiler.driver

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.hsc.compiler.codegen.ActionTransformer
import com.hsc.compiler.codegen.generateHtsl
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Diagnostic
import com.hsc.compiler.errors.Level
import com.hsc.compiler.errors.printDiagnostic
import com.hsc.compiler.ir.ast.*
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt

class OutputDriver(opts: CompileOptions) : Driver(opts) {

    private val json = Json { prettyPrint = true }
    private val snbtp = StringifiedNbt { prettyPrint = true }
    private val snbt = StringifiedNbt { }

    val sm = SourceMap()

    override fun runCompiler(fp: FileProvider) = runBlocking {
        val ast = enter { runBlocking {
            Ast().apply {
                fp.getFiles().map { sf ->
                    launch(scope) {
                        val srcp = sm.loadFile(sf)
                        dcx = DiagCtx(this@OutputDriver, srcp)

                        val sess = CompileSess(dcx, opts, sm)

                        val lexer = Lexer(sess, srcp)

                        val tokenStream = TokenStream(lexer.iterator())
                        val parser = Parser(tokenStream, sess)

                        parser.parseCompletely(this@apply)
                    }
                }
            }
        }}

        dcx = DiagCtx(this@OutputDriver)
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
                        functions.map { fn ->
                            Pair(
                                json.encodeToString(fn.actions),
                                fn.name
                            )
                        }.forEach { (text, name) ->
                            add(
                                buildJsonObject {
                                    put("name", "$name.json")
                                    put("content", text)
                                }
                            )
                        }
                        ast.items.filter { it.kind is ItemKind.Const }.forEach {
                            val item = (((it.kind as ItemKind.Const).value.kind as ExprKind.Lit).lit as Lit.Item).value

                            add(
                                buildJsonObject {
                                    put("name", "${it.ident.name}.snbt")
                                    put("content", snbtp.encodeToString(NbtCompound.serializer(), item.nbt))
                                }
                            )
                        }
                    }

                    Target.Htsl -> {
                        functions.map { fn -> Pair(generateHtsl(sess, fn), fn.name) }.forEach { (text, name) ->
                            add(
                                buildJsonObject {
                                    put("name", "$name.htsl")
                                    put("content", text)
                                }
                            )
                        }
                        object : AstVisitor { // serialize & write all items
                            override fun visitLit(lit: Lit) {
                                if (lit !is Lit.Item) return
                                val str = snbt.encodeToString(
                                    NbtCompound.serializer(),
                                    lit.value.nbt
                                )
                                val jsonObject = buildJsonObject {
                                    put("item", str)
                                }

                                add(
                                    buildJsonObject {
                                        put("name", "${lit.value.name}.json")
                                        put("content", Json.encodeToString(JsonObject.serializer(), jsonObject))
                                    }
                                )
                            }
                        }.visitAst(ast)
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