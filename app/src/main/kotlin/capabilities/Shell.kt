package capabilities

import CommandNotFoundOrInsuffiecientPermissions
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.time.Duration


fun shell(vararg command : String , workingDir: File? = null , timeOut: Duration = Duration.INFINITE) =
    process(*command , workingDir=workingDir ).run().wait(timeOut)

fun process(vararg commandAndArgs : String, workingDir: File? = null) =
    ProcessBuilder(*commandAndArgs).directory(workingDir)

fun pipeline(vararg processBuilders : ProcessBuilder) =
    ProcessBuilder.startPipeline(processBuilders.toList())

data class ProcessResult(val exitCode: Int, val output: String , val error: String)

/** returns null only if timeout is reached without process terminating*/
fun Process.wait(timeOut: Duration = Duration.INFINITE) : ProcessResult? =
    if (waitFor(timeOut.inWholeMilliseconds , java.util.concurrent.TimeUnit.MICROSECONDS))
        ProcessResult(waitFor(), inputStream.dumpToString() , errorStream.dumpToString())
     else null

fun ProcessBuilder.run() =
    try {
        start()
    } catch (e: IOException) {
        throw CommandNotFoundOrInsuffiecientPermissions(command()[0])
    }

fun InputStream.dumpToString() = this.bufferedReader().use { it.readText() }

/**
 * You can redirect input,output and errors to files, NULL or pipes (which is the default),
 * visit [java.lang.ProcessBuilder] for more. You can also set environment variables apparently
 */

