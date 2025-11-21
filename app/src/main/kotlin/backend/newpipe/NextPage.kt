package services.newpipe

import services.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import org.schabi.newpipe.extractor.search.SearchInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonClassDiscriminator
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo

//@Serializable data class PageToken()

//@OptIn(ExperimentalSerializationApi::class)
//@JsonClassDiscriminator("type")
@Serializable sealed class NextPage {
    abstract val service : String
    abstract val page : org.schabi.newpipe.extractor.Page?
    @Serializable data class SearchNextPage(override val service : String, val query : String, @Contextual override val page : org.schabi.newpipe.extractor.Page?) : NextPage()
    @Serializable data class PlaylistNextPage(override val service : String , val url : String ,  @Contextual override val page : org.schabi.newpipe.extractor.Page?)  : NextPage()
    @Serializable data class TabNextPage(override val service : String , @Contextual override val page : org.schabi.newpipe.extractor.Page? , val originalUrl : String , val url : String , val id : String , val contentFilters : List<String> , val sortFilter: String) : NextPage()

    fun toJson() = json.encodeToString<NextPage>(this)
}

fun <T : InfoItem> InfoItemsPage<T>.toSearchResultsItems(service: String , query : String) =
    Items(items.toSummaries() , NextPage.SearchNextPage(service , query , nextPage).toJson())
fun <T : InfoItem> InfoItemsPage<T>.toPlaylistItems(service : String , url : String) =
    Items(items.toSummaries() , NextPage.PlaylistNextPage(service , url , nextPage).toJson())
fun <T : InfoItem> InfoItemsPage<T>.toTabItems(service : String , originalUrl : String , url : String , id : String , contentFilters : List<String> , sortFilter : String) =
    Items(items.toSummaries() , NextPage.TabNextPage(service , nextPage , originalUrl , url , id , contentFilters , sortFilter).toJson())

val NextPage.items get() =
    page?.let { page ->
        serviceFromName(service)?.let { service ->
            when (this) {
                is NextPage.SearchNextPage ->
                    SearchInfo.getMoreItems( service , service.searchQHFactory.fromQuery(query) , page).toSearchResultsItems(service.name , query)
                is NextPage.PlaylistNextPage ->
                    PlaylistInfo.getMoreItems(service , url , page).toPlaylistItems(service.name , url)
                is NextPage.TabNextPage ->
                    ChannelTabInfo.getMoreItems(service , ListLinkHandler(originalUrl , url , id , contentFilters , sortFilter) , page).toTabItems(service.name , originalUrl , url , id , contentFilters , sortFilter)
            }
        }?:throw UnknownServiceName(service)
    }?:throw PageIsNull()
