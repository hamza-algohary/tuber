package plugins.newpipe

import plugins.AudioTrackType
import plugins.Chapter
import plugins.FormattedText
import plugins.Info
import plugins.Items
import plugins.PlaylistType
import plugins.PreviewFrames
import plugins.SearchResult
import plugins.Stream
import plugins.StreamType
import plugins.Summary
import plugins.Thumbnail
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.stream.*
import plugins.ContentType
import java.time.LocalTime
import java.time.ZoneOffset

// =============== Search and Summaries =============
fun NewPipeSearchInfo.toSearchResult() : SearchResult =
    SearchResult(Items(relatedItems.toSummaries() , emptyList() , NextPage.SearchNextPage(service.name , searchString , contentFilters , sortFilter , nextPage).toJson()) , searchSuggestion , isCorrectedSearch)

fun InfoItem.toSummary() : Summary? =
    when {
        infoType == InfoItem.InfoType.STREAM   && this is NewPipeStreamInfoItem -> toStreamSummary()
        infoType == InfoItem.InfoType.PLAYLIST && this is NewPipePlaylistInfoItem -> toPlaylistSummary()
        infoType == InfoItem.InfoType.CHANNEL  && this is NewPipeChannelInfoItem -> toChannelSummary()
        else -> null
    }

fun List<InfoItem>.toSummaries() : List<Summary> =
    map(InfoItem::toSummary).filterNotNull()

fun NewPipeStreamInfoItem.toStreamSummary() : Summary.StreamSummary =
    Summary.StreamSummary(
        name ,
        url ,
        thumbnails.map(Image::toThumbnail).toList() ,
        service.name,
        emptyList(),
        emptyList(),
        streamType.toStreamType() ,
        duration ,
        viewCount ,
        uploadDate?.offsetDateTime()?.toEpochSecond(),
        FormattedText.Plain(shortDescription?:"") ,
        Summary.ChannelSummary(
            uploaderName ,
            uploaderUrl?:"" ,
            uploaderAvatars?.map(Image::toThumbnail)?:emptyList() ,
            service.name,
            emptyList(),
            emptyList(),
            isUploaderVerified ,
            null ,
            null ,
            null
        )
    )

fun NewPipePlaylistInfoItem.toPlaylistSummary() : Summary.PlaylistSummary =
    Summary.PlaylistSummary(
        name ,
        url ,
        thumbnails?.map(Image::toThumbnail)?:emptyList() ,
        service.name,
        emptyList(),
        emptyList(),
        Summary.ChannelSummary(
            uploaderName , uploaderUrl?:"" , emptyList() , service.name ,
            emptyList(),
            emptyList(),
            isUploaderVerified , null , null , null
        ) ,
        streamCount ,
        description.toFormattedText() ,
        playlistType.toPlaylistType()
    )

fun NewPipeChannelInfoItem.toChannelSummary() : Summary.ChannelSummary =
    Summary.ChannelSummary(
        name ,
        url ,
        thumbnails?.map(Image::toThumbnail)?:emptyList() ,
        service.name,
        emptyList(),
        emptyList(),
        isVerified ,
        FormattedText.Plain(description?:"") ,
        subscriberCount ,
        streamCount ,
    )


// =============== plugins.Info ================
fun NewPipeStreamInfo.toStreamInfo() : Info.StreamInfo =
    Info.StreamInfo(
        id,
        name,
        url,
        originalUrl,
        service.name,
        emptyList(),
        emptyList(),
        streamType.toStreamType(),
        thumbnails?.map(Image::toThumbnail)?:emptyList(),
        uploadDate.offsetDateTime().toLocalDate().toEpochSecond(LocalTime.now(), ZoneOffset.UTC),
        duration,
        ageLimit,
        description.toFormattedText(),
        viewCount,
        likeCount,
        dislikeCount,
        Summary.ChannelSummary(
            uploaderName,
            uploaderUrl,
            uploaderAvatars?.map(Image::toThumbnail)?:emptyList(),
            service.name,
            emptyList(),
            emptyList(),
            isUploaderVerified,
            subscriberCount = uploaderSubscriberCount,
            description = null,
            streamCount = null
        ),
        Summary.ChannelSummary(
            subChannelName,
            subChannelUrl,
            subChannelAvatars?.map(Image::toThumbnail)?:emptyList(),
            service.name,
            emptyList(),
            emptyList(),
            null,
            null,
            null,
            null
        ),
        relatedItems.toSummaries(),
        startPosition,
        category,
        licence,
        supportInfo,
        languageInfo?.language,
        isShortFormContent,
        tags?:emptyList(),
        streamSegments?.map(StreamSegment::toChapter)?:emptyList(),
        previewFrames?.map(Frameset::toPreviewFrames)?:emptyList(),
        videoStreams?.map { it.toVideo() }?:emptyList(),
        audioStreams?.map { it.toAudio() }?:emptyList(),
        videoOnlyStreams?.map { it.toVideo() }?:emptyList(),
        subtitles?.map { it.toSubtitles() }?:emptyList(),
        hlsUrl = hlsUrl,
        dashUrl = dashMpdUrl
    )
fun NewPipePlaylistInfo.toPlaylistInfo() : Info.PlaylistInfo =
    Info.PlaylistInfo(
        id, name, url, originalUrl, service.name,
        emptyList(),
        emptyList(),
        description.toFormattedText(),
        Items(
            relatedItems.toSummaries() ,
            emptyList(),
            NextPage.PlaylistNextPage(service.name, url, nextPage).toJson()
        ),
        Summary.ChannelSummary(
            uploaderName,
            uploaderUrl,
            uploaderAvatars?.map(Image::toThumbnail)?:emptyList(),
            service.name,
            emptyList(),
            emptyList(),

            null,
            null,
            null,
            null
        ),
        Summary.ChannelSummary(
            subChannelName,
            subChannelUrl,
            subChannelAvatars?.map(Image::toThumbnail)?:emptyList(),
            service.name,
            emptyList(),
            emptyList(),

            null,
            null,
            null,
            null
        ),
        thumbnails?.map(Image::toThumbnail)?:emptyList(), banners?.map(Image::toThumbnail)?:emptyList(), playlistType.toPlaylistType(),
        streamCount
    )
fun NewPipeChannelInfo.toChannelInfo() : Info.ChannelInfo =
    Info.ChannelInfo(
        id,
        name,
        url,
        originalUrl,
        service.name,
        emptyList(),
        emptyList(),
        Summary.ChannelSummary(
            parentChannelName,
            parentChannelUrl,
            parentChannelAvatars?.map(Image::toThumbnail)?:emptyList(),
            service.name,
            emptyList(),
            emptyList(),

            null,
            null,
            null,
            null
        ),
        avatars?.map(Image::toThumbnail)?:emptyList(),
        isVerified,
        FormattedText.Plain(description?:""),
        subscriberCount,
        null,
        banners?.map(Image::toThumbnail)?:emptyList(),
        donationLinks?.toList()?:emptyList(),
        tags?:emptyList(),
        feedUrl,
        tabs?.map{
            ChannelTabInfo.getInfo(service , it)
        }?.map{
            it.toPlaylistInfo()
        }?:emptyList()
    )

fun ChannelTabInfo.toPlaylistInfo() : Info.PlaylistInfo =
    Info.PlaylistInfo(
        id =id, name =name, url =url, originalUrl = originalUrl,
        service = service.name ,
        emptyList(),
        emptyList(),
        null ,
        Items(
            relatedItems.toSummaries() ,
            emptyList(),
            NextPage.TabNextPage(service.name ,nextPage , originalUrl , url, id, contentFilters, sortFilter).toJson() ,
        ) ,
        null , null , emptyList() , emptyList() ,null , null
    )

fun KioskInfo.toPlaylistInfo() : Info.PlaylistInfo =
    Info.PlaylistInfo(
        id =id , name =name , url = url , originalUrl =originalUrl ,
        service = service.name ,
        emptyList(),
        emptyList(),
        null ,
        Items(
            relatedItems.toSummaries() ,
            emptyList(),
            nextPageToken = NextPage.KioskNextPage(service.name , url , nextPage).toJson()
        ),
        uploader = null , subUploader = null , thumbnails = emptyList() , banners = emptyList() , playlistType = null, null
    )

fun SearchResult.toPlaylistInfo(name : String , service : StreamingService , playlistType: PlaylistType = PlaylistType.SUPER) : Info.PlaylistInfo =
    Info.PlaylistInfo (
        id = null , name = name , url = null , originalUrl = null ,
        service = service.name ,
        emptyList(),
        emptyList(),
        null ,
        items , uploader = null , subUploader = null , thumbnails = emptyList() , banners = emptyList() , playlistType = playlistType , null
    )




// ================= Helper Classes ======================
fun Image.toThumbnail() : Thumbnail =
    Thumbnail(url, width, height)
fun StreamSegment.toChapter() =
    Chapter(title , startTimeSeconds, channelName, previewUrl)
fun Frameset.toPreviewFrames() =
    PreviewFrames(urls , frameWidth , frameHeight , totalCount , durationPerFrame, framesPerPageX, framesPerPageY)


// ====================== Streams ========================


fun NewPipeVideoStream.toVideo() =
    Stream.Video(content, width, height, fps, getResolution(), bitrate)
fun NewPipeAudioStream.toAudio() =
    Stream.Audio(content, audioLocale?.language, audioTrackType?.toAudioTrackType(), audioTrackName)
fun NewPipeSubtitlesStream.toSubtitles() =
    Stream.Subtitles(content, locale?.language, isAutoGenerated)



/* ==============================
 * =========== Enums ============
 * ==============================
 */


fun NewPipeAudioTrackType.toAudioTrackType() : AudioTrackType =
    when (this) {
        org.schabi.newpipe.extractor.stream.AudioTrackType.ORIGINAL -> AudioTrackType.ORIGINAL
        org.schabi.newpipe.extractor.stream.AudioTrackType.DUBBED -> AudioTrackType.DUBBED
        org.schabi.newpipe.extractor.stream.AudioTrackType.DESCRIPTIVE -> AudioTrackType.DESCRIPTIVE
        org.schabi.newpipe.extractor.stream.AudioTrackType.SECONDARY -> AudioTrackType.SECONDARY
    }

fun Description.toFormattedText() : FormattedText =
    when (type) {
        Description.HTML -> FormattedText.HTML(content)
        Description.MARKDOWN -> FormattedText.Markdown(content)
        Description.PLAIN_TEXT -> FormattedText.Plain(content)
        else -> FormattedText.Plain(content)
    }

fun StreamingService.ServiceInfo.MediaCapability.toContentTypes() : List<ContentType> =
    when (this) {
        StreamingService.ServiceInfo.MediaCapability.AUDIO -> listOf(ContentType.AUDIO)
        StreamingService.ServiceInfo.MediaCapability.VIDEO-> listOf(ContentType.VIDEO)
        StreamingService.ServiceInfo.MediaCapability.LIVE -> listOf(ContentType.LIVE_AUDIO , ContentType.LIVE_VIDEO)
        StreamingService.ServiceInfo.MediaCapability.COMMENTS -> emptyList()
    }


fun NewPipeStreamType.toStreamType() : StreamType =
    when (this) {
        org.schabi.newpipe.extractor.stream.StreamType.NONE -> throw Exception("plugins.StreamType of plugins.type NONE (THIS SHOULD NEVER HAPPEN)")
        org.schabi.newpipe.extractor.stream.StreamType.VIDEO_STREAM -> StreamType.VIDEO
        org.schabi.newpipe.extractor.stream.StreamType.AUDIO_STREAM -> StreamType.AUDIO
        org.schabi.newpipe.extractor.stream.StreamType.LIVE_STREAM -> StreamType.VIDEO_LIVE
        org.schabi.newpipe.extractor.stream.StreamType.AUDIO_LIVE_STREAM -> StreamType.AUDIO_LIVE
        org.schabi.newpipe.extractor.stream.StreamType.POST_LIVE_STREAM -> StreamType.VIDEO_POST_LIVE
        org.schabi.newpipe.extractor.stream.StreamType.POST_LIVE_AUDIO_STREAM -> StreamType.AUDIO_POST_LIVE
    }

@Suppress("DEPRECATION")
fun NewPipePlaylistType.toPlaylistType() : PlaylistType =
    when (this) {
        NewPipePlaylistType.NORMAL -> PlaylistType.NORMAL
        NewPipePlaylistType.MIX_STREAM -> PlaylistType.MIX_VIDEO
        NewPipePlaylistType.MIX_MUSIC -> PlaylistType.MIX_MUSIC
        NewPipePlaylistType.MIX_CHANNEL -> PlaylistType.MIX_VIDEO
        NewPipePlaylistType.MIX_GENRE -> PlaylistType.MIX_MUSIC_GENRE
    }
