import kotlin.system.exitProcess


fun handleCLIExceptions(func : ()->Unit) =
    try {
        func()
    } catch(e : Exception) {
        when (e) {
            is Error -> error(e.message)
            else -> if (BuildInfo.isDebug) {
                throw e
            } else {
                error(e.message?:"")
            }
        }
    }

private fun error(message: String = "" , exitCode : Int = 1) {
    System.err.println(message)
    exitProcess(exitCode)
}

sealed class Error(val exitCode : Int, override val message: String = "") : Exception(message)

class UnableToHandleLinkException(link : String)                    : Error(1 , "Unable to handle link ${link}" )
class InvalidTokenException                                         : Error(2 , "Invalid token")
class CanNotCreateDirectory(path : String)                          : Error(3 , "Unable to create directory: $path (maybe permission issue)" )
class UndefinedEnvironmentVariables(vararg anyOf : String)          : Error(4 , "Non of the following environment variables is defined: ${anyOf.joinToString(",")}")
class UnidentifiableService(message : String)                       : Error(5 , "Unidentifiable Service: $message ")
class UnknownServiceName(name : String)                             : Error(6 , "Unknown service name: $name")
class PageIsNull                                                    : Error(7 , "Page is null")
class InvalidPageToken                                              : Error(8 , "Invalid page token")
class CommandNotFoundOrInsuffiecientPermissions(command : String)   : Error(9 , "Command `$command` not found or insufficient permissions")