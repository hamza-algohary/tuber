package tasks

import BuildInfo
import BuildInfo.compareTo
import capabilities.run
import capabilities.shell
import capabilities.wait
import com.github.ajalt.clikt.core.PrintMessage
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration

/** Equivalent to running `shell()` with `inherit=true` and `printCommand=true` */
private fun shellJob(vararg command : String , workingDir: File? = null , timeOut: Duration = Duration.INFINITE ) =
    shell(*command , workingDir = workingDir , timeOut = timeOut , printOutput = false , printCommand  = false)


private object GitHub {
    /** GitHub draft release */
    fun release(assets : List<Asset>, tag : String, releaseNotesMD : String) =
        ProcessBuilder(
            "gh","release","create",tag,"--draft","--fail-on-no-commits","--notes",releaseNotesMD,
            *assets.map { "${it.path}#${it.displayName}" }.toTypedArray() ,
        ).redirectErrorStream(true).inheritIO().run().wait()

    fun latestReleaseVersion() : BuildInfo.Version =
        shellJob("gh", "release", "view", "--json", "tagName", "-q", ".tagName")?.output?.trim()?.split(".")?.runCatching {
            let { (major,minor,patch) ->
                BuildInfo.Version(major.toInt(), minor.toInt(), patch.toInt())
            }
        } ?.getOrNull()?: BuildInfo.Version(0, 0, 0)


}

/** The two functions here are ChatGPT generated an untested. Good Luck :D */
private object Git {
    fun allChangesAreCommitted(): Boolean = shellJob("git", "status", "--porcelain" , "--untracked-files=no")?.output.isNullOrBlank()
    fun hasUnpushedCommitsOrNoUpstream(): Boolean =
        shellJob("git", "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}")
            ?.let {
                shellJob("git", "rev-list", "@{u}..HEAD")?.output?.isNotBlank()
            }
            ?: true
    fun allCommitArePushed() = !hasUnpushedCommitsOrNoUpstream()
}

private data class Asset(val path : String , val displayName : String)
private fun assets(vararg pathToDisplayName : Pair<String,String>) =
    pathToDisplayName.map { (path,name) -> Asset(path,name) }

private fun red(str: String) = "\u001B[31m$str\u001B[0m"
private fun green(str: String) = "\u001B[92m$str\u001B[0m"

object Release {
    private val assets = assets(
        "build/distributions/app.zip" to "tuber.zip"
    )

    @JvmStatic
    fun main(args: Array<String>) {
//        val missingFiles = assets.filter { !File(it.path).exists() }
        val latestReleaseVersion = GitHub.latestReleaseVersion()
        val releaseNotes = BuildInfo.currentReleaseNotes?.content?:""

        checkAll (
            check(assets.all { File(it.path).exists() } , "All assets in place" ,
                "The following assets are missing:\n ${
                    assets.filter { !File(it.path).exists() }
                          .joinToString("\n") { it.path }
                } "
            ),
            check (
                BuildInfo.currentVersion > latestReleaseVersion,
                "Version is correctly bumped (${BuildInfo.currentVersion} > ${latestReleaseVersion})",
                "Current Version is NOT greater than latest version (${BuildInfo.currentVersion} <= $latestReleaseVersion )"
            ),
            check (releaseNotes.isNotBlank() , "Release notes exist" , "Release notes for ${BuildInfo.currentVersion} not available"),
            check (Git.allChangesAreCommitted() , "All changes are commited", "There are some uncommited changes, run `git status --porcelain` to see them"),
            check (Git.allCommitArePushed() , "All commits are pushed" , "Some commits are not pushed."),
        ) onTrue {
            println("Running gh release")
            GitHub.release(assets , BuildInfo.currentVersion.toString() , releaseNotes)?.apply {
                println("gh release exitCode = $exitCode")
                assert (exitCode == 0)  { error }
            } ?: error("`gh release` command wait timeout")
        } onFalse {
            println(red("Failed to create release. Ensure all conditions above are met then retry."))
        }

    }
}

fun check(condition : Boolean, successMessage : String, errorMessage : String ) =
    condition.apply {
        if (condition) println(green("✓ " + successMessage))
        else           println(red("✘ " + errorMessage))
    }

fun checkAll(vararg conditions: Boolean , message : String = "Some checks failed") = conditions.all { it }
inline infix fun Boolean.onTrue (func : () -> Unit ) = apply { if (this)  func() }
inline infix fun Boolean.onFalse(func : () -> Unit ) = apply { if (!this) func() }
