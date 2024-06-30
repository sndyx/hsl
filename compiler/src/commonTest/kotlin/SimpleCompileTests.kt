import kotlin.test.Test

class SimpleCompileTests {

    @Test
    fun `simple compile test`() {
        "tests/src/simple/simple.hsl".compile().assertSuccess()
    }

    @Test
    fun `all built-ins compile test`() {
        "tests/src/simple/all_builtins.hsl".compile().assertSuccess()
    }

}