package com.hsc.compiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.hsc.compiler.driver.*
import com.hsc.compiler.driver.Target
import kotlinx.io.files.Path

class CompileCommand : CliktCommand() {

    private val paths: List<String> by argument().multiple()

    private val houseName: String by option("--house-name")
        .default("Project")

    private val target: Target by option("--target", "-t")
        .enum<Target> { it.label }
        .default(Target.Json)
        .help("Set housing output target")

    private val mode: Mode by option("--mode", "-m")
        .enum<Mode> { it.label }
        .default(Mode.Normal)
        .help("Set compiler ruleset")

    private val emitter: EmitterType by option("--emitter", "-e")
        .enum<EmitterType> { it.label }
        .default(EmitterType.Terminal)
        .help("Set output emitter mode")

    private val output: String? by option("--output", "-o")

    private val color: Color by option("--color", "-c")
        .enum<Color> { it.label }
        .default(Color.Auto)
        .help("set output color")

    private val version: Boolean by option("--version", "-v").flag()
        .help("Show version")

    private val slashIdents: Boolean by option("--slash-idents").flag()

    override fun run() {
        if (version || paths.isEmpty()) {
            println("hsc v1.2.0 beta")
            return
        }
        val opts = CompileOptions(houseName, target, mode, emitter, output, color, slashIdents)
        runCompiler(opts, paths.map { Path(it) })
    }

}