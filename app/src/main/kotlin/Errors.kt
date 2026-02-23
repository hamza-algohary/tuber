import kotlin.system.exitProcess

sealed class Error(val exitCode : Int, override val message: String = "") : Exception(message)

class UnableToHandleLinkException(link : String)                    : Error(2 , "Unable to handle link ${link}" )
class InvalidTokenException                                         : Error(3 , "Invalid token")
class CanNotCreateDirectory(path : String)                          : Error(4 , "Unable to create directory: $path (maybe permission issue)" )
class UndefinedEnvironmentVariables(vararg anyOf : String)          : Error(5 , "Non of the following environment variables is defined: ${anyOf.joinToString(",")}")
class UnidentifiableService(message : String)                       : Error(6 , "Unidentifiable Service: $message ")
class UnknownServiceName(name : String)                             : Error(7 , "Unknown service name: $name")
class PageIsNull                                                    : Error(8 , "Page is null")
class InvalidPageToken                                              : Error(9 , "Invalid page token")
class CommandNotFoundOrInsufficientPermissions(command : String)   : Error(10 , "Command `$command` not found or insufficient permissions")

val UNKNOWN_ERROR_EXIT_CODE = 1
fun handleCLIExceptions(func : ()->Unit) =
    try {
        func()
    } catch(e : Exception) {
        when (e) {
            is Error -> error(e.message , e.exitCode)
            else -> if (BuildInfo.isDebug) {
                throw e
            } else {
                error(e.message?:"",UNKNOWN_ERROR_EXIT_CODE)
            }
        }
    }

private fun error(message: String = "" , exitCode : Int = 1) {
    System.err.println(message)
    exitProcess(exitCode)
}

