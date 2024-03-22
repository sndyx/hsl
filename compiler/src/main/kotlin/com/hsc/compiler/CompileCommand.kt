package com.hsc.compiler

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.hsc.compiler.driver.*
import com.hsc.compiler.driver.Target
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.toPath

class CompileCommand : CliktCommand() {

    private val path: String by argument()

    private val target: Target by option("--target", "-t")
        .enum<Target> { it.label }
        .default(Target.Default)
        .help("Set housing destination target")

    private val mode: Mode by option("--mode", "-m")
        .enum<Mode> { it.label }
        .default(Mode.Normal)
        .help("Set compiler ruleset")

    private val output: Output by option("--output", "-o")
        .enum<Output> { it.label }
        .default(Output.Terminal)
        .help("Set output emitter mode")

    private val version: Boolean by option("--version", "-v").flag()
        .help("Show version")


    private val trace: Boolean by option("--trace").flag()
        .help("Enable stack trace in errors")

    private val verbose: Boolean by option("--verbose").flag()
        .help("Enable extra pass messages")

    override fun run() {
        if (version) {
            println("hslc v1.0.0")
            return
        }
        val opts = CompileOptions(target, mode, output, trace, verbose)
        Driver(opts).run(listOf(URI(path).toPath()))
    }

}