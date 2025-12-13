
import kotlinx.serialization.json.Json
import services.*
import services.newpipe.newpipeBackend
import java.lang.Exception
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import old.backend
import kotlin.system.exitProcess

operator fun Backend.plus(other : Backend) =
    Backend(
        infoProviders + other.infoProviders,
        moreItemsProvider + other.moreItemsProvider,
        searchProviders + other.searchProviders ,
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
//    override fun help(context: Context): String =
//        Help().message
}


class SearchProviders : CliktCommand(name = "search-providers") {
    override fun run() {
        backend.searchProviders.map { it.name }.toJson().println()
    }
}

fun Backend.searchProviderFromName(name : String) = searchProviders.find { it.name == name }?: throw UnknownServiceName(name)
fun Backend.catalogProviderFromName(name : String) = catalogProviders.find { it.name == name }?: throw UnknownServiceName(name)


class Search : CliktCommand(name = "search") {
    private val provider by argument("search-provider")
    private val query by argument("query")
    private val filters by option("--filters").split(":").default(emptyList()) //argument("filters").multiple().optional()
    private val sortBy by option("--sort").default("")

    override fun run() {
//        backend.searchProviders
//            .find { it.name == provider }
        println("===filters===")
        filters.forEach { print(it) }
        println("=============")
        backend.searchProviderFromName(provider)
            .search(query , filters , sortBy)
            .toJson()
            .println()
//            ?: throw UnknownServiceName(provider)
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

class Filters : CliktCommand(name = "filters") {
    private val searchProvider : String by argument("search-provider")
    override fun run() {
        backend.searchProviderFromName(searchProvider).filters().toJson().println()
    }
}

class SortOptions : CliktCommand(name = "sort-options") {
    private val searchProvider : String by argument("search-provider")
    override fun run() {
        backend.searchProviderFromName(searchProvider).sortOptions().toJson().println()
    }
}

class Catalogs : CliktCommand(name = "catalogs") {
//    private val searchProvider : String by argument("search-provider")
    override fun run() {
        backend.catalogProviders.map { it.name }.toJson().println()
    }
}

class Catalog : CliktCommand(name = "catalog") {
    private val catalogProvider : String by argument("catalog-provider")
    override fun run() {
        backend.catalogProviderFromName(catalogProvider).catalog().toJson().println()
    }
}

class Help : CliktCommand(name = "help") {
    val repo = "https://github.com/hamza-algohary/tuber"
    val message = """
    Visit $repo for more details.
    Search Commands:
        search-providers                                -> List<String> 
        search <search provider> <query> [--filters <colon separated list>] [--sort <criteria>] -> SearchResult
        filters  <search provider>                      -> List<String>
        sort-options <search provider>                  -> List<String>
    Url Handlers:
        stream   <url>                                  -> StreamInfo
        playlist <url>                                  -> PlaylistInfo
        channel  <url>                                  -> ChannelInfo
    Page Tokens Handler
        more     <pageToken>                            -> Items
    Catalogs/Recommendations
        catalogs                                        -> List<String>
        catalog  <catalog provider>                     -> List<PlaylistInfo>
    ============================================
    Example
        $ tuber search youtube "linux" --filters video:audio --sort date
    To view this message use help
    """.trimIndent()

    override fun run() = echo(message)
}

//class Randomize : CliktCommand(name = "randomize") {
//    private val keywords by option("--keywords").split(":").default(listOf("games","sports","news","trending"))
//    private val services by option("--search-providers").split(":").default(listOf("youtube","peertube","soundcloud"))
//    override fun run() {
//        val results = services.map { service ->
//            keywords.map { keyword ->
//                backend.searchProviderFromName(service).search(keyword).items.items
//            }
//        }.flatten().flatten()
//        results.toJson().println()
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
        Filters(),
        SortOptions(),
        Catalog(),
        Catalogs(),
        Help(),
    ).main(args)

private inline fun <reified T> T.toJson() = Json.encodeToString(this)
private fun String.println() = println(this)
private fun String.print() = print(this)

//class TooFewArgumentsException(message : String? = null) : Exception(message?:"")
//class UnknownCommandException(val command : String) : Exception(command)
class UnableToHandleLinkException(val link : String) : Exception(link)
class InvalidTokenException : Exception()

fun handleCLIExceptions(func : ()->Unit) =
    try {
        func()
    } catch(e : Exception) {
        when (e) {
            is old.UnableToHandleLinkException -> error("Unable to handle link ${e.link}")
            is UnknownServiceName -> error("Unknown service name ${e.name}")
            is old.InvalidTokenException -> error("Invalid token")
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
