package com.hsc.compiler.errors

import kotlin.time.Duration

object WebviewEmitter : Emitter() {

    override fun print(diagnostic: Diagnostic) {}
    override fun start(name: String) {}
    override fun pass(name: String, time: Duration) {}
    override fun close() {}

    override fun complete(name: String, time: Duration) {
        println("<div class=\"complete\"><p><i><span class=\"dim\">Compiled in ${time.inWholeMilliseconds}ms</span></i></p></div>")
    }

    override fun failed(name: String) {
        println("<div class=\"error\"><p>There was an error during compilation</p></div>")
    }

}