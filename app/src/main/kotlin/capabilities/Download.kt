package capabilities

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.net.URL
import java.util.Scanner


private val httpClient = OkHttpClient()


/** Get text from an online resource */
fun resolveUrlToString (url : String) =
    Scanner(URI.create(url).toURL().openStream(), "UTF-8").useDelimiter("\\A").next()
fun resolveUrlToStream(url : String) =
    URI.create(url).toURL().openStream()

//fun resolveUrlToString(url: String) : String = responseFromUrl(url).body.string()
//fun resolveUrlToStream(url : String) : InputStream = responseFromUrl(url).body.byteStream()

//private fun responseFromUrl(url: String) : Response {
//    return httpClient.newCall(requestFromUrl(url)).execute().use { response ->
//        if (!response.isSuccessful)
//            throw IOException("HTTP ${response.code} when fetching $url")
//        response
//    }
//}
//
//private fun requestFromUrl(url: String) : Request = Request.Builder().url(url).build()
