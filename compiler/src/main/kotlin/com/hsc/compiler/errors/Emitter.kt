package com.hsc.compiler.errors

abstract class Emitter {

    var emittedError = false

    fun emit(diagnostic: Diagnostic) {
        if (diagnostic.level == Level.Error) emittedError = true
        print(diagnostic)
    }

    abstract fun print(diagnostic: Diagnostic)

    abstract fun start(name: String, location: String)
    abstract fun failed(name: String)
    abstract fun complete(name: String, time: Long)

    abstract fun pass(name: String, time: Long)

    abstract fun close()

}