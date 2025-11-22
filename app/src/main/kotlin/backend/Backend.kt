package services

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Backend(
    val infoProviders : List<InfoProvider> ,
    val moreItemsProvider: List<MoreItemsProvider> ,
    val searchProviders : List<SearchProvider>
)

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
    fun search(query : String) : SearchResult
}

