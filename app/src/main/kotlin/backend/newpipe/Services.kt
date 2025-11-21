package services.newpipe

import OkHttpDownloader
import services.*
import services.Info
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.*
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.*

private val YouTube = ServiceList.YouTube.asSearchProvider()
private val PeerTube = ServiceList.PeerTube.asSearchProvider()
private val SoundCloud = ServiceList.SoundCloud.asSearchProvider()
private val BandCamp = ServiceList.Bandcamp.asSearchProvider()
private val MediaCCC = ServiceList.MediaCCC.asSearchProvider()

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
).also {
    init()
}

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
        override fun search(query: String) = this@asSearchProvider.search(query)
    }

fun StreamingService.search(query : String) : SearchResult =
    SearchInfo.getInfo(this , searchQHFactory.fromQuery(query)).toSearchResult()

fun streamFromUrl(url : String) : Info.StreamInfo =
    StreamInfo.getInfo(url).toStreamInfo()

fun playlistFromUrl(url : String) : Info.PlaylistInfo =
    PlaylistInfo.getInfo(url).toPlaylistInfo()

fun channelFromUrl(url : String) : Info.ChannelInfo =
    ChannelInfo.getInfo(url).toChannelInfo()


