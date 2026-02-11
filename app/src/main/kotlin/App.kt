
import kotlinx.coroutines.runBlocking
import plugins.m3u.M3uUrlHandler
import plugins.newpipe.newpipePlugin
import plugins.plus
import plugins.podcastindex.PodcastIndex
import plugins.podcastindex.toPlugin
import plugins.rss.RssUrlHandler

object Backend {
    val podcastindex = PodcastIndex(Config.PODCASTINDEX_INDEX_PATH)
    val plugins = newpipePlugin + podcastindex.toPlugin() + RssUrlHandler + M3uUrlHandler
}

fun main(args: Array<String>) =
    runBlocking {
        handleCLIExceptions {
            CLI(args)
        }
    }
