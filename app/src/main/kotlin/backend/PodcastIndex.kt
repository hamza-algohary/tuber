package backend

import capabilities.asSequence
import capabilities.countQuery
import capabilities.sqliteConnection
import capabilities.useQuery
import kotlinx.serialization.json.Json
import services.Backend
import services.Category
import services.FormattedText
import services.FormattedText.Plain
import services.Items
import services.MoreItemsProvider
import services.PlaylistType
import services.SearchProvider
import services.SearchResult
import services.Summary
import services.Thumbnail
import java.sql.ResultSet

//val podcastIndexBackend = Backend(
//
//)
const val backendName = "podcastindex"

class PodcastIndex(val luceneIndexPath : String) : SearchProvider , MoreItemsProvider {
    val lists by lazy {
        Lists(luceneIndexPath , useVectorEmbeddings=false)
    }
    override val name = backendName
    override fun search(query: String, filters: List<String>, sortBy: String): SearchResult =
        lists.search(backendName , query).toSearchResult()

    override fun filters() : List<String> = emptyList()
    override fun sortOptions(): List<String> = emptyList()

    override fun moreItems(pageToken: String): Items =
        lists.getPage(Json.decodeFromString<Page>(pageToken)).toItems()
}

fun PodcastIndex.toBackend() = let {
    Backend(emptyList(), listOf(it), listOf(it), emptyList())
}
private const val QUERY_ALL_PODCASTS = "SELECT * FROM podcasts WHERE explicit = 0;"
/** This method will overwrite any previously indexed podcasts */
fun PodcastIndex.createIndex(
    sqlitePath: String,
//    lists : Lists = lists,
    maxNumber : Int = Int.MAX_VALUE ,
    report : (done : Long , total : Long)->Unit = {_,_->},
    samplePeriod : Long = 10000,
    listName : String = backendName
) {
    sqliteConnection(sqlitePath) {
        val size = countQuery(QUERY_ALL_PODCASTS)
        useQuery(QUERY_ALL_PODCASTS) { results ->
            lists.overwrite(listName) { lists ->
                results.asSequence()
                    .take(maxNumber)
                    .map { it.toSummary() }
                    .forEachIndexed { index, summary ->
                        lists.addToList(summary, listName)
                        if (index % samplePeriod == 0L)
                            report(index.toLong(), size)
                    }
            }
        }
    }
//    lists.close()
}

fun String?.asThumbnailUrl() =
    this?.let { Thumbnail(url = this, width = null, height = null) }

fun String?.asPlainText() =
    this?.let(::Plain)


fun ResultSet.toSummary() : Summary =
    Summary.PlaylistSummary(
        name = getString("title"),
        url = getString("url"),
        thumbnails = listOfNotNull(getString("imageUrl").asThumbnailUrl()),
        service = backendName,
        categories = listOf(Category.PODCAST),
        related = emptyList(),
        description = FormattedText.HTML(getString("description")),
        uploader = null,
        numberOfItems = null,
        playlistType = PlaylistType.NORMAL,
    )

fun LuceneSearchResults.toItems() : Items =
    Items(
        items = items.toSummaries(),
        nextPageToken = Json.encodeToString(nextPage) //nextPage?.toBinaryString()
    )

fun Items.toSearchResult() = SearchResult(this,null,null)
//
//fun Page.toResults(lists: Lists) : Results =
//    lists.getPage(this).let {
//        Results(
//            ,
//        )
//    }


fun List<ScoredDocument>.toSummaries() = mapNotNull { it.document.toSummary() }


//fun Results.toSearchResults
