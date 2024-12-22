import kotlin.test.Test
import kotlin.test.assertEquals

class TestInlineFunctions {

    @Test
    fun simple() {
        val result = interpret("""
            fn main() {
                other()
            }
            
            #inline
            fn other() {
                message("Hello")
            }
        """.trimIndent())
        assertEquals("Hello", result, "Wrong result")
    }

    @Test
    fun param() {
        val result = interpret("""
            fn main() {
                x = 5
                y = other(x)
                message("%stat.player/y%")
            }
            
            #inline
            fn other(_param) {
                return _param * 5
            }
        """.trimIndent())
        assertEquals("25", result, "Wrong result")
    }

    @Test
    fun param_modified() {
        val result = interpret("""
            fn main() {
                x = 5
                y = other(x)
                message("%stat.player/y%")
            }
            
            #inline
            fn other(_param) {
                _param += 5
                return _param * 5
            }
        """.trimIndent())
        assertEquals("50", result, "Wrong result")
    }

}