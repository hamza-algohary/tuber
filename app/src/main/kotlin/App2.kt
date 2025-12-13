package old

import kotlinx.serialization.json.Json
import services.*
import services.newpipe.newpipeBackend
import java.lang.Exception
import kotlin.system.exitProcess
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option

//val searchProviders = listOf(*services.newpipe.SearchProviders.all)
//fun searchProviderByName(name : String) =
//    searchProviders.find { it.name == name }
//val infoProviders = listOf(*services.newpipe.InfoProviders.all)

operator fun Backend.plus(other : Backend) =
    Backend(
        infoProviders + other.infoProviders,
        moreItemsProvider + other.moreItemsProvider,
        searchProviders + other.searchProviders,
        catalogProviders + other.catalogProviders
    )

val DEBUG = System.getenv("DEBUG")=="true"
fun <T , O> List<T>.attemptUntilOneSucceeds(func : T.()->O) : O? {
    forEach {
        runCatching {
            return try {
                it.func()
            } catch (e : Exception) {
                if(DEBUG)
                    println(e.stackTraceToString())
                throw e
            }
        }
    }
    return null
}

val backend = newpipeBackend

object CLIServer : CliktCommand() {
    override fun run() = Unit
}


class SearchProviders : CliktCommand(name = "search-providers") {
    override fun run() {
        backend.searchProviders.map { it.name }.toJson().println()
    }
}

class Search : CliktCommand(name = "search") {
    private val provider by argument("provider")
    private val query by argument("query")
    private val filters by option("--filters").multiple() //argument("filters").multiple().optional()

    override fun run() {
        backend.searchProviders
            .find { it.name == provider }
            ?.search(query)
            ?.toJson()
            ?.println()
            ?: throw UnknownServiceName(provider)
    }
}

class More : CliktCommand(name = "more") {
    private val token by argument("pageToken")

    override fun run() {
        backend.moreItemsProvider.attemptUntilOneSucceeds {
            moreItems(token).toJson().println()
        } ?: throw InvalidTokenException()
    }
}

class Stream : CliktCommand(name = "stream") {
    private val url by argument("url")

    override fun run() {
        backend.infoProviders.attemptUntilOneSucceeds {
            stream(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Playlist : CliktCommand(name = "playlist") {
    private val url by argument("url")

    override fun run() {
        backend.infoProviders.attemptUntilOneSucceeds {
            playlist(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Channel : CliktCommand(name = "channel") {
    private val url by argument("url")

    override fun run() {
        backend.infoProviders.attemptUntilOneSucceeds {
            channel(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

//class HelpCmd : CliktCommand(name = "help") {
//    override fun run() {
//        helpMessage().println()
//    }
//}

fun main(args: Array<String>) =
    CLIServer.subcommands(
            SearchProviders(),
            Search(),
            More(),
            Stream(),
            Playlist(),
            Channel(),
        ).main(args)

fun main2 (args : Array<String>) =
    handleCLIExceptions {
        args.toList() into func(
            "search-providers" to {
                backend.searchProviders.map{it.name}.toJson().println()
            } ,
            "help" to {
                helpMessage().println()
            } ,
            "search" to func(2) { args ->
                val (service,query) = args

                backend.searchProviders
                       .find { it.name == service }
                       ?.search(query)
                       ?.toJson()
                       ?.println()
                       ?: throw UnknownServiceName(service)
            } ,
            "stream" to func(1) { (url) ->
                backend.infoProviders.attemptUntilOneSucceeds {
                    stream(url).toJson().println()
                } ?: throw UnableToHandleLinkException(url)
            } ,
            "channel" to func(1) { (url) ->
                backend.infoProviders.attemptUntilOneSucceeds {
                    channel(url).toJson().println()
                } ?: throw UnableToHandleLinkException(url)
            } ,
            "playlist" to func(1) { (url) ->
                backend.infoProviders.attemptUntilOneSucceeds {
                    playlist(url).toJson().println()
                } ?: throw UnableToHandleLinkException(url)
            } ,
            "more" to func(1) { (token) ->
                backend.moreItemsProvider.attemptUntilOneSucceeds {
                    moreItems(token).toJson().println()
                } ?: throw InvalidTokenException()
            }
        )
    }
/**
 * ./gradlew run --args "more {\"service\":\"youtube\",\"query\":\"hello\",\"page\":\"rO0ABXNyACFvcmcuc2NoYWJpLm5ld3BpcGUuZXh0cmFjdG9yLlBhZ2UXoeHzgN3fxQIABVsABGJvZHl0AAJbQkwAB2Nvb2tpZXN0AA9MamF2YS91dGlsL01hcDtMAAJpZHQAEkxqYXZhL2xhbmcvU3RyaW5nO0wAA2lkc3QAEExqYXZhL3V0aWwvTGlzdDtMAAN1cmxxAH4AA3hwcHB0AkRFcDRERWdWb1pXeHNieHFVQTFOQ1UwTkJVWFJhVlZWb2VsZEZNVzVpUlUwMVVWbEpRa015TVVsVU1EVlBXVEZ3YVdRd1VscG5aMFZNWkVaYWMxa3dkSGROTWtwWVUwUnBRMEZSZEV0TldFSnVVMVpqZEdWRmVFbGlORWxDUkZaS1JWZFdSa2xqTVdoT1dqSjRSRTlWUjBOQlVYUlNWbTB4YmxKck5YQlJibXhUVGtsSlFrTjZhRzFUVmxaeVUxVTFWRTFZVm5ablowVk1XakprYjFKR1NrdFdibWhIWlVaWFEwRlJkRUpsU0djMVUxVXhhMVJFVW5GWk5FbENRM3BLUmxZelVuRlVibWd4WWxka2NtZG5SVXhhYlVZMlZGWk9SRmR0WTNSaVdHVkRRVkYwYTJJeFpGaGhNR041V2pGQ1VWSlpTVUpEZWtad1ltdDRlV05ZU210TlZWWldaMmRGVEZOdE5UVlViVGxLVmxoT1FscEdSME5CVVhOM1ZWZGtWVTFyU1hsaE1WcENWRmxKUWtNeFVYaGtSM2N5VG01U2VWZEdVbEpuWjBWTVlXdHpNVkpyT1RWVVYyaEZWREZ0UTBGUmRIZGpWVkpHWkRGQ2NFNHpXalJhTkVsQ1F6SkdhRk5YY0hwUFJYTjZZakZzZG1kblJVeGhlbFl3VFVaS2VXRlhjSGRUUjJWNVFWRlpTMEpCWjFsRlFVeHhRVkZSU1VGb1FWVTRRVVZDR0lIZzZCZ2lDM05sWVhKamFDMW1aV1ZrcHQAPGh0dHBzOi8vd3d3LnlvdXR1YmUuY29tL3lvdXR1YmVpL3YxL3NlYXJjaD9wcmV0dHlQcmludD1mYWxzZQ==\"}" > test_results/youtube_search_token.json
 */


infix fun <T> T.into(func : (T)->Unit) = func(this)

val repo = "https://github.com/hamza-algohary/tuber"
fun helpMessage() =
    """
    Visit $repo for more details.
    commands:
        search-providers                                -> List<String>
        search   <search provider> <query> [filters]    -> SearchResult
        more     <pageToken>                            -> Items
        stream   <url>                                  -> StreamInfo
        playlist <url>                                  -> PlaylistInfo
        channel  <url>                                  -> ChannelInfo
        help
    """.trimIndent()

private inline fun <reified T> T.toJson() = Json.encodeToString(this)
private fun String.println() = println(this)
private fun String.print() = print(this)


typealias CommandLineFunction = (List<String>)->Unit
// A set of pairs of names of commands and their handlers.
fun func(vararg commands : Pair<String,CommandLineFunction> ) : CommandLineFunction = { args ->
    args.ifEmpty {
        throw TooFewArgumentsException()
    }
    commands.find { it.first == args[0] }
            ?.second?.invoke(args.drop(1))?:throw UnknownCommandException(args[0])
}

fun func(minArgs : Int = 0 , handler : CommandLineFunction) : CommandLineFunction = { args ->
    if (args.size < minArgs)
        throw TooFewArgumentsException()
    handler(args)
}

//fun List<String>.parameters() : Map<String,String> =
//    filter{ "=" in it }.
//    map {
//        it.split("=" , limit = 2)
//    }
//
//fun List<String>.parseOptionalList(name : String , delimiter : String = ":") : List<String> =
//    findLast { name in it }?.split("=", limit = 2)?.getOrNull(1)?.split(":")

class TooFewArgumentsException(message : String? = null) : Exception(message?:"")
class UnknownCommandException(val command : String) : Exception(command)
class UnableToHandleLinkException(val link : String) : Exception(link)
class InvalidTokenException : Exception()

fun handleCLIExceptions(func : ()->Unit) =
    try {
        func()
    } catch(e : Exception) {
        when (e) {
            is TooFewArgumentsException -> error("Too few arguments. ${e.message}")
            is UnknownCommandException -> error("Unknown command ${e.command}")
            is UnableToHandleLinkException -> error("Unable to handle link ${e.link}")
            is UnknownServiceName -> error("Unknown service name ${e.name}")
            is InvalidTokenException -> error("Invalid token")
            else -> if (DEBUG) {
                throw e
            } else {
                error(e.message?:"")
            }
        }
    }

fun error(message: String = "" , exitCode : Int = 1) {
    System.err.println(message)
    exitProcess(exitCode)
}

