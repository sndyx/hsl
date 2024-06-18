package com.hsc.mason.proc

import kotlinx.coroutines.coroutineScope
import okio.Path

object Git {

    suspend fun isInstalled(): Boolean = coroutineScope {
        runCatching {
            printProcess("git --version")
        }.isSuccess
    }

    fun clone(dir: Path, repo: String, tag: String? = null) {
        val branch = if (tag != null) "--branch ${sanitizeStrict(tag)}" else ""
        printProcess("git clone --quiet --depth 1 $branch ${sanitizeStrict(repo)} $dir")
    }

    fun latest(repo: String, tag: String? = null): String {
        val proc = printProcess("git ls-remote $repo ${tag ?: "HEAD"}")
        return ""
        // return proc.substring(0, 40)
    }

}