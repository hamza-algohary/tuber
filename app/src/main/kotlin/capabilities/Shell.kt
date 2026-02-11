package capabilities

import CommandNotFoundOrInsuffiecientPermissions
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration

/**
 * You can redirect input,output and errors to files, NULL or pipes (which is the default),
 * visit [java.lang.ProcessBuilder] for more. You can also set environment variables apparently
 */


/** Runs a command and waits on it. */
fun shell(vararg command : String , workingDir: File? = null , timeOut: Duration = Duration.INFINITE , printOutput : Boolean = false , printCommand : Boolean = true) =
    ProcessBuilder(*command).apply {
        if(printCommand) println(command().joinToString(" "))
    }.run().wait(timeOut).also {
        if (printOutput) print(it?.output)
    }


//fun process(
//    vararg commandAndArgs : String,
//    workingDir: File? = null ,
//    stdout : ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE ,
//    stderr : ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE ,
//    stdin : ProcessBuilder.Redirect = ProcessBuilder.Redirect.PIPE ,
//) =
//    ProcessBuilder(*commandAndArgs).directory(workingDir).redirectOutput(stdout).redirectError(stderr).redirectInput(stdin)
//
//fun pipeline(vararg processBuilders : ProcessBuilder) =
//    ProcessBuilder.startPipeline(processBuilders.toList())

data class ProcessResult(val exitCode: Int, val output: String , val error: String)

/** returns null only if timeout is reached without process terminating*/
fun Process.wait(timeOut: Duration = Duration.INFINITE) : ProcessResult? =
    if (waitFor(timeOut.inWholeMicroseconds , java.util.concurrent.TimeUnit.MICROSECONDS))
        ProcessResult(waitFor(), inputStream.dumpToString() , errorStream.dumpToString())
    else null

fun ProcessBuilder.run() =
    try {
        start()
    } catch (e: IOException) {
        throw CommandNotFoundOrInsuffiecientPermissions(command()[0])
    }


fun InputStream.dumpToString() = this.bufferedReader().use { it.readText() }



//private class TeeOutputStream(private vararg val targets: OutputStream) : OutputStream() {
//    override fun write(b: Int) = targets.forEach { it.write(b) }
//    override fun write(b: ByteArray, off: Int, len: Int) = targets.forEach { it.write(b, off, len) }
//    override fun flush() = targets.forEach { it.flush() }
//    override fun close() = targets.forEach { runCatching { it.close() } }
//}
//
//private fun InputStream.redirectAndWait(outputStream: OutputStream) = use {
//    it.copyTo(outputStream)
//    outputStream.close()
//}

