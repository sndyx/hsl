package com.hsc.compiler.errors

import com.hsc.compiler.span.SourceMap
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlin.time.Duration

class DiagEmitter(private val sourceMap: SourceMap) : Emitter() {

    val list: MutableList<Diagnostic> = mutableListOf()

    override fun print(diagnostic: Diagnostic) {
        list.add(diagnostic)
    }

    override fun start(name: String) {
        // Ignore
    }

    override fun failed(name: String) {
        // Ignore
    }

    override fun complete(name: String, time: Duration) {
        // Ignore
    }

    override fun pass(name: String, time: Duration) {
        // Ignore
    }

    override fun close() {
        val arr = buildJsonArray {
            list.forEach { diagnostic ->
                val severity = when (diagnostic.level) {
                    Level.Error -> "error"
                    Level.Warning -> "warning"
                    Level.Note -> "note"
                    Level.Hint -> "hint"
                    else -> null
                }

                if (severity != null) {
                    val obj = buildJsonObject {
                        val (span, _, label) = diagnostic.spans.first()

                        val file = sourceMap.files[span.fid]!!
                        val loPos = file.lookupPos(span.lo)
                        val hiPos = file.lookupPos(span.hi)

                        put("level", severity)
                        put("message", diagnostic.message)
                        label?.let {
                            put("label", it)
                        }
                        put("lo", buildJsonObject {
                            put("line", loPos.first)
                            put("col", loPos.second)
                        })
                        put("hi", buildJsonObject {
                            put("line", hiPos.first)
                            put("col", hiPos.second)
                        })
                    }
                    add(obj)
                }
            }
        }
        println(Json.encodeToString(arr))
    }

}