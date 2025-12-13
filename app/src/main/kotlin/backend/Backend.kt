package services

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class Backend(
    val infoProviders : List<InfoProvider> ,
    val moreItemsProvider: List<MoreItemsProvider> ,
    val searchProviders : List<SearchProvider> ,
    val catalogProviders: List<CatalogProvider> ,
    init : ()->Unit = {} ,
) {
    init {
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
