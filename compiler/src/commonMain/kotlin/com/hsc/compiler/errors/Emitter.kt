package com.hsc.compiler.errors

import kotlin.time.Duration

abstract class Emitter {

    var pass: String = "none"
    var emittedError = false
    var emittedBug = false

    fun emit(diagnostic: Diagnostic) {
        if (diagnostic.level == Level.Error) emittedError = true
        if (diagnostic.level == Level.Bug) emittedBug = true
        print(diagnostic)
    }

    abstract fun print(diagnostic: Diagnostic)

    abstract fun start(name: String)
    abstract fun failed(name: String)
    abstract fun complete(name: String, time: Duration)

    abstract fun pass(name: String, time: Duration)

    abstract fun close()

}