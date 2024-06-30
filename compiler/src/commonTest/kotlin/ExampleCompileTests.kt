import kotlin.test.Test

class ExampleCompileTests {

    @Test
    fun `simple example compile test`() {
        "examples/simple/src/example.hsl".compile().assertSuccess()
    }

    @Test
    fun `macros example compile test`() {
        "examples/macros/src/example.hsl".compile().assertSuccess()
    }

}