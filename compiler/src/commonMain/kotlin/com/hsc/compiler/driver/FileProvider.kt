package com.hsc.compiler.driver

import com.hsc.compiler.span.SourceFile
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

interface FileProvider {

    fun getFiles(): List<SourceFile>

}

class SystemFileProvider(private val paths: List<Path>) : FileProvider {

    override fun getFiles(): List<SourceFile> {
        return paths.map {
            val src = FileSystem.SYSTEM.read(it) {
                readUtf8()
            }
            SourceFile(it, src)
        }
    }

}

class VirtualFileProvider(private val base64: String) : FileProvider {

    @OptIn(ExperimentalEncodingApi::class)
    override fun getFiles(): List<SourceFile> {
        return Json.decodeFromString<List<VirtualFile>>(
            Base64.decode(base64).decodeToString()
        ).map {
            SourceFile(it.path.toPath(), it.src)
        }
    }

}

class VirtualFileProviderFlat(val content: String) : FileProvider {
    override fun getFiles(): List<SourceFile> {
        return listOf(SourceFile("test.hsl".toPath(), content))
    }
}

@Serializable
class VirtualFile(val path: String, val src: String)