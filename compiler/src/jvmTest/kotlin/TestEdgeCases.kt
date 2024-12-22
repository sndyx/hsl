import kotlin.test.Test
import kotlin.test.assertEquals

class TestEdgeCases {

    @Test
    fun function_call_overwrites_temp() {
        val result = interpret("""
            fn main() {
                _x = 1
                other(2)
                result = _x
                message("%stat.player/result%")
            }
            
            fn other(_x) {
                message("%stat.player/_x%")
            }
        """.trimIndent())
        assertEquals("2\n1", result, "Wrong result")
    }

}