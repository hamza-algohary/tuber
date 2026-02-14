import kotlin.test.Test

class CLITest {
    @Test fun testCLISearchPodcastIndex() = testMain("search","podcastindex","linux" , testName = "podcastindex_search.json")
    @Test fun testCLISearchYoutube() = testMain("search","youtube","linux" , testName = "youtube_search.json")
    @Test fun testCLISearchSoundCloud() = testMain("search","soundcloud","linux", testName = "soundcloud_search.json")
    @Test fun testCLISearchPeerTube() = testMain("search","peertube","linux", testName = "peertube_search.json")
//        @Test fun testCLISearchMediaCCC() = testMain("search","mediaccc","linux", testName = "mediaccc_search.json")
    @Test fun testCLISearchBandCamp() = testMain("search","bandcamp","linux", testName = "bandcamp_search.json")
    @Test fun testCLIStreamYoutube() = testMain("stream","https://www.youtube.com/watch?v=bB9z2HEldNw" , testName = "youtube_stream.json")
    @Test fun testCLIChannelYoutube() = testMain("channel","https://www.youtube.com/@TheLinuxEXP" , testName = "youtube_channel.json")
    @Test fun testCLIPlaylistYoutube() = testMain("playlist" , "https://www.youtube.com/playlist?list=PLqmbcbI8U55EQLnXs1ehDw5-D94vloCzb" , testName = "youtube_playlist.json")
    @Test fun testCLIM3uUrlHandling() = testMain("playlist","https://raw.githubusercontent.com/Free-TV/IPTV/master/playlist.m3u8" , testName = "m3u_url_handling.json")
    @Test fun testCLIRssUrlHandling() = testMain("playlist","https://runtube.re/feeds/videos.xml?videoChannelId=34652" , testName = "rss_url_handling.json")

    @Test fun testSearchProviders() = testMain("search-providers", testName = "search_providers.json")

    @Test fun testVersion() = testMain("version", testName = "version.json")
}