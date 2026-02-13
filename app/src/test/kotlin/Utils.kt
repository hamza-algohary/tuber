import capabilities.attemptUntilOneSucceeds
import plugins.Plugin
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.PrintStream

fun withStreams(
    out : PrintStream = System.out ,
    err : PrintStream = System.err ,
    input : InputStream = System.`in` ,
    func : ()->Unit
) {

    val stdOut = System.out ; val stdErr = System.err ; val stdIn = System.`in`
    System.setOut(out) ; System.setErr(err) ; System.setIn(input)
    func()
    System.setOut(stdOut) ; System.setErr(stdErr) ; System.setIn(stdIn)
}

fun testMain(vararg args : String , testName : String) {
    File("test_results").mkdirs()
    BuildInfo.isTest = true
    withStreams (out = PrintStream(FileOutputStream("test_results/$testName")) ) {
        main(args.map{it}.toTypedArray())
    }
}

fun red(str: String) = "\u001B[31m$str\u001B[0m"
fun yellow(str: String) = "\u001B[93m$str\u001B[0m"
fun green(str: String) = "\u001B[92m$str\u001B[0m"

fun error(message : String) = println(red("Error: ") + message)
fun warning(message : String) = println(yellow("Warning: ") + message)
fun good(message : String) = println(green("Good: ") + message)

fun Plugin.testSearch() {
    searchProviders.forEach {
        try {
            it.search("linux").let { results ->
                if(results.items.items.isEmpty()) warning("service ${it.info().name} search returned empty list")
                else good("service ${it.info().name} search returned some results")
                Backend.plugins.moreItemsProvider.attemptUntilOneSucceeds { provider ->
                    results.items.nextPageToken?.let { token ->
                        provider.moreItems(token)?.let{ good("Next page token is fine") }?:error("Next page token is not fine")
                    } ?: warning("Next page for search of ${it.info().name} is null")
                } ?: error("Unable to get to next page for service: ${it.info().name}")
            }
        } catch (e:Exception) {
            error("${it.info().name} search threw exception: ${e.stackTraceToString()}")
            null
        }
    }
}
