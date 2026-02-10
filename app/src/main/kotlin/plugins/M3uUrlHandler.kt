package plugins

import UnableToHandleLinkException
import capabilities.resolveUrlToString
import net.bjoernpetersen.m3u.M3uParser
import net.bjoernpetersen.m3u.model.M3uEntry
import Plugin
import Info
import InfoProvider
import Items
import PlaylistType
import Summary

fun parseM3UPlaylistFromUrl(url : String) : List<Summary.GenericSummary> =
    M3uParser.parse(resolveUrlToString(url))
        .map { it.toSummary() }

val M3uUrlHandler = Plugin(
    listOf(
        object : InfoProvider {
            override val name = "m3u"
            override fun playlist(url: String): Info.PlaylistInfo =
                parseM3UPlaylistFromUrl(url).let {
                    Info.PlaylistInfo(
                        url = url,
                        id = null,
                        name = null,
                        originalUrl = url,
                        service = null,
                        categories = emptyList(),
                        related = emptyList(),
                        description = null,
                        items = Items(
                            items = it,
                            detailedItems = emptyList(),
                            nextPageToken = null,
                        ),
                        uploader = null,
                        subUploader = null,
                        thumbnails = emptyList(),
                        banners = emptyList(),
                        playlistType = PlaylistType.SUPER,
                        numberOfItems = it.size.toLong(),
                    )
                }

            override fun stream(url: String) = throw UnableToHandleLinkException(url)
            override fun channel(url: String) = throw UnableToHandleLinkException(url)
        }
    ),
    moreItemsProvider = emptyList(),
    searchProviders = emptyList(),
    emptyList()
)

fun M3uEntry.toSummary() : Summary.GenericSummary =
    Summary.GenericSummary(
        name = channelName,
        url = location.url.toString(),
        thumbnails = listOfNotNull(logoUrl.asThumbnailUrl()),
        service = null,
        categories = emptyList(),
        related = emptyList(),
        description = null
    )


private fun M3uEntry.meta(key: String): String? =
    metadata[key]?.takeIf { it.isNotBlank() }

val M3uEntry.channelName get() = meta("tvg-name")?:title
val M3uEntry.groupTitle get() = meta("group-title")
val M3uEntry.channelId get() = meta("tvg-id")
val M3uEntry.channelNumber get() = meta("tvg-chno")?.toIntOrNull()
val M3uEntry.language get() = meta("tvg-language")
val M3uEntry.country get() = meta("tvg-country")

val M3uEntry.logoUrl get() =
    meta("tvg-logo") ?:
    meta("logo") ?:
    meta("tvg-logo-small")