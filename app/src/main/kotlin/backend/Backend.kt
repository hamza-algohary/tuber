package services

import org.jsoup.Jsoup

class Backend(
    val infoProviders : List<InfoProvider> ,
    val moreItemsProvider: List<MoreItemsProvider> ,
    val searchProviders : List<SearchProvider> ,
    val catalogProviders: List<CatalogProvider> ,
    val categories : List<String> = emptyList(),
    init : ()->Unit = {} ,
    condition : ()->Boolean = {true} ,
) {
    init {
        if (condition())
            init()
    }
}

interface InfoProvider {
    val name : String
    fun stream(url : String) : Info.StreamInfo
    fun playlist(url : String) : Info.PlaylistInfo
    fun channel(url : String) : Info.ChannelInfo
}

interface MoreItemsProvider {
    val name : String
    fun moreItems(pageToken: String) : Items?
}

interface SearchProvider {
    val name : String
    fun search(query : String , filters : List<String> = emptyList() , sortBy : String = "") : SearchResult
    fun filters() : List<String>
    fun sortOptions() : List<String>
//    fun kiosks() : List<String>
//    fun kiosk(name : String) : Items
}

interface CatalogProvider {
    val name : String
    fun catalog() : List<Info.PlaylistInfo>
}

/**
 * Turns dynamic fields (like views, subscribers, etc.)
 * into null. Useful when caching/indexing, as you don't
 * want your cache to contain outdated data.
 */
fun Summary.eraseDynamicFields() =
    when (this) {
        is Summary.ChannelSummary -> copy(subscriberCount = null , streamCount = null)
        is Summary.PlaylistSummary -> copy(numberOfItems = null)
        is Summary.StreamSummary -> copy(views = null)
    }


fun FormattedText?.toPlainText() =
    when(this) {
        is FormattedText.HTML -> Jsoup.parse(content).text()
        is FormattedText.Markdown ->
            org.commonmark.parser.Parser.Builder().build().parse(content).let {
                org.commonmark.renderer.text.TextContentRenderer.builder().build().render(it)
            }
        is FormattedText.Plain -> content
        null -> ""
    }

val Summary.type : String get() =
    when (this) {
        is Summary.ChannelSummary -> "channel"
        is Summary.PlaylistSummary -> "playlist"
        is Summary.StreamSummary -> "stream"
    }

val emptyChannelSummary : Summary.ChannelSummary =
    Summary.ChannelSummary(null,null,emptyList(),null,emptyList(),emptyList(),null,null,null,null)
val emptyPlaylistSummary : Summary.PlaylistSummary =
    Summary.PlaylistSummary(null,null,emptyList(),null,emptyList(),emptyList(),null,null,null,null)
val emptyStreamSummary : Summary.StreamSummary =
    Summary.StreamSummary(null,null,emptyList(),null,emptyList(),emptyList(),null,null,null,null,null,null)

fun Info.StreamInfo.toStreamSummary() =
    Summary.StreamSummary(
        name,url,thumbnails,service,categories,related,type,
        duration,viewCount,uploadTimeStamp,description,uploader
    )

fun Info.PlaylistInfo.toPlaylistSummary() =
    Summary.PlaylistSummary (
        name,url,thumbnails,service,categories,related,uploader,numberOfItems,description,playlistType
    )

fun Info.ChannelInfo.toChannelSummary() =
    Summary.ChannelSummary (
        name,url, avatars,service,categories,related,verified,description,subscriberCount, streamCount
    )

fun Info.toSummary() : Summary =
    when (this) {
        is Info.StreamInfo -> toStreamSummary()
        is Info.PlaylistInfo -> toPlaylistSummary()
        is Info.ChannelInfo -> toChannelSummary()
    }

fun Info.PlaylistInfo.iter(moreItemsProvider: MoreItemsProvider) =
    sequence {
        var page: Items? = this@iter.items
        while (page != null) {
            yieldAll(page.items)
            page = page.nextPageToken?.let(moreItemsProvider::moreItems)
        }
    }


