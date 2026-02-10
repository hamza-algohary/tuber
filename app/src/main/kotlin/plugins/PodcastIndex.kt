package plugins

import capabilities.asSequence
import capabilities.countQuery
import capabilities.sqliteConnection
import capabilities.useQuery
import kotlinx.serialization.json.Json
import Backend
import Category
import FormattedText
import FormattedText.Plain
import Items
import MoreItemsProvider
import PlaylistType
import SearchProvider
import SearchResult
import Summary
import Thumbnail
import toPlainText
import java.sql.ResultSet


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
                        lists.addToList(summary)
                        if (index % samplePeriod == 0L)
                            report(index.toLong(), size)
                    }
            }
        }
    }
}

fun String?.asThumbnailUrl() =
    this?.let { Thumbnail(url = this, width = null, height = null) }

fun String?.asPlainText() =
    this?.let {
        Plain(FormattedText.HTML(this).toPlainText())
    }


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


fun Items.toSearchResult() = SearchResult(this,null,null)
