import com.hsc.compiler.driver.Color
import com.hsc.compiler.driver.CompileOptions
import com.hsc.compiler.driver.DriverMode
import com.hsc.compiler.driver.Mode
import com.hsc.compiler.driver.Target
import com.hsc.compiler.driver.VirtualFileProviderFlat
import com.hsc.compiler.driver.runCompiler
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertTrue

fun interpret(src: String): String {
    val systemOut = System.out

    val newOut = ByteArrayOutputStream()
    System.setOut(PrintStream(newOut))

    val result = runCatching {
        val opts = CompileOptions("test", Target.Json, Mode.Optimize, DriverMode.Interpreter, null, Color.Never, false, "_", true, true)
        runCompiler(opts, VirtualFileProviderFlat(src))
    }

    System.setOut(systemOut)

    if (result.isFailure) print(newOut.toString())
    assertTrue(result.isSuccess, "Compilation failed")
    val parts = newOut.toString().split("=====")
    println(parts[1].drop(1))
    return parts[0].dropLast(1).trim().replace("\r", "")
}

fun compile(src: String) {
    val systemOut = System.out

    val newOut = ByteArrayOutputStream()
    System.setOut(PrintStream(newOut))

    val result = runCatching {
        val opts = CompileOptions("test", Target.Json, Mode.Optimize, DriverMode.Diagnostics, null, Color.Never, false, "_", true, true)
        runCompiler(opts, VirtualFileProviderFlat(src))
    }

    System.setOut(systemOut)

    if (result.isFailure) print(newOut.toString())
    assertTrue(result.isSuccess, "Compilation failed")
}