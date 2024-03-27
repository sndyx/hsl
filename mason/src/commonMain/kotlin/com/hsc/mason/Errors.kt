package com.hsc.mason

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import com.hsc.mason.proc.exitProcess
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

private val t = Terminal()

@OptIn(ExperimentalSerializationApi::class)
fun errorInvalidManifest(err: Throwable): Nothing {
    // Why is UnknownKeyException private?
    if (err::class.simpleName == "UnknownKeyException" || err is IllegalStateException) {
        // err.message is always key name
        t.println(TextStyles.bold("${TextColors.red("failed")}: unknown manifest property `${err.message}`"))
    } else if (err is MissingFieldException) {
        if (err.missingFields.size > 1) {
            val fields = err.missingFields.joinToString { "`${it}`" }
            t.println(TextStyles.bold("${TextColors.red("failed")}: missing required manifest properties $fields"))
        } else {
            t.println(TextStyles.bold("${TextColors.red("failed")}: missing required manifest property `${err.missingFields.single()}`"))
        }
    } else {
        err.printStackTrace()
        t.println(TextStyles.bold("${TextColors.red("failed")}: failed parsing manifest"))
    }
    exitProcess(0)
}

fun errorInvalidLockfile(): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: invalid lockfile"))
    exitProcess(0)
}

fun errorWrongName(name: String, remote: String): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: package `$name` does not match remote name `$remote`"))
    exitProcess(0)
}

fun errorMalformedURL(url: String): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: malformed URL `$url`"))
    exitProcess(0)
}

fun errorProtocolURL(url: String): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: URL contains protocol `$url`"))
    exitProcess(0)
}

fun errorMissingGit(): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: git installation not found."))
    exitProcess(0)
}

fun errorMissingHsc(): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: hsc installation not found."))
    exitProcess(0)
}

fun errorNotAPackage(name: String): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: not a package `$name`"))
    exitProcess(0)
}

fun errorDependencyConflict(name: String): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: conflicting dependencies for `$name`"))
    exitProcess(0)
}

fun errorCannotDelete(): Nothing {
    t.println(TextStyles.bold("${TextColors.red("failed")}: cannot delete directory"))
    exitProcess(0)
}
