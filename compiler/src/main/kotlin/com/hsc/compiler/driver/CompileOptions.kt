package com.hsc.compiler.driver

data class CompileOptions(
    val target: Target,
    val mode: Mode,
    val output: Output,
    val trace: Boolean,
    val verbose: Boolean,
)

enum class Target(val label: String) {
    Default("default"),
    HousingPlus("housing+");
}

enum class Mode(val label: String) {
    Strict("strict"),
    Normal("normal"),
    Optimized("optimized");
}

enum class Output(val label: String) {
    Terminal("terminal"),
    Minecraft("minecraft"),
    Internal("internal"),
    Webview("webview"),
}