
import backend.Lists
import backend.M3uUrlHandler
import backend.PodcastIndex
import backend.RssUrlHandler
import backend.createIndex
import backend.toBackend
import capabilities.attemptUntilOneSucceeds
import kotlinx.serialization.json.Json
import services.*
import services.newpipe.newpipeBackend
import java.lang.Exception
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import kotlinx.serialization.decodeFromString
import java.io.File
import java.util.Scanner
import kotlin.system.exitProcess
import capabilities.DEBUG
import capabilities.globalJsonSerializer
import kotlinx.coroutines.Dispatchers

fun <T> Try(func : ()->T) : T? =
    try { func() } catch (e : Exception) { null }

operator fun Backend.plus(other : Backend) =
    Backend(
        infoProviders + other.infoProviders,
        moreItemsProvider + other.moreItemsProvider,
        searchProviders + other.searchProviders ,
        catalogProviders + other.catalogProviders
    )



val podcastindex = PodcastIndex(Config.PODCASTINDEX_INDEX_PATH)
val backend = newpipeBackend + podcastindex.toBackend() + RssUrlHandler + M3uUrlHandler

fun InfoProvider.infoFromUrl(url : String) : Info? =
    Try { stream(url) } ?: Try { playlist(url) } ?: Try { channel(url) }
fun Backend.infoFromUrl(url : String) =
    infoProviders.attemptUntilOneSucceeds { provider -> provider.infoFromUrl(url) }



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
        backend.moreItemsProvider.attemptUntilOneSucceeds { provider ->
            provider.moreItems(token).toJson().println()
        } ?: throw InvalidTokenException()
    }
}

class Stream : CliktCommand(name = "stream") {
    private val url by argument("url")

    override fun run() {
        backend.infoProviders.attemptUntilOneSucceeds {provider ->
            provider.stream(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Playlist : CliktCommand(name = "playlist") {
    private val url by argument("url")

    override fun run() {
        backend.infoProviders.attemptUntilOneSucceeds { provider ->
            provider.playlist(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Channel : CliktCommand(name = "channel") {
    private val url by argument("url")

    override fun run() {
        backend.infoProviders.attemptUntilOneSucceeds { provider ->
            provider.channel(url).toJson().println()
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

const val NULL_CHAR = Char.MIN_VALUE
class PreparePodcastindex : CliktCommand(name = "prepare-podcastindex") {
    private val databasePath : String by argument("podcastindex-sqlitedb-path")
    override fun run() {
        podcastindex.createIndex(
            databasePath ,
            report = { progress,total ->
                Progress(progress,total).toJson().plus(NULL_CHAR).println()
            }
        )
    }
}

val mediaLists = Lists(Config.MEDIA_LISTS_PATH)
class ListsCommand : CliktCommand(name = "lists") {
    override fun run() =
        mediaLists.lists().toJson().println()
}

class ListSearch : CliktCommand(name = "list-search") {
    val listName by argument("list-name")
    val query by argument("query")
    override fun run() {
        mediaLists.search(listName,query).toJson().println()
    }
}


class ListAdd : CliktCommand("list-add") {
    val listName : String by argument("list-name")
    val url : String by argument("url")
    override fun run(): Unit {
        backend.infoFromUrl(url)?.let { info ->
            mediaLists.commit(listName) {
                addToList(info.toSummary())
                if (info is Info.PlaylistInfo) {
                    info.iter(backend.moreItemsProvider).forEach {
                        addToList(it)
                    }
                }
            }
        } ?: throw UnableToHandleLinkException(url)
    }
}

class ListRemove : CliktCommand("list-remove") {
    val listName : String by argument("list-name")
    val url : String by argument("url")
    override fun run(): Unit {
        backend.infoFromUrl(url)?.let { info ->
            info.url?.let { url ->
                mediaLists.commit(listName) {
                    removeFromList(url)
                    if (info is Info.PlaylistInfo) {
                        info.iter(backend.moreItemsProvider).mapNotNull { it.url }.forEach {
                            removeFromList(it)
                        }
                    }
                }
            }
        } ?: throw UnableToHandleLinkException(url)
    }
}

class ListDelete : CliktCommand("list-delete") {
    val listName by argument("list-name")
    override fun run() {
        mediaLists.deleteList(listName)
    }

}

class ListExport : CliktCommand("list-export") {
    val listName : String by argument("list-name")
    val path : String by argument("path")
    override fun run() {
        java.io.File(path).writer().use { file ->
            mediaLists.getAll(listName).forEach { item ->
                file.write(item.toJson() + NULL_CHAR)
            }
        }
    }
}

class ListImport : CliktCommand("list-import") {
    val listName : String by argument("list-name")
    val path : String by argument("path")
    override fun run() {
        Scanner(File(path)).use { scanner ->
            scanner.useDelimiter(NULL_CHAR.toString())
            while(scanner.hasNext())
                mediaLists.commit(listName) {
                    addToList(Json.decodeFromString<Summary>(scanner.next()))
                }
        }
    }
}

class ListChannels : CliktCommand("list-channels") {
    val listName : String by argument("list-name")
    override fun run() {
        mediaLists.channels(listName).toJson().println()
    }
}

class ListServices : CliktCommand("list-services") {
    val listName : String by argument("list-name")
    override fun run() {
        mediaLists.services(listName).toJson().println()
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
        more     <page token>                            -> Items
    Catalogs/Recommendations
        catalogs                                        -> List<String>
        catalog  <catalog provider>                     -> List<PlaylistInfo>
    Lists
        lists                              -> List<String> # available lists names
        list-add    <list name> <url>      # Adds url of stream, playlist or channel to list. Adding a playlist also adds its entire content.
        list-remove <list name> <url>      # Removes an item from list, removing a playlist removes all its content
        list-search <list name> <query>    # Search inside a list
        list-export <list name> <path>     # Export an entire list to a file (a text file of NULL delimited stream of Summary JSONs)
        list-import <list name> <path>     # Import an entire list from a file
        list-delete <list name>            # Deletes an entire list
        list-services <list name>          # Get all services used in a list
        list-channels <list name>          # Get all explicitly added channels to a list
    Others
        prepare-podcastindex <path to podcastindex sqlite db>  -> Progressive null-delimited stream of Progress objects each in Json format.
    
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

fun main(args: Array<String>) {
    handleCLIExceptions {
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
            PreparePodcastindex(),
            ListsCommand(),
            ListAdd(),
            ListRemove(),
            ListSearch(),
            ListChannels(),
            ListServices(),
            ListImport(),
            ListExport(),
            ListDelete(),
        ).main(args)
    }
    mediaLists.close()
    System.exit(0) // We have to do it because either I know nothing about coroutines, or they are just DISGUSTING (ps: it's the latter)
}

private inline fun <reified T> T.toJson() = globalJsonSerializer.encodeToString(this)
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
