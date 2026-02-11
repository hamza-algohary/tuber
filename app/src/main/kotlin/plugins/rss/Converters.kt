package plugins.rss

import plugins.Items
import com.prof18.rssparser.model.RssChannel
import com.prof18.rssparser.model.RssItem
import org.schabi.newpipe.extractor.ServiceList
import plugins.Info
import plugins.Stream
import plugins.newpipe.name
import plugins.podcastindex.asPlainText
import plugins.podcastindex.asThumbnailUrl


fun RssChannel.toPlaylist() =
    Info.PlaylistInfo(
        url = link,
        originalUrl = link,
        id = youtubeChannelData?.channelId,
        name = title,
        service = null,
        categories = emptyList(),
        related = emptyList(),
        description = description.asPlainText(),
        items = items.toItems(),
        uploader = null,
        subUploader = null,
        thumbnails = listOfNotNull(image?.url?.asThumbnailUrl()),
        banners = emptyList(),
        playlistType = null,
        numberOfItems = null,
    )

fun List<RssItem>.toItems() =
    Items(
        items = emptyList(),
        detailedItems = mapNotNull  {
            it.toStreamInfo()
        } ,
        nextPageToken = null,
    )

fun RssItem.toStreamInfo() =
    Info.StreamInfo(
        id = guid ?: youtubeItemData?.videoId,
        name = title,
        url = link ?: youtubeItemData?.videoUrl,
        originalUrl = link ?: youtubeItemData?.videoUrl,
        service = youtubeItemData?.let { ServiceList.YouTube.name },
        categories = emptyList(),
        related = emptyList(),
        streamType = null,
        thumbnails = listOfNotNull(image?.asThumbnailUrl() , youtubeItemData?.thumbnailUrl?.asThumbnailUrl()),
        uploadTimeStamp = null,
        duration = null,
        ageLimit = null,
        description = description?.asPlainText() ?: youtubeItemData?.description?.asPlainText(),
        viewCount = youtubeItemData?.viewsCount?.toLong() ,
        likeCount = youtubeItemData?.likesCount?.toLong(),
        dislikeCount = null,
        uploader = null,
        subUploader = null,
        recommendations = emptyList(),
        startPosition = null,
        category = null,
        license = null,
        supportInfo = null,
        language = null,
        short = false,
        tags = emptyList(),
        chapters = emptyList(),
        previewFrames = emptyList(),
        videoStreams = video?.let {
            listOfNotNull(
                Stream.Video(video,null,null,null,null,null)
            )
        } ?: emptyList(),
        audioStreams = audio?.let {
            listOfNotNull(
                Stream.Audio(audio , null , null , null)
            )
        } ?: emptyList(),
        videoOnlyStreams = emptyList(),
        subtitles = emptyList(),
        hlsLink = null,
        dashLink = null
    )
