package plugins

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class SearchProviderInfo(
    val name : String,
    val displayName : String,
    val url : String?,

    /** From System Icon Theme */
    val iconName : String?,
    /** From System Icon Theme */
    val symbolicIconName : String?,
    /** URL may be local path */
    val symbolicIconUrl : Thumbnail?,
    /** URL may be local path */
    val iconUrl : Thumbnail?,

    val filters : List<String>,
    val sortOptions : List<String>,
    val contentTypes : List<ContentType>,
    val contentCategories : List<ContentCategory>,
)

@Serializable
data class SearchResult (
    val items : Items,
    val suggestion : String?,
    val isCorrected : Boolean?
)

@Serializable
data class Items (
    val items : List<Summary>,
    val detailedItems : List<Info>,
    val nextPageToken : String?,
)

@Serializable
sealed class Summary {
    abstract val name : String?
    abstract val url : String?
    abstract val thumbnails : List<Thumbnail>
    abstract val service : String?
    abstract val description : FormattedText?
    abstract val categories : List<ContentCategory>
    abstract val related : List<Related>
    @Serializable
    @SerialName("stream")
    data class StreamSummary(
        override val name : String?, override val url : String?, override val thumbnails : List<Thumbnail>, override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,
        val streamType : StreamType?,
        val duration : Long?,
        val views : Long?,
        @Contextual val uploadDateUnixEpoch : Long?,
        override val description : FormattedText?,
        val uploader : ChannelSummary?
    ) : Summary()

    @Serializable
    @SerialName("playlist")
    data class PlaylistSummary(
        override val name : String?, override val url : String?, override val thumbnails : List<Thumbnail>, override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,
        val uploader: ChannelSummary?,
        val numberOfItems: Long?,
        override val description : FormattedText?,
        val playlistType : PlaylistType?
    ) : Summary()

    @Serializable
    @SerialName("channel")
    data class ChannelSummary(
        override val name : String?, override val url : String?, override val thumbnails : List<Thumbnail>, override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,
        val verified: Boolean?,
        override val description : FormattedText?,
        val subscriberCount : Long?,
        val streamCount : Long?,
    ) : Summary()

    @Serializable
    @SerialName("generic")
    data class GenericSummary(
        override val name : String?,
        override val url : String?,
        override val thumbnails : List<Thumbnail>,
        override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,
        override val description : FormattedText?,
    ) : Summary()
    /** A lighter version of ChannelSummary to wrap info useful for each StreamSummary's channel */
    //    data class Uploader(override val name : String, override val url : String, override val thumbnails : List<plugins.Thumbnail>, val verified : Boolean) : plugins.Summary(name, url, thumbnails)
}

@Serializable
sealed class Info {
    abstract val id : String?
    abstract val name : String?
    abstract val url : String?
    abstract val originalUrl : String?
    abstract val service : String?
    abstract val categories : List<ContentCategory>
    abstract val related : List<Related>
    @SerialName("stream")
    @Serializable data class StreamInfo(
        override val id: String?,
        override val name: String?,
        override val url: String?,
        override val originalUrl: String?,
        override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,

        val streamType: StreamType?,
        val thumbnails: List<Thumbnail>,
        val uploadTimeStamp: Long?,
        val duration: Long?,
        val ageLimit: Int?,
        val description: FormattedText?,
        val viewCount: Long?,
        val likeCount: Long?,
        val dislikeCount: Long?,
        val uploader: Summary.ChannelSummary?,
        val subUploader: Summary.ChannelSummary?,
        val recommendations: List<Summary>,
        val startPosition: Long?,

        val category: String?,
        val license: String?,
        val supportInfo: String?,
        val language: String?,
        val short: Boolean?,
        val tags: List<String>,
        val chapters: List<Chapter>,
        /** Each plugins.PreviewFrames object contains the preview frames of the entire
         *  video, but in a different resolution (width x height) */
        val previewFrames: List<PreviewFrames>,
        val videoStreams: List<Stream.Video>,
        val audioStreams: List<Stream.Audio>,
        val videoOnlyStreams: List<Stream.Video>,
        val subtitles: List<Stream.Subtitles>,
        val hlsUrl: String?,
        val dashUrl: String?,
    ) : Info()
    @SerialName("playlist")
    @Serializable data class PlaylistInfo(
        override val id: String?,
        override val name: String?,
        override val url: String?,
        override val originalUrl: String?,
        override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,

        val description: FormattedText?,
        val items: Items?,
        val uploader: Summary.ChannelSummary?,
        val subUploader: Summary.ChannelSummary?,
        val thumbnails: List<Thumbnail>,
        val banners: List<Thumbnail>,
        val playlistType: PlaylistType?,
        val numberOfItems: Long?,
    ) : Info()
    @SerialName("channel")
    @Serializable data class ChannelInfo(
        override val id: String?,
        override val name: String?,
        override val url: String?,
        override val originalUrl: String?,
        override val service: String?,
        override val categories: List<ContentCategory>,
        override val related: List<Related>,

        val parentChannel : Summary.ChannelSummary?,
        val avatars : List<Thumbnail>,
        val verified: Boolean?,
        val description: FormattedText?,
        val subscriberCount: Long?,
        val streamCount: Long?,
        val banners : List<Thumbnail>,
        val donationLinks : List<String>,
        val tags : List<String>,
        /** Probably RSS? */
        val feedUrl : String?,
        val tabs : List<PlaylistInfo>
    ) : Info()
}

@Serializable
sealed class Stream { // Remaining: id,manifestUrl,mediaFormat,deliveryMethod
    abstract val content : String?
    @Serializable data class Video(
        override val content : String? ,
        val width : Int? ,
        val height : Int? ,
        val fps : Int? ,
        val resolution : String? ,
        val bitrate : Int?
    ) : Stream()
    @Serializable data class Audio(
        override val content : String? ,
        @Contextual val locale : String? ,
        val trackType: AudioTrackType? ,
        val trackName : String?
    ) : Stream()
    @Serializable data class Subtitles(
        override val content : String? ,
        @Contextual val locale : String? ,
        val autoGenerated : Boolean?
    ) : Stream()
}

enum class StreamType {
    AUDIO,VIDEO,AUDIO_LIVE,VIDEO_LIVE,AUDIO_POST_LIVE,VIDEO_POST_LIVE
}

enum class AudioTrackType {
    ORIGINAL , DUBBED , DESCRIPTIVE , SECONDARY
}

enum class PlaylistType {
    NORMAL,MIX_VIDEO,MIX_MUSIC,MIX_MUSIC_GENRE,
    /** A playlist that may contain streams, playlists, channels and all types of summaries */
    SUPER
}
@Serializable
data class Thumbnail(val url : String , val width : Int? , val height : Int?)

@Serializable
data class Related(val url : String, val relation : Relation)

@Serializable
enum class Relation {
    TRAILER , CONTENT
}

@Serializable
enum class ContentCategory {
    PODCAST,MUSIC,MOVIE,SERIES,ANIME,IPTV,CABLE_CHANNEL,RADIO
}

@Serializable
enum class ContentType {
    CHANNEL,PLAYLIST,VIDEO,AUDIO,LIVE_VIDEO,LIVE_AUDIO,SHORTS
}

@Serializable
sealed interface FormattedText {
    abstract val content : String
    @Serializable @SerialName("html")     data class HTML(override val content : String) : FormattedText
    @Serializable @SerialName("markdown") data class Markdown(override val content : String) : FormattedText
    @Serializable @SerialName("plain")    data class Plain(override val content: String) : FormattedText
}

val FormattedText.typePresenetableName get() =
    when(this) {
        is FormattedText.HTML -> "HTML"
        is FormattedText.Markdown -> "Markdown"
        is FormattedText.Plain -> "Plain Text"
    }
/**
 * Data about a chapter in a YouTube video or anything like it.
 */
@Serializable
data class Chapter(val title : String , val startTimeSeconds : Int , val channelName : String? , val previewUrl : String?)

/**
 * Represents preview frames that should be displayed by video
 * player when hovering/seeking progressbar.
 *
 * Explanantion:
 * Preview frames get transferred as pages, each page is a
 * grid of a large number thumbnails/frames. Each frame represents
 * a fixed duration in the video.
 *
 */
@Serializable
data class PreviewFrames(
    val pagesUrls : List<String> ,
    val frameWidth : Int ,
    val frameHeight : Int ,
    val framesCount : Int ,
    val durationPerFrame : Int ,
    val framesPerPageX : Int ,
    val framesPerPageY : Int
)

@Serializable data class Progress(val progress : Long , val total : Long)

