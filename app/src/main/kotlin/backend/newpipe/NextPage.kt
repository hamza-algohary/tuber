package services.newpipe

import capabilities.JsonSerializable
import capabilities.fromBinaryString
import capabilities.kStringSerializer
import capabilities.toBinaryString
import services.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import org.schabi.newpipe.extractor.search.SearchInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfo

//@Serializable data class PageToken()

//@OptIn(ExperimentalSerializationApi::class)
//@JsonClassDiscriminator("type")
@Serializable sealed class NextPage : JsonSerializable {
    abstract val service : String
    abstract val page : org.schabi.newpipe.extractor.Page?
    @Serializable data class SearchNextPage(override val service : String, val query : String, val contentFilters: List<String> , val sortFilter: String , @Contextual override val page : org.schabi.newpipe.extractor.Page?) : NextPage()
    @Serializable data class PlaylistNextPage(override val service : String , val url : String ,  @Contextual override val page : org.schabi.newpipe.extractor.Page?)  : NextPage()
    @Serializable data class TabNextPage(override val service : String , @Contextual override val page : org.schabi.newpipe.extractor.Page? , val originalUrl : String , val url : String , val id : String , val contentFilters : List<String> , val sortFilter: String) : NextPage()
    @Serializable data class KioskNextPage(override val service : String , val url : String , @Contextual override val page : org.schabi.newpipe.extractor.Page?) : NextPage()

    override fun toJson() = json.encodeToString<NextPage>(this)
}

fun <T : InfoItem> InfoItemsPage<T>.toSearchResultsItems(service: String , query : String , contentFilters: List<String> , sortFilter: String) =
    Items(items.toSummaries() , emptyList(), NextPage.SearchNextPage(service , query , contentFilters , sortFilter , nextPage).toJson())
fun <T : InfoItem> InfoItemsPage<T>.toPlaylistItems(service : String , url : String) =
    Items(items.toSummaries() , emptyList(),NextPage.PlaylistNextPage(service , url , nextPage).toJson())
fun <T : InfoItem> InfoItemsPage<T>.toTabItems(service : String , originalUrl : String , url : String , id : String , contentFilters : List<String> , sortFilter : String) =
    Items(items.toSummaries() , emptyList(),NextPage.TabNextPage(service , nextPage , originalUrl , url , id , contentFilters , sortFilter).toJson())
fun <T : InfoItem> InfoItemsPage<T>.toKioskItems(service: String , url : String) =
    Items(items.toSummaries() , emptyList(),NextPage.KioskNextPage(service , url , nextPage).toJson())
val NextPage.items : Items get() =
    page?.let { page ->
        serviceFromName(service)?.let { service ->
            when (this) {
                is NextPage.SearchNextPage ->
                    SearchInfo.getMoreItems( service , service.searchQHFactory.fromQuery(query , contentFilters , sortFilter) , page).toSearchResultsItems(service.name , query , contentFilters , sortFilter)
                is NextPage.PlaylistNextPage ->
                    PlaylistInfo.getMoreItems(service , url , page).toPlaylistItems(service.name , url)
                is NextPage.TabNextPage ->
                    ChannelTabInfo.getMoreItems(service , ListLinkHandler(originalUrl , url , id , contentFilters , sortFilter) , page).toTabItems(service.name , originalUrl , url , id , contentFilters , sortFilter)
                is NextPage.KioskNextPage ->
                    KioskInfo.getMoreItems(service , url , page).toKioskItems(service.name , url)
            }
        }?:throw UnknownServiceName(service)
    }?:throw PageIsNull()

private val json = Json {
    serializersModule = SerializersModule {
        contextual(
            kStringSerializer(
                "Page",
                { it.toBinaryString() },
                { fromBinaryString<org.schabi.newpipe.extractor.Page>(it) }
            )
        )
    }
}

fun NextPage.Companion.fromJson(string : String) = json.decodeFromString<NextPage>(string)