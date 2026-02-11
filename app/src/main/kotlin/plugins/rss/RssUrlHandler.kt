package plugins.rss

import UnableToHandleLinkException
import com.prof18.rssparser.RssParser
import com.prof18.rssparser.model.RssChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import plugins.Plugin
import plugins.Info
import plugins.InfoProvider

private val rssParser = RssParser()

/** Try `runBlocking { }` */
fun parseRssChannelFromUrl(url : String) : RssChannel {
    val result = runBlocking (Dispatchers.Default) {
        rssParser.getRssChannel(url)
    }
    return result
}

val RssUrlHandler = Plugin(
    listOf(
        object : InfoProvider {
            override val name = "rss"
            override fun playlist(url: String): Info.PlaylistInfo =
                    parseRssChannelFromUrl(url).toPlaylist()
            override fun stream(url: String) = throw UnableToHandleLinkException(url)
            override fun channel(url: String) = throw UnableToHandleLinkException(url)
        }
    ),
)
