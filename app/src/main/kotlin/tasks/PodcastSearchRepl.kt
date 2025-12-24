package tasks

import backend.toSummary

object PodcastSearchRepl {
    @JvmStatic
    fun main(args: Array<String>) {
        while (true) {
            print("query > ")
            val query = readLine() ?: continue
            if(query.trim() == "q")
                break
            PodcastIndex.search(query)
                .items
                .mapNotNull { it.document.toSummary() }
                .forEach {
                    println("=====================================\n${it.name}\n\t${it.description?.content?:""}\n\n")
                }
        }
    }
}

fun String.take(maxLength : Int) =
    substring(0, minOf(maxLength, length))