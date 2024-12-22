import org.junit.Test
import kotlin.test.assertEquals

class TestComplexExpressions {

    @Test
    fun complex_expression_1() {
        val result = interpret("""
            fn main() {
                a = 2
                b = 8
                
                result = b / a * (a + a)
                message("%stat.player/result%")
            }
        """.trimIndent())
        assertEquals("16", result, "Wrong result")
    }

    @Test
    fun complex_expression_2() {
        val result = interpret("""
            fn main() {
                a = 3
                b = 4
                
                if (a + b == 7) {
                    result = (a + b) * b
                    message("%stat.player/result%")
                }
            }
        """.trimIndent())
        assertEquals("28", result, "Wrong result")
    }

    @Test
    fun complex_expression_3() {
        val result = interpret("""
            fn main() {
                a = 1
                b = 1
                c = 1
                d = 1
                e = 1
                f = 1
                g = 1
                h = 1
                i = 1
                j = 1
                
                result = a + b + c + d + e + f + g + h + i + j
                message("%stat.player/result%")
            }
        """.trimIndent())
        assertEquals("10", result, "Wrong result")
    }

    @Test
    fun complex_expression_4() {
        val result = interpret("""
            fn main() {
                a = 1
                b = 2
                c = 3
                d = 4
                e = 5
                f = 6
                g = 7
                h = 8
                i = 9
                j = 10
                
                result = (a + b) * (c + (d * e) / f * g - (h * i * j))
                message("%stat.player/result%")
            }
        """.trimIndent())
        assertEquals("-2088", result, "Wrong result")
    }

    @Test
    fun complex_expression_5() {
        val result = interpret("""
            fn main() {
                a = 3
                b = 4
                
                result = other(a * b + 5)
                message("%stat.player/result%")
            }
            
            fn other(_x) {
                return _x * 10
            }
        """.trimIndent())
        assertEquals("170", result, "Wrong result")
    }

}