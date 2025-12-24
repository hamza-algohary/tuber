// src/main/kotlin/tools/PrepareResources.kt
package tasks

import backend.Lists
import java.sql.DriverManager
import services.Category
import services.FormattedText.Plain
import services.PlaylistType
import services.Summary
import services.Thumbnail
import java.io.File
import java.sql.Connection
import java.sql.ResultSet

const val SERVICE_NAME = "podcastindex"
const val LIST_NAME = SERVICE_NAME
private const val PODCASTINDEX_LUCENE_PATH = "src/main/resources/podcastindex.lucene"

object PodcastIndex {
    private val lists by lazy {
        Lists(PODCASTINDEX_LUCENE_PATH,3)
    }
    fun search(query: String) = lists.search(LIST_NAME , query)
    @JvmStatic
    fun main(args: Array<String>) {
        File(PODCASTINDEX_LUCENE_PATH).deleteRecursively()
        indexPodcastIndexDatabase("/home/hamza/Downloads/podcastindex_feeds.db" , lists)
    }
}

fun indexPodcastIndexDatabase(sqlitePath: String, lists : Lists , maxNumber : Int = Int.MAX_VALUE ) {
    val start_time = System.currentTimeMillis()
    var counter = 0L
    println()
    sqliteConnection(sqlitePath)
        .useQuery("SELECT * FROM podcasts WHERE explicit = 0;") { results ->
            results.asSequence()
                .take(maxNumber)
                .map{ it.toSummary() }
                .forEach {
                    lists.add(it,LIST_NAME)
                    if (counter % 100 == 0L)
                        print("\r$counter podcasts indexed.  (average speed = ${counter*1000/(System.currentTimeMillis()-start_time)} docs/sec.) (total time = ${(System.currentTimeMillis()-start_time)/1000} sec)         ")
                    if (counter % 1000 == 0L)
                        lists.index.writer.commit()

                    counter++
                    //println(it)
                }
        }
    lists.index.writer.commit()
//    lists.index.writer.close()
}

fun String?.asThumbnailUrl() =
    this?.let { Thumbnail(url = this, width = null, height = null) }

fun String?.asPlainText() =
    this?.let(::Plain)

fun ResultSet.asSequence(): Sequence<ResultSet> = sequence {
    while (next()) {
        yield(this@asSequence)
    }
}

fun ResultSet.toSummary() : Summary =
    Summary.PlaylistSummary(
        name = getString("title"),
        url = getString("url"),
        thumbnails = listOfNotNull(getString("imageUrl").asThumbnailUrl()),
        service = SERVICE_NAME,
        categories = listOf(Category.PODCAST),
        related = emptyList(),
        description = getString("description").asPlainText(),
        uploader = null,
        streamCount = null,
        playlistType = PlaylistType.NORMAL,
    )

fun sqliteConnection(dbPath : String) =
    DriverManager.getConnection("jdbc:sqlite:$dbPath")

fun Connection.useQuery(query : String, func : (ResultSet)->Unit) =
    prepareStatement(query).use {
        it.executeQuery().use(func)
    }