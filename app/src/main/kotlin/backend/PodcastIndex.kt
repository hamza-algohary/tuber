package backend

import services.SearchProvider
import services.SearchResult
import tasks.PodcastIndex

//val podcastIndexBackend = Backend(
//
//)
val backendName = "podcastindex"
//object podcastIndexSearchProvider : SearchProvider {
//    override val name = backendName
//    override fun search(query: String, filters: List<String>, sortBy: String): SearchResult {
//        PodcastIndex.search()
//    }
//
//    override fun filters(): List<String> {
//        TODO("Not yet implemented")
//    }
//
//    override fun sortOptions(): List<String> {
//        TODO("Not yet implemented")
//    }
//
//}