package tasks

import BuildInfo
import BuildInfo.compareTo
import capabilities.shell
import java.io.File

private object GitHub {
    /** GitHub draft release */
    fun release(assets : List<Asset>, tag : String, releaseNotesMD : String) =
        shell(
            "gh","release","create",tag,"--draft"," --fail-on-no-commits","--notes",releaseNotesMD,
            *assets.map { "${it.path}#${it.displayName}" }.toTypedArray()
        )

    fun latestReleaseVersion() : BuildInfo.Version =
        shell("gh", "release", "view", "--json", "tagName", "-q", ".tagName")?.output?.trim()?.split(".")?.runCatching {
            let { (major,minor,patch) ->
                BuildInfo.Version(major.toInt(), minor.toInt(), patch.toInt())
            }
        } ?.getOrNull()?: BuildInfo.Version(0, 0, 0)


}

private object Git {
    fun allChangesAreCommitted(): Boolean = shell("git", "status", "--porcelain")?.output.isNullOrBlank()
}

private data class Asset(val path : String , val displayName : String)
private fun assets(vararg pathToDisplayName : Pair<String,String>) =
    pathToDisplayName.map { (path,name) -> Asset(path,name) }


object Release {
    private val assets = assets(
        "app/build/distributions/app.zip" to "tuber.zip"
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val missingFiles = assets.takeWhile { !File(it.path).exists() }
        val latestReleaseVersion = GitHub.latestReleaseVersion()
        val releaseNotes = BuildInfo.currentReleaseNotes?.content?:""

        assert(missingFiles.isEmpty()) {
            "The following assets are missing:\n ${missingFiles.joinToString("\n")} "
        }
        assert (latestReleaseVersion > BuildInfo.currentVersion) {
            "Current Version is NOT greater than latest version ($latestReleaseVersion <= ${BuildInfo.currentVersion})"
        }
        assert(releaseNotes.isNotBlank()) {
            "Release notes for ${BuildInfo.currentVersion} not available"
        }

        assert(Git.allChangesAreCommitted()) {
            "There are some uncommited changes, run `git status --porcelain` to see them"
        }

        GitHub.release(assets , BuildInfo.currentVersion.toString() , releaseNotes)
    }
}
