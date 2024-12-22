import kotlin.test.Test
import kotlin.test.assertEquals

class TestLimits {

    @Test
    fun simple() {
        val result = interpret("""
            fn main() {
                other()
            }
            
            fn other() {
                x = 0
                #for (i in 1..15) {
                    x += 1
                }
                message("%stat.player/x%")
            }
        """.trimIndent())
        assertEquals("15", result, "Wrong result")
    }

}