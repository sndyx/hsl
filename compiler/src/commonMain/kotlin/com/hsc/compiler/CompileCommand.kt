package com.hsc.compiler

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.hsc.compiler.driver.*
import com.hsc.compiler.driver.Target
import okio.Path.Companion.toPath
import kotlin.collections.map

class CompileCommand : com.github.ajalt.clikt.core.CliktCommand() {

    private val files: List<String>? by argument().multiple()

    private val virtualFiles: String? by option("--virtual")
        .help("Virtual files")

    private val output: String? by option("--output", "-o")

    // Matters if output exists OR emitter is output
    private val target: Target by option("--target", "-t")
        .enum<Target> { it.label }
        .default(Target.Json)
        .help("Set housing output target")

    private val driver: DriverMode by option("--driver", "-d")
        .enum<DriverMode> { it.label }
        .default(DriverMode.Diagnostics)
        .help("Set output driver mode")

    private val color: Color by option("--color", "-c")
        .enum<Color> { it.label }
        .default(Color.Auto)
        .help("set output color")

    // Emitter Diagnostics
    private val houseName: String by option("--house-name")
        .default("Project")

    // Emitter Interpreter
    private val instant: Boolean by option("--instant", "-i").flag()
        .help("Skip interpreter pauses (does not change behavior)")

    // LANGUAGE RULES:
    private val mode: Mode by option("--mode", "-m")
        .enum<Mode> { it.label }
        .default(Mode.Normal)
        .help("Set compiler ruleset")

    private val slashIdents: Boolean by option("--slash-idents").flag()

    private val tempPrefix: String by option("--temp-prefix")
        .default("_")
        .help("Set temp variable prefix")

    // Show version
    private val version: Boolean by option("--version", "-v").flag()
        .help("Show version")

    override fun run() {
        if (version || ((files == null || files?.isEmpty() == true) && virtualFiles == null)) {
            println("hsc v$VERSION")
            return
        }

        val fp: FileProvider =
            if (files?.isNotEmpty() == true) SystemFileProvider(files!!.map { it.toPath() })
            else VirtualFileProvider(virtualFiles!!)

        val opts = CompileOptions(houseName, target, mode, driver, output, color, slashIdents, tempPrefix, instant)

        runCompiler(opts, fp)
    }

}