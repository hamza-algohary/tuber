package capabilities

import java.lang.Exception

val DEBUG = System.getenv("DEBUG")=="true"
fun <T , O> List<T>.attemptUntilOneSucceeds(func : (T)->O) : O? {
    forEach {
        runCatching {
            return try {
                func(it)
            } catch (e : Exception) {
                if(DEBUG)
                    println(e.stackTraceToString())
                throw e
            }
        }
    }
    return null
}