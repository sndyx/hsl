import kotlin.test.Test
import kotlin.test.assertEquals

class TestSimple {

    @Test
    fun print() {
        val result = interpret("""
            fn main() {
                message("Hello, world!")
            }
        """.trimIndent())
        assertEquals("Hello, world!", result, "Wrong result")
    }

    @Test
    fun if_statement() {
        val result = interpret("""
            fn main() {
                x = 5
                if (x == 5) {
                    message("IF 1")
                } else {
                    message("ELSE 1")
                }
                
                if (x == 4) {
                    message("IF 2")
                } else {
                    message("ELSE 2")
                }
            }
        """.trimIndent())
        assertEquals("IF 1\nELSE 2", result, "Wrong result")
    }

    @Test
    fun block_inlining() {
        val result = interpret("""
            fn main() {
                x = {
                    message("1")
                    2
                }
                message("%stat.player/x%")
            }
        """.trimIndent())
        assertEquals("1\n2", result, "Wrong result")
    }

    @Test
    fun test_strict() {
        compile("""
            #strict
            fn main() {
                _x = 5
                _y = _x
                _return = _y
            }
        """.trimIndent())
    }

}