package capabilities

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.use

fun sqliteConnection(dbPath : String , func : Connection.()->Unit = {}): Connection? =
    DriverManager.getConnection("jdbc:sqlite:$dbPath")?.apply(func)

fun Connection.useQuery(query : String, func : (ResultSet)->Unit) =
    prepareStatement(query).use {
        it.executeQuery().use(func)
    }

fun Connection.countQuery(query: String) : Long =
    prepareStatement("SELECT COUNT(*) FROM (${query.trimEnd(';')}) t;").use {
        it.executeQuery().use { resultSet ->
            if (resultSet.next()) resultSet.getLong(1) else 0L
        }
    }

fun ResultSet.asSequence(): Sequence<ResultSet> = sequence {
    while (next()) {
        yield(this@asSequence)
    }
}
