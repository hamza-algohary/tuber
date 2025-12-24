package services

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
        is Summary.PlaylistSummary -> copy(streamCount = null)
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
    when(this) {
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
