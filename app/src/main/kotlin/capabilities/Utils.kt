package capabilities

import java.lang.Exception

fun <T , O> List<T>.attemptUntilOneSucceeds(func : (T)->O) : O? {
    forEach {
        runCatching {
            try {
                func(it)
            } catch (e : Exception) {
                if(BuildInfo.isDebug)
                    println(e.stackTraceToString())
                throw e
            }
        }.let { result -> if(result.isSuccess) return result.getOrNull() }
    }
    return null
}

fun <T> Try(func : ()->T) : T? =
    try { func() } catch (e : kotlin.Exception) { null }

