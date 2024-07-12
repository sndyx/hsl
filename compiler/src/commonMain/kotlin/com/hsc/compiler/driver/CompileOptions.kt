package com.hsc.compiler.driver

data class CompileOptions(
    val houseName: String,
    val target: Target,
    val mode: Mode,
    val emitter: EmitterType,
    val output: String?,
    val color: Color,
    val stupidDumbIdiotMode: Boolean,
    val tempPrefix: String,
)

enum class Color(val label: String) {
    Auto("auto"),
    Always("always"),
    Never("never");
}

enum class Target(val label: String) {
    Json("json"),
    Htsl("htsl");
}

enum class Mode(val label: String) {
    Strict("strict"),
    Normal("normal"),
    Optimize("optimize");
}

enum class EmitterType(val label: String) {
    Terminal("terminal"),
    Minecraft("minecraft"),
    Internal("internal"),
    Webview("webview"),
}