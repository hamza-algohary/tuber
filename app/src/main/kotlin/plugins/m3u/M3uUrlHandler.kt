package plugins.m3u

import UnableToHandleLinkException
import capabilities.resolveUrlToString
import net.bjoernpetersen.m3u.M3uParser
import plugins.Plugin
import plugins.Info
import plugins.InfoProvider
import plugins.Summary
import kotlin.io.path.Path

fun parseM3UPlaylistFromUrl(url : String) : List<Summary.GenericSummary> =
    M3uParser.parse(resolveUrlToString(url), Path(url))
        .map { it.toSummary() }

val M3uUrlHandler = Plugin(
    listOf(
        object : InfoProvider {
            override val name = "m3u"
            override fun playlist(url: String): Info.PlaylistInfo =
                parseM3UPlaylistFromUrl(url).toPlaylistInfo(url)
            override fun stream(url: String) = throw UnableToHandleLinkException(url)
            override fun channel(url: String) = throw UnableToHandleLinkException(url)
        }
    ),
    moreItemsProvider = emptyList(),
    searchProviders = emptyList(),
    emptyList()
)
