package com.hsc.mason.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.hsc.mason.*

class RunCommand : CliktCommand() {

    private val path: String? by argument().optional()

    override fun run() {
        BuildSession(path, true).run()
    }
}