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

    @Test
    fun nested() {
        val result = interpret("""
            fn main() {
                hello = -10
                msg = abs(hello)
                message("%stat.player/msg%")
            }

            #inline
            fn sign(_stat) { // for |_stat| < 2^32 or smth
                return ((_stat + 2^32) / 2^32) * 2 - 1  
            }

            #inline
            fn abs(_stat) {
                return _stat * sign(_stat)
            }
        """.trimIndent())
        assertEquals("10", result, "Wrong result")
    }

    @Test
    fun adjacent() {
        val result = interpret("""
            fn main() {
                a = 5
                b = -10
                
                result = test(a) * test(b)
                message("%stat.player/result%")
            }

            #inline
            fn test(_stat) {
                return _stat * -1
            }
        """.trimIndent())
        assertEquals("-50", result, "Wrong result")
    }

}