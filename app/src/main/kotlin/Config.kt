import kotlinx.serialization.Serializable
import plugins.FormattedText


object Config {
    const val APP_NAME = "tuber"
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

object BuildInfo {
    @Serializable
    data class Version(val major: Int, val minor: Int, val patch: Int) {
        override fun toString(): String = "$major.$minor.$patch"
    }
    operator fun Version.compareTo(other: Version): Int = when {
        major != other.major -> major - other.major
        minor != other.minor -> minor - other.minor
        else                 -> patch - other.patch
    }
    val currentVersion = Version(0,1,3)
    val currentReleaseNotes get() = getReleaseNotesForVersion(currentVersion)?.notes

    val isDebug by lazy {
        System.getenv("DEBUG") == "true"
    }
    var isTest = false

    data class ReleaseNotes(val version: Version , val notes : FormattedText)
    val releaseNotesForAllVersions = listOf<ReleaseNotes>(
        ReleaseNotes(Version(0,1,0) , plain("First Release hooray \uD83C\uDF89")),
        ReleaseNotes(Version(0,1,1) , md("""
            Changes:
            - Made the Tuber Developer Manual.
            - Introduced the `SearchProviderInfo` class.
            - Removed `filters` and `sort-options` command, as this data is available inside `SearchProviderInfo`.
        """.trimIndent())),
        ReleaseNotes(Version(0,1,2) , md("""
            Changes:
            - Added a hack to fix the app hanging after retrieving rss url. (Needs a real solution later though)
        """.trimIndent())),
        ReleaseNotes(Version(0,1,3) , md("""
            Changes:
            - Fixed `version` commands. 
        """.trimIndent())),
    )
    fun getReleaseNotesForVersion(version: Version) = releaseNotesForAllVersions.find { it.version == version }

}

private fun html(str : String) = FormattedText.HTML(str)
private fun md(str : String) = FormattedText.HTML(str)
private fun plain(str : String) = FormattedText.HTML(str)
