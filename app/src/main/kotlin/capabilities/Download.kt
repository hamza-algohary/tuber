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
