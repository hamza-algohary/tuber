package plugins.m3u

import UnableToHandleLinkException
import capabilities.resolveUrlToString
import net.bjoernpetersen.m3u.M3uParser
import plugins.Plugin
import plugins.Info
import plugins.InfoProvider
import kotlin.io.path.Path

fun parseM3UPlaylistFromUrl(url : String) : Info.PlaylistInfo =
    M3uParser.parse(resolveUrlToString(url), Path(url))
             .groupBy { it.groupTitle }
             .map { (groupTitle, entries) -> entries.map { it.toSummary() }.summariesToPlaylistInfo(name = groupTitle , url) }
             .infosToPlaylistInfo(null , url)

val M3uUrlHandler = Plugin(
    listOf(
        object : InfoProvider {
            override val name = "m3u"
            override fun playlist(url: String): Info.PlaylistInfo =
                parseM3UPlaylistFromUrl(url)
            override fun stream(url: String) = throw UnableToHandleLinkException(url)
            override fun channel(url: String) = throw UnableToHandleLinkException(url)
        }
    ),
    moreItemsProvider = emptyList(),
    searchProviders = emptyList(),
    emptyList()
)
