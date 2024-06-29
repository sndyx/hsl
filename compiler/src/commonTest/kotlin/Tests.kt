import com.hsc.compiler.driver.Color
import com.hsc.compiler.driver.CompileOptions
import com.hsc.compiler.driver.EmitterType
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.driver.Target
import com.hsc.compiler.driver.runCompiler
import kotlinx.io.files.Path

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

fun List<String>.compileWith(
    builder: CompileOptionsBuilder.() -> Unit
) {
    runCompiler(CompileOptionsBuilder().apply(builder).build(), map { Path(it) })
}

fun List<String>.compile(): Unit = compileWith {  }

fun String.compileWith(
    builder: CompileOptionsBuilder.() -> Unit
): Unit = listOf(this).compileWith(builder)

fun String.compile(): Unit = compileWith {  }