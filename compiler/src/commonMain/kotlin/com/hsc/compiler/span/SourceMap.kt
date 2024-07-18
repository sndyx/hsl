package com.hsc.compiler.span

import com.hsc.compiler.parse.SourceProvider

data class SourceMap(
    val files: MutableMap<Int, SourceFile> = mutableMapOf()
) {

    fun loadFile(file: SourceFile): SourceProvider {
        files[file.fid] = file
        return SourceProvider(file)
    }

}