package services.newpipe

import OkHttpDownloader
import services.*
import services.Info
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.kiosk.KioskExtractor
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.kiosk.KioskList
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.*

private val YouTube = ServiceList.YouTube.asSearchProvider()
private val PeerTube = ServiceList.PeerTube.asSearchProvider()
private val SoundCloud = ServiceList.SoundCloud.asSearchProvider()
private val BandCamp = ServiceList.Bandcamp.asSearchProvider()
private val MediaCCC = ServiceList.MediaCCC.asSearchProvider()

private val YouTubeCatalogProvider = ServiceList.YouTube.toCatalogProvider()
private val SoundCloudCatalogProvider = ServiceList.SoundCloud.toCatalogProvider()


fun init() {
    NewPipe.init(OkHttpDownloader())
}

val newpipeBackend = Backend(
    searchProviders =  listOf(YouTube , PeerTube , SoundCloud , BandCamp , MediaCCC) ,
    infoProviders = listOf(
        object : InfoProvider {
            override val name = "newpipe"
            override fun stream(url: String) = streamFromUrl(url)
            override fun playlist(url: String) = playlistFromUrl(url)
            override fun channel(url: String) = channelFromUrl(url)
        }
    ),
    moreItemsProvider = listOf(
        object : MoreItemsProvider {
            override val name = "newpipe"
            override fun moreItems(pageToken: String) = fromJson<NextPage>(pageToken).items
        }
    ),
    catalogProviders = listOf(
        YouTubeCatalogProvider,
        SoundCloudCatalogProvider
    ),
    init=::init
)

internal fun serviceFromName(name : String) =
    when (name) {
        ServiceList.YouTube.name -> ServiceList.YouTube
        ServiceList.PeerTube.name -> ServiceList.PeerTube
        ServiceList.SoundCloud.name -> ServiceList.SoundCloud
        ServiceList.Bandcamp.name -> ServiceList.Bandcamp
        ServiceList.MediaCCC.name -> ServiceList.MediaCCC
        else -> throw Exception("Service is unregistered: This should never happen!!")
    }
internal fun serviceFromId(id : Int) =
    ServiceList.all().find { it.serviceId == id }?:
        throw UnidentifiableService("Service ID doesn't match any NewPipe registered service. THIS SHOULD NEVER HAPPEN")
val InfoItem.service get() = serviceFromId(serviceId)

val StreamingService.name get() : String =
    when(this) {
        ServiceList.YouTube -> "youtube"
        ServiceList.PeerTube -> "peertube"
        ServiceList.SoundCloud  -> "soundcloud"
        ServiceList.Bandcamp -> "bandcamp"
        ServiceList.MediaCCC -> "mediaccc"
        else -> throw Exception("Service is unregistered: This should never happen!!")
    }

private fun StreamingService.asSearchProvider() : SearchProvider =
    object : SearchProvider {
        override val name = this@asSearchProvider.name
        override fun search(query: String , filters : List<String> , sortBy: String) = this@asSearchProvider.search(query , filters , sortBy)
        override fun filters() = this@asSearchProvider.contentFilters()
        override fun sortOptions() = this@asSearchProvider.sortFilters()
//        override fun kiosks() = this@asSearchProvider.kiosks()
//        override fun kiosk(name: String) = Items(emptyList(),null)
    }

fun StreamingService.search(query : String , filters : List<String> = emptyList() , sortBy : String = "") : SearchResult =
    SearchInfo.getInfo(this , searchQHFactory.fromQuery(query , filters , sortBy)).toSearchResult()

fun streamFromUrl(url : String) : Info.StreamInfo =
    StreamInfo.getInfo(url).toStreamInfo()

fun playlistFromUrl(url : String) : Info.PlaylistInfo =
    PlaylistInfo.getInfo(url).toPlaylistInfo()

fun channelFromUrl(url : String) : Info.ChannelInfo =
    ChannelInfo.getInfo(url).toChannelInfo()

fun StreamingService.contentFilters() : List<String> =
    this.searchQHFactory.availableContentFilter.toList()

fun StreamingService.sortFilters() : List<String> =
    this.searchQHFactory.availableSortFilter.toList()

fun StreamingService.toCatalogProvider(keywords : List<String> = listOf("trending","games","news","sports")) =
    object : CatalogProvider {
        override val name = this@toCatalogProvider.name
        override fun catalog() =
            kiosks() +
            keywords.map { keyword ->
                search(keyword).toPlaylistInfo(keyword , this@toCatalogProvider)
            }
    }


fun StreamingService.kiosks() =
    kioskList.availableKiosks.mapNotNull { kiosk ->
        runCatching {
            KioskInfo.getInfo(
                kioskList.getListLinkHandlerFactoryByType(kiosk).getUrl(kiosk)
            ).toPlaylistInfo()
        }.getOrNull()
    }
//fun StreamingService.kiosks() =
//    this.kioskList.availableKiosks.

//fun StreamingService.kiosk(url: String) : List<String> =
//    KioskInfo.g

//fun StreamingService.kiosks() =
//    this.

fun test() {
    println(ServiceList.YouTube.kioskList.availableKiosks)

    ServiceList.all().forEach { service ->
        service.kioskList.availableKiosks.forEach { kiosk ->
            runCatching {
                println(
                    KioskInfo.getInfo(
                        service.kioskList.getListLinkHandlerFactoryByType(kiosk).getUrl(kiosk)
                    )
                )
            }.getOrElse(::println)
        }
    }

//    println(KioskInfo.getInfo(ServiceList.YouTube.kioskList.defaultKioskExtractor.url).toPlaylistInfo())
}