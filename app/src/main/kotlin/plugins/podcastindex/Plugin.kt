package plugins.podcastindex

import capabilities.asSequence
import capabilities.countQuery
import capabilities.sqliteConnection
import capabilities.useQuery
import kotlinx.serialization.json.Json
import plugins.Plugin
import plugins.Items
import plugins.MoreItemsProvider
import plugins.SearchProvider
import plugins.SearchResult
import capabilities.Lists
import capabilities.Page
import capabilities.toItems
import plugins.ContentCategory
import plugins.ContentType
import plugins.SearchProviderInfo


const val pluginName = "podcastindex"

class PodcastIndex(val luceneIndexPath : String) : SearchProvider , MoreItemsProvider {
    val lists by lazy {
        Lists(luceneIndexPath, useVectorEmbeddings = false)
    }
    override val name = pluginName

    override fun search(query: String, filters: List<String>, sortBy: String): SearchResult =
        lists.search(pluginName , query).toSearchResult()
    override fun moreItems(pageToken: String): Items =
        lists.getPage(Json.decodeFromString<Page>(pageToken)).toItems()

    override fun info() =
        SearchProviderInfo(
            name = name,
            displayName = "Podcast Index",
            url = "https://podcastindex.org/",
            iconName = null,
            symbolicIconName = null,
            symbolicIconUrl = null,
            iconUrl = null,
            filters = emptyList(),
            sortOptions = emptyList(),
            contentTypes = listOf(ContentType.PLAYLIST),
            contentCategories = listOf(ContentCategory.PODCAST)
        )
//    fun filters() : List<String> = emptyList()
//    fun sortOptions(): List<String> = emptyList()
}

fun PodcastIndex.toPlugin() = let {
    Plugin(emptyList(), listOf(it), listOf(it), emptyList())
}

private const val QUERY_ALL_PODCASTS = "SELECT * FROM podcasts WHERE explicit = 0;"
/** This method will overwrite any previously indexed podcasts */
fun PodcastIndex.createIndex(
    sqlitePath: String,
    maxNumber : Int = Int.MAX_VALUE ,
    report : (done : Long , total : Long)->Unit = {_,_->},
    samplePeriod : Long = 10000,
    listName : String = pluginName
) {
    sqliteConnection(sqlitePath) {
        val size = countQuery(QUERY_ALL_PODCASTS)
        useQuery(QUERY_ALL_PODCASTS) { results ->
            lists.overwrite(listName) { lists ->
                results.asSequence()
                    .take(maxNumber)
                    .map { it.toSummary() }
                    .forEachIndexed { index, summary ->
                        lists.addToList(summary)
                        if (index % samplePeriod == 0L)
                            report(index.toLong(), size)
                    }
            }
        }
    }
}

