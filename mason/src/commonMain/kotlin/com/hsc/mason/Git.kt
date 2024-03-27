package com.hsc.mason

import kotlinx.coroutines.coroutineScope
import okio.Path

object Git {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            process("git --version")
        }.isSuccess
    }

    fun clone(dir: Path, repo: String, tag: String? = null) {
        val branch = if (tag != null) "--branch $tag" else ""
        process("git clone --quiet --depth 1 $branch $repo $dir")
    }

    fun latest(repo: String, tag: String? = null): String {
        val proc = process("git ls-remote $repo ${tag ?: "HEAD"}")
        return proc.substring(0, 40)
    }

}