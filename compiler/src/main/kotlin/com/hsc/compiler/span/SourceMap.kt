package com.hsc.compiler.span

import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.random.Random

data class SourceMap(
    val files: MutableMap<Int, SourceFile> = mutableMapOf()
) {

    fun loadFile(path: Path): SourceFile {
        val src = path.readText()
        val file = SourceFile(path, src)
        files[file.fid] = file
        return file
    }

}

data class SourceFile(
    val path: Path,
    val src: String,
    val lines: MutableList<Int> = mutableListOf()
) {

    val fid: Int = Random.nextInt()

    fun lookupLine(pos: Int): Int {
        val index = lines.indexOfFirst { it > pos }
        return if (index > -1) index else lines.size
    }

    fun lookupPos(pos: Int): Pair<Int, Int> =
        lookupLine(pos).let { line ->
            val linePos = lines.getOrNull(line - 1) ?: 0
            val col = pos - linePos
            return Pair(line + 1, col) // line is 0-indexed
        }

    fun getLine(line: Int): String {
        val begin = lines.getOrNull(line - 2) ?: 0
        val slice = src.substring(begin)
        // Avoid lookup of `lines[line + 1]` as it might not be lexed yet
        return slice.substringBefore('\n')
    }

    fun addLine(pos: Int) {
        lines.add(pos)
    }

}
