package plugins.m3u

import plugins.Items
import plugins.Summary
import net.bjoernpetersen.m3u.model.M3uEntry
import plugins.Info
import plugins.PlaylistType
import plugins.podcastindex.asThumbnailUrl


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

fun List<Summary>.toPlaylistInfo(url : String) =
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
            items = this,
            detailedItems = emptyList(),
            nextPageToken = null,
        ),
        uploader = null,
        subUploader = null,
        thumbnails = emptyList(),
        banners = emptyList(),
        playlistType = PlaylistType.SUPER,
        numberOfItems = this.size.toLong(),
    )

