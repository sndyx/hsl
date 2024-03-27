package com.hsc.compiler.parse

data class MacroProvider(
    val name: String,
    val source: String,
    val args: List<String>
) {

    fun invoke(values: List<String>): String =
        args.zip(values).fold(source) { src, (arg, value) ->
            src.replace("\${$arg}", value).replace(Regex("\\\$$arg\\b"), value)
        }

}