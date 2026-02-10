package capabilities

import java.lang.Exception

val DEBUG = System.getenv("DEBUG")=="true"
fun <T , O> List<T>.attemptUntilOneSucceeds(func : (T)->O) : O? {
    forEach {
        runCatching {
            try {
                func(it)
            } catch (e : Exception) {
                if(DEBUG)
                    println(e.stackTraceToString())
                throw e
            }
        }.let { result -> if(result.isSuccess) return result.getOrNull() }
    }
    return null
}

//fun <T , O> List<T>.attemptUntilOneSucceeds(func : (T)->O) : O? =
//    forEachIndexed { index, it ->
//        println("INDEX = $index")
//        runCatching {
//            func(it)
//        }.onSuccess {
//            return it
//        }
//    }.let {
//        null
//    }