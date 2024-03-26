package com.hsc.mason

import kotlinx.coroutines.coroutineScope
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object Git {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            process("git --version").await()
        }.isSuccess
    }

    suspend fun clone(dir: Path, repo: URL, tag: String? = null) {
        val branch = if (tag != null) "--branch $tag" else ""
        process("git clone --progress --verbose --depth 1 $branch $repo ${dir.absolutePathString()}").await()
    }

    suspend fun latest(repo: URL, tag: String? = null): String {
        val proc = process("git ls-remote $repo ${tag ?: "HEAD"}").await()
        return proc.result.substring(0, 40)
    }

}