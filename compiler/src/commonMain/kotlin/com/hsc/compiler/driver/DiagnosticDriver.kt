package com.hsc.compiler.driver

import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.hsc.compiler.codegen.ActionTransformer
import com.hsc.compiler.codegen.generateHtsl
import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.Diagnostic
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.time.Duration
import kotlin.time.measureTimedValue

class DiagnosticDriver(opts: CompileOptions) : Driver(opts) {

    private val json = Json { prettyPrint = true }
    private val snbtp = StringifiedNbt { prettyPrint = true }
    private val snbt = StringifiedNbt { }

    val sm = SourceMap()

    override fun runCompiler(fp: FileProvider) = measureTimedValue {
        runBlocking {
            start()

            val ast = enter { runBlocking {
                Ast().apply {
                    fp.getFiles().map { sf ->
                        launch(scope) {
                            val srcp = sm.loadFile(sf)
                            dcx = DiagCtx(this@DiagnosticDriver, srcp)

                            val sess = CompileSess(dcx, opts, sm)

                            val lexer = Lexer(sess, srcp)

                            val tokenStream = TokenStream(lexer.iterator())
                            val parser = Parser(tokenStream, sess)

                            parser.parseCompletely(this@apply)
                        }
                    }
                }
            }}

            dcx = DiagCtx(this@DiagnosticDriver)
            val sess = CompileSess(dcx, opts, sm)
            enter {
                val lcx = LoweringCtx(ast, sess)
                lower(lcx)
            }

            val functions = enter {
                val transformer = ActionTransformer(sess)
                transformer.transform(ast)
            }

            enter<Unit> {
                opts.output?.let { outDir ->
                    when (opts.target) {
                        Target.Json -> {
                            if (FileSystem.SYSTEM.exists("$outDir/json".toPath())) {
                                FileSystem.SYSTEM.list("$outDir/json".toPath()).forEach { path ->
                                    FileSystem.SYSTEM.delete(path)
                                }
                            }
                            FileSystem.SYSTEM.createDirectories("$outDir/json".toPath())
                            functions.map { fn ->
                                Pair(
                                    json.encodeToString(fn.actions),
                                    fn.name
                                )
                            }.forEach { (text, name) ->
                                val path = "$outDir/json/$name.json".toPath()
                                FileSystem.SYSTEM.write(path) {
                                    writeUtf8(text)
                                }
                            }
                            ast.items.filter { it.kind is ItemKind.Const }.forEach {
                                val path = "$outDir/json/${it.ident.name}.snbt".toPath()
                                val item =
                                    (((it.kind as ItemKind.Const).value.kind as ExprKind.Lit).lit as Lit.Item).value
                                FileSystem.SYSTEM.write(path) {
                                    writeUtf8(
                                        snbtp.encodeToString(
                                            NbtCompound.serializer(),
                                            item.nbt
                                        )
                                    )
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
                                    val str = snbt.encodeToString(
                                        NbtCompound.serializer(),
                                        lit.value.nbt
                                    )
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

        }
    }.let { time ->
        complete(time.duration)
    }

    override fun handleDiagnostic(diag: Diagnostic) {
        t.printDiagnostic(diag, sm)
    }

    private fun start() {
        t.println("${(green + bold)("compiling:")} ${bold(opts.houseName)}")
    }

    private fun complete(duration: Duration) {
        if (buildFailed) {
            t.println("${(red + bold)("failed:")} ${bold("could not compile ${opts.houseName}")}")
        } else {
            t.print("${(green + bold)("complete:")} ")
            t.println("${bold(opts.houseName)} successfully compiled in ${italic("${duration.inWholeMilliseconds}ms")}")
        }
    }

}