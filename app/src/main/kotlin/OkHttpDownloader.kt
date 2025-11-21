import okhttp3.*
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

val GlobalOkHttpClient by lazy {
    OkHttpClient()
}

class OkHttpDownloader private constructor( private val client: OkHttpClient = GlobalOkHttpClient ) : Downloader() {
    companion object {
        operator fun invoke(): OkHttpDownloader {
            val client = OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            return OkHttpDownloader(client)
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())

        // Apply headers
        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                if (value != null) builder.addHeader(key, value)
            }
        }

        when (request.httpMethod()) {
            "GET" -> builder.get()
            "HEAD" -> builder.head()
            "POST" -> {
                val bodyBytes = request.dataToSend() ?: ByteArray(0)
                val body = RequestBody.create(null, bodyBytes)
                builder.post(body)
            }
        }

        val call = client.newCall(builder.build())
        val resp = call.execute()

        val bodyString = resp.body?.string()

        // Check for Recaptcha domain -> throw
        val finalUrl = resp.request.url.toString()
        if (finalUrl.contains("recaptcha", ignoreCase = true)) {
            resp.close()
            throw ReCaptchaException("Recaptcha challenge detected",  finalUrl)
        }

        val headersMap: Map<String, List<String>> = resp.headers.toMultimap()

        val result = Response(
            resp.code,
            resp.message,
            headersMap,
            bodyString,
            finalUrl
        )

        resp.close()
        return result
    }
}
