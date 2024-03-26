package com.hsc.mason

import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = MasonCommand().subcommands(BuildCommand()).main(args)