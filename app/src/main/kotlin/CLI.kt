import capabilities.attemptUntilOneSucceeds
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import kotlinx.serialization.json.Json
import plugins.Info
import capabilities.Lists
import plugins.Progress
import plugins.Summary
import plugins.catalogProviderFromName
import plugins.infoFromUrl
import plugins.iter
import plugins.podcastindex.createIndex
import plugins.searchProviderFromName
import plugins.toSummary
import java.io.File
import java.util.Scanner
import kotlin.sequences.forEach


//fun cli(args: Array<String>) {
//    runBlocking {
//        handleCLIExceptions {
//            CLI.main(args)
//        }
//    }
////    System.exit(0) // We have to do it because either I know nothing about coroutines, or they are just DISGUSTING (ps: it's the latter)
//}

private inline fun <reified T> T.toJson() = Json.encodeToString(this)
private inline fun <reified T> fromJson(string : String) = Json.decodeFromString<T>(string)
private fun String.println() = println(this)
private fun String.print() = print(this)

object CLI : CliktCommand() {
    override fun run() = Unit
    //override fun help(context: Context) = Help().message
    init {
        subcommands(
            SearchProviders(), Search(), More(), StreamCommand(), Playlist(), Channel(),
            Catalog(), Catalogs(), Help(), PreparePodcastindex(), ListsCommand(),
            ListAdd(), ListRemove(), ListSearch(), ListChannels(), ListServices(), ListImport(),
            ListExport(), ListDelete(), ListReindex(), VersionCommand(),
            /** Filters(), SortOptions(),*/
        )
    }
    operator fun invoke(args : Array<String>) {
        main(args)
        mediaLists.close()
    }
}

class SearchProviders : CliktCommand(name = "search-providers") {
    override fun run() {
        Backend.plugins.searchProviders.map { it.info() }.toJson().println()
    }
}

class Search : CliktCommand(name = "search") {
    private val provider by argument("search-provider")
    private val query by argument("query")
    private val filters by option("--filters").split(":").default(emptyList()) //argument("filters").multiple().optional()
    private val sortBy by option("--sort").default("")

    override fun run() {
        Backend.plugins.searchProviderFromName(provider)
            .search(query , filters , sortBy)
            .toJson()
            .println()
    }
}

class More : CliktCommand(name = "more") {
    private val token by argument("pageToken")

    override fun run() {
        Backend.plugins.moreItemsProvider.attemptUntilOneSucceeds { provider ->
            provider.moreItems(token).toJson().println()
        } ?: throw InvalidTokenException()
    }
}

class StreamCommand : CliktCommand(name = "stream") {
    private val url by argument("url")

    override fun run() {
        Backend.plugins.infoProviders.attemptUntilOneSucceeds { provider ->
            provider.stream(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Playlist : CliktCommand(name = "playlist") {
    private val url by argument("url")

    override fun run() {
        Backend.plugins.infoProviders.attemptUntilOneSucceeds { provider ->
            provider.playlist(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Channel : CliktCommand(name = "channel") {
    private val url by argument("url")

    override fun run() {
        Backend.plugins.infoProviders.attemptUntilOneSucceeds { provider ->
            provider.channel(url).toJson().println()
        } ?: throw UnableToHandleLinkException(url)
    }
}

class Filters : CliktCommand(name = "filters") {
    private val searchProvider : String by argument("search-provider")
    override fun run() {
        Backend.plugins.searchProviderFromName(searchProvider).info().filters.toJson().println()
    }
}

class SortOptions : CliktCommand(name = "sort-options") {
    private val searchProvider : String by argument("search-provider")
    override fun run() {
        Backend.plugins.searchProviderFromName(searchProvider).info().sortOptions.toJson().println()
    }
}

class Catalogs : CliktCommand(name = "catalogs") {
    //    private val searchProvider : String by argument("search-provider")
    override fun run() {
        Backend.plugins.catalogProviders.map { it.name }.toJson().println()
    }
}

class Catalog : CliktCommand(name = "catalog") {
    val catalogProvider : String by argument("catalog-provider")
    override fun run() {
        Backend.plugins.catalogProviderFromName(catalogProvider).catalog().toJson().println()
    }
}

const val NULL_CHAR = Char.MIN_VALUE
class PreparePodcastindex : CliktCommand(name = "prepare-podcastindex") {
    private val databasePath : String by argument("podcastindex-sqlitedb-path")
    override fun run() {
        Backend.podcastindex.createIndex(
            databasePath ,
            report = { progress,total ->
                Progress(progress, total).toJson().plus(NULL_CHAR).println()
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
        Backend.plugins.infoFromUrl(url)?.let { info ->
            mediaLists.commit(listName) {
                addToList(info.toSummary())
                if (info is Info.PlaylistInfo) {
                    info.iter(Backend.plugins.moreItemsProvider).forEach {
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
        Backend.plugins.infoFromUrl(url)?.let { info ->
            info.url?.let { url ->
                mediaLists.commit(listName) {
                    removeFromList(url)
                    if (info is Info.PlaylistInfo) {
                        info.iter(Backend.plugins.moreItemsProvider).mapNotNull { it.url }.forEach {
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
                    addToList(fromJson<Summary>(scanner.next()))
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

class ListReindex : CliktCommand(name = "list-reindex") {
    val listName : String by argument("list-name")
    override fun run() {
        mediaLists.reindex(listName)
    }
}


class Help : CliktCommand(name = "help") {
    val repo = "https://github.com/hamza-algohary/tuber"
    val message = """
    Visit $repo for more details.
        version                                         -> Tuber version (Major.Minor.Patch)
    Search Commands:
        search-providers                                -> List<SearchProviderInfo> 
        search <search provider> <query> [--filters <colon separated list>] [--sort <criteria>] -> plugins.SearchResult
        [DO NOT USE] filters  <search provider>                      -> List<String> [OLD API, NOT AVAILABLE ANY MORE. Use info provided by SearchProviderInfo instead]
        [DO NOT USE] sort-options <search provider>                  -> List<String> [OLD API, NOT AVAILABLE ANY MORE. Use info provided by SearchProviderInfo instead]
    Url Handlers:
        stream   <url>                                  -> StreamInfo
        playlist <url>                                  -> PlaylistInfo
        channel  <url>                                  -> ChannelInfo
    Page Tokens Handler
        more     <page token>                            -> plugins.Items
    Catalogs/Recommendations
        catalogs                                        -> List<String>
        catalog  <catalog provider>                     -> List<PlaylistInfo>
    Lists
        lists                              -> List<String> # available lists names
        list-add    <list name> <url>      # Adds url of stream, playlist or channel to list. Adding a playlist also adds its entire content.
        list-remove <list name> <url>      # Removes an item from list, removing a playlist removes all its content
        list-search <list name> <query>    # Search inside a list
        list-export <list name> <path>     # Export an entire list to a file (a text file of NULL delimited stream of plugins.Summary JSONs)
        list-import <list name> <path>     # Import an entire list from a file
        list-delete <list name>            # Deletes an entire list
        list-services <list name>          # Get all services used in a list
        list-channels <list name>          # Get all explicitly added channels to a list
    Others
        prepare-podcastindex <path to podcastindex sqlite db>  -> Progressive null-delimited stream of plugins.Progress objects each in Json format.
    
    ============================================
    Example
        $ tuber search youtube "linux" --filters video:audio --sort date
    To view this message use help
    """.trimIndent()

    override fun run() = echo(message)
}

class VersionCommand : CliktCommand(name = "version") {
    override fun run() {
        println(BuildInfo.currentVersion.toJson())
    }
}


