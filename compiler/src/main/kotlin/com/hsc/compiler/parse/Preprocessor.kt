package com.hsc.compiler.parse

import com.hsc.compiler.errors.DiagCtx
import com.hsc.compiler.errors.CompileException
import com.hsc.compiler.span.Span

// This is terrible.
// This is miserable.
// Nobody should ever write code like this.
// This will surely be replaced
// Here are 100 reasons why this code is awful: (nvm chatjibbity only gave me like 10)
//
// Reusability and Readability:
//
// Limited functionality: This code only handles basic macro definitions with string substitution.
// Lack of Error Handling: While there's basic error handling for identifier detection, more robust checks for syntax errors and unexpected characters are missing.
// Magic Numbers: Using a constant value like 7 for checking "#define" is not ideal.
// Unnecessary String Builders: Consider using string concatenation directly instead of multiple StringBuilder instances.
// Variable Scope: The found map could be declared outside the run function for better access and modification.
// Maintainability and Performance:
//
// Primitive Data Structures: Using a basic HashMap might not be the most efficient choice for larger preprocessor needs. Consider libraries offering optimized data structures.
// Redundant Code: The logic for handling whitespace before and after content can be reused.
// isWhitespace check: Leverage built-in character classification functions provided by the language/library.
// Stream Processing: Explore the possibility of using language features or libraries for stream-based processing of the source code, potentially improving performance for larger files.
// Security Considerations:
//
// Potential for Code Injection: Without proper validation and sanitization of macro content, the code could be vulnerable to code injection attacks if the source is not trusted.
// Additional Points:
//
// Limited Testing: The code seems to lack comprehensive testing, making it difficult to ensure its correctness and behavior under various input scenarios.
// Documentation: Comments explaining the purpose of the code, functions, and variables would significantly improve readability and maintainability.
class Preprocessor(private val dcx: DiagCtx, private val srcp: SourceProvider) {

    private val src = srcp.src
    private var pos = 0

    fun run() {
        try {
            val found = mutableMapOf<String, String>()

            eatWhile {
                if (it == '\n') srcp.addLine(pos - 1)
                it.isWhitespace()
            }

            while (src.substring(pos, pos + 7) == "#define") {
                pos += 8
                eatWhile { it == ' ' }
                val name = StringBuilder()
                if (src[pos].isLetter() || src[pos] == '_') {
                    name.append(src[pos++])
                } else {
                    val err = dcx.err("expected preprocessor ident")
                    err.span(Span(pos, pos, srcp.fid))
                    throw CompileException(err)
                }
                while (src[pos].isLetterOrDigit() || src[pos] == '_') {
                    name.append(src[pos++])
                }

                eatWhile { it == ' ' }

                val content = StringBuilder()
                if (src[pos] == '`') {
                    pos++
                    eatWhile {
                        if (it != '`') content.append(it)
                        if (it == '\n') srcp.addLine(pos - 1)
                        it != '`'
                    }
                    pos++
                } else {
                    eatWhile {
                        if (it != '\n' && it != '\r') content.append(it)
                        it != '\n'
                    }
                }

                found[name.toString()] = content.toString()

                eatWhile {
                    if (it == '\n') srcp.addLine(pos + 1)
                    it.isWhitespace()
                }
            }

            srcp.pos = pos

            found.forEach {
                srcp.addSource(it.key, it.value)
            }
        } catch (e: Exception) {
            // FUCKL YOU! :LOL
        }
    }

    private fun bump(): Char? {
        return src.getOrNull(pos++)
    }

    private fun eatWhile(predicate: (Char) -> Boolean) {
        try {
            while (predicate(src[pos]) && !isEof()) {
                bump()
            }
        } catch (e: StringIndexOutOfBoundsException) {
            // you're welcome for the band-aid :-)
        }
    }

    private fun isEof(): Boolean = src.length == pos

}