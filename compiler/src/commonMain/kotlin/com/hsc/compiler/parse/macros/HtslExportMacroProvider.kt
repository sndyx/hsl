package com.hsc.compiler.parse.macros

import com.hsc.compiler.parse.*
import com.hsc.compiler.ir.ast.Lit
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import net.benwoodworth.knbt.NbtCompound
import net.benwoodworth.knbt.StringifiedNbt

object HtslExportMacroProvider : MacroProvider {

    private val snbt by lazy { StringifiedNbt { } }

    override val name: String = "#htsl_export"

    override fun invoke(lexer: Lexer): CharProvider = with(lexer) {
        val stream = TokenStream(lexer.iterator())
        val parser = Parser(stream, lexer.sess)

        eatSpaces()
        val ident = parser.parseRawIdent()
        eatSpaces()
        val item = parser.parseLiteral()

        if (item !is Lit.Item) {
            throw sess.dcx().err("expected item, found literal")
        }

        val itemPath = Path(sess.opts.output!! + "/htsl/$ident.json")

        val nbt = snbt.encodeToString(NbtCompound.serializer(), item.value.nbt)

        val buffer = Buffer()
        val jsonObject = buildJsonObject {
            put("item", nbt)
        }
        buffer.write(Json.encodeToString(JsonObject.serializer(), jsonObject).encodeToByteArray())
        SystemFileSystem.sink(itemPath).write(buffer, buffer.size)

        EmptyCharProvider
    }

    private fun Lexer.eatSpaces(): Unit = eatWhile { it.isWhitespace() && it != '\n' }

}