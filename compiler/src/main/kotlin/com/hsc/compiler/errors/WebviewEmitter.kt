package com.hsc.compiler.errors

object WebviewEmitter : Emitter() {

    override fun print(diagnostic: Diagnostic) {}
    override fun start(name: String, location: String) {}
    override fun pass(name: String, time: Long) {}
    override fun close() {}

    override fun complete(name: String, time: Long) {
        println("<div class=\"complete\"><p><i><span class=\"dim\">Compiled in ${time}ms</span></i></p></div>")
    }

    override fun failed(name: String) {
        println("<div class=\"error\"><p>There was an error during compilation</p></div>")
    }

}