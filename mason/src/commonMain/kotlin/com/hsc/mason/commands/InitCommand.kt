package com.hsc.mason.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM

class InitCommand : CliktCommand() {

    private val _path: String? by argument().optional()
    private lateinit var path: Path

    private val fs = FileSystem.SYSTEM

    override fun run() {
        path = _path?.toPath() ?: "".toPath()

        if (!fs.exists(path.resolve("House.toml"))) {

            fs.write(path.resolve("House.toml")) {
                writeUtf8("""
                # see https://github.com/sndyx/hsl/blob/master/docs/reference/manifest.md for reference
                [package]
                name = "example_house"
                version = "1.0.0"
                """.trimIndent())
            }

            fs.createDirectory(path.resolve("src"))
            fs.write(path.resolve("src/example.hsl")) {
                writeUtf8("""
                fn example_function() {
                    message("Hello from HSL!")
                }
                """.trimIndent())
            }

        }
    }

}