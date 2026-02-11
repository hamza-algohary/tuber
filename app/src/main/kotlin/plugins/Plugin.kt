package plugins

import UnknownServiceName
import capabilities.Try
import capabilities.attemptUntilOneSucceeds
import org.commonmark.parser.Parser
import org.commonmark.renderer.text.TextContentRenderer
import org.jsoup.Jsoup

class Plugin(
    infoProviders : List<InfoProvider> = emptyList(),
    moreItemsProvider: List<MoreItemsProvider> = emptyList(),
    searchProviders : List<SearchProvider> = emptyList(),
    catalogProviders: List<CatalogProvider> = emptyList(),
    categories : List<String> = emptyList(),
    val init : ()->Unit = {} ,
    val condition : ()->Boolean = {true} ,
) {
    //    init {
    //        if (condition())
    //            init()
    //    }
    private var initialized = false
    val infoProviders : List<InfoProvider> = infoProviders
        get() = checkInitialized(field)
    val moreItemsProvider: List<MoreItemsProvider> = moreItemsProvider
        get() = checkInitialized(field)
    val searchProviders : List<SearchProvider> = searchProviders
        get() = checkInitialized(field)
    val catalogProviders: List<CatalogProvider> = catalogProviders
        get() = checkInitialized(field)
    val categories : List<String> = categories
        get() = checkInitialized(field)

    fun <T> checkInitialized(value : T) =
        value.also {
            if (!initialized) init()
            initialized = true
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
        is Summary.GenericSummary -> this
    }


fun FormattedText?.toPlainText() =
    when(this) {
        is FormattedText.HTML -> Jsoup.parse(content).text()
        is FormattedText.Markdown ->
            Parser.Builder().build().parse(content).let {
                TextContentRenderer.builder().build().render(it)
            }
        is FormattedText.Plain -> content
        null -> ""
    }

val Summary.type : String get() =
    when (this) {
        is Summary.ChannelSummary -> "channel"
        is Summary.PlaylistSummary -> "playlist"
        is Summary.StreamSummary -> "stream"
        is Summary.GenericSummary -> "generic"
    }

val emptyChannelSummary : Summary.ChannelSummary =
    Summary.ChannelSummary(null,null,emptyList(),null,emptyList(),emptyList(),null,null,null,null)
val emptyPlaylistSummary : Summary.PlaylistSummary =
    Summary.PlaylistSummary(null,null,emptyList(),null,emptyList(),emptyList(),null,null,null,null)
val emptyStreamSummary : Summary.StreamSummary =
    Summary.StreamSummary(null,null,emptyList(),null,emptyList(),emptyList(),null,null,null,null,null,null)

fun Info.StreamInfo.toStreamSummary() =
    Summary.StreamSummary(
        name,url,thumbnails,service,categories,related,streamType,
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

fun Info.PlaylistInfo.iter(moreItemsProviders : List<MoreItemsProvider>) =
    sequence {
        var page: Items? = this@iter.items
        while (page != null) {
            yieldAll(page.items)
            page = moreItemsProviders.attemptUntilOneSucceeds { provider ->
                provider.moreItems(page!!.nextPageToken!!)
            }
        }
    }

fun Plugin.searchProviderFromName(name : String) = searchProviders.find { it.name == name }?: throw UnknownServiceName(name)
fun Plugin.catalogProviderFromName(name : String) = catalogProviders.find { it.name == name }?: throw UnknownServiceName(name)
fun InfoProvider.infoFromUrl(url : String) : Info? =
    Try { stream(url) } ?: Try { playlist(url) } ?: Try { channel(url) }
fun Plugin.infoFromUrl(url : String) =
    infoProviders.attemptUntilOneSucceeds { provider -> provider.infoFromUrl(url) }
operator fun Plugin.plus(other : Plugin) =
    Plugin(
        infoProviders + other.infoProviders,
        moreItemsProvider + other.moreItemsProvider,
        searchProviders + other.searchProviders ,
        catalogProviders + other.catalogProviders
    )



