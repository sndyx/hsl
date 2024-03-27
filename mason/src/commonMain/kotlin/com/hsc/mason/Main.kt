package com.hsc.mason

import com.github.ajalt.clikt.core.subcommands
import com.hsc.mason.commands.BuildCommand
import com.hsc.mason.commands.CleanCommand
import com.hsc.mason.commands.MasonCommand

fun main(args: Array<String>) = MasonCommand()
    .subcommands(BuildCommand(), CleanCommand()).main(args)