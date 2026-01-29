

object Config {
    const val APP_NAME = "tuber"
    const val APP_VERSION = "0.5"
    val DATA_PATH by lazy { "${get_XDG_HOME()}/$APP_NAME" }
    val LUCENE_INDEX_PATH = "$DATA_PATH/index"
    val MEDIA_LISTS_PATH = "$LUCENE_INDEX_PATH/lists"
    val PODCASTINDEX_INDEX_PATH = "$LUCENE_INDEX_PATH/podcastindex"
}

/** Will throw [UndefinedEnvironmentVariables] if neither of XDG_DATA_HOME or HOME exist */
fun get_XDG_HOME() =
    System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotEmpty() } ?:
    System.getenv("HOME")?.takeIf { it.isNotEmpty() } ?.let { "$it/.local/share" } ?:
    throw UndefinedEnvironmentVariables("XDG_DATA_HOME","HOME")


/** returns null if environment [variable] is empty */
//fun env(variable : String) = System.getenv(variable)?.takeIf { it.isNotEmpty() }
class UndefinedEnvironmentVariables(vararg anyOf : String) : Exception("Non of the following environment variables is defined: ${anyOf.joinToString(",")}")
