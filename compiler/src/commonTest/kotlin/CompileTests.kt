import com.hsc.compiler.driver.Color
import com.hsc.compiler.driver.CompileOptions
import com.hsc.compiler.driver.Compiler
import com.hsc.compiler.driver.EmitterType
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.driver.Target
import com.hsc.compiler.driver.runCompiler
import kotlinx.io.files.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

data class CompileOptionsBuilder(
    var houseName: String = "test",
    var target: Target = Target.Htsl,
    var mode: Mode = Mode.Normal,
    var emitter: EmitterType = EmitterType.Internal,
    var output: String? = "test/build",
    var color: Color = Color.Auto,
    var stupidDumbIdiotMode: Boolean = false,
) {
    fun build(): CompileOptions = CompileOptions(houseName, target, mode, emitter, output, color, stupidDumbIdiotMode)
}

class CompilerResult(val compiler: Compiler) {

    fun assertSuccess() {
        assertFalse(compiler.emitter.emittedError, "Compiler emitted error")
    }

    fun assertFailed() {
        assertTrue(compiler.emitter.emittedError, "Compiler did not emit error")
        assertFalse(compiler.emitter.emittedBug, "Compiler emitted bug")
    }

}

fun List<String>.compileWith(
    builder: CompileOptionsBuilder.() -> Unit
): CompilerResult {
    return runCompiler(
        CompileOptionsBuilder().apply(builder).build(),
        map { Path(it) }
    ).let { CompilerResult(it) }
}

fun List<String>.compile(): CompilerResult = compileWith {  }

fun String.compileWith(
    builder: CompileOptionsBuilder.() -> Unit
): CompilerResult = listOf(this).compileWith(builder)

fun String.compile(): CompilerResult = compileWith {  }