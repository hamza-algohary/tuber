package capabilities

import kotlinx.serialization.json.Json
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.KnnFloatVectorField
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.index.VectorSimilarityFunction
import org.apache.lucene.search.FuzzyQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.TermQuery
import org.apache.lucene.util.BytesRef
import InvalidPageToken
import plugins.emptyChannelSummary
import plugins.eraseDynamicFields
import plugins.toPlainText
import plugins.type
import java.io.File


fun fuzzyPhraseQueries(fieldName : String , fieldValue : String , weight : Double = 1.0) =
    fieldValue.trim().split(" ").map { value ->
        FuzzyQuery(Term(fieldName,value)) weight weight
    }.toTypedArray()

/** Tokenized text. Use it for fuzzy matching and stuff.*/
fun text(name : String , value : String? , store : Store = Store.NO) = TextField(name,value,store)
/** Untokenized text. Use it for exact whole matching/filtering */
fun string(name : String , value : String? , store: Store = Store.NO) = StringField(name,value,store)
fun vector(name : String , value : FloatArray? , similarityFunction: VectorSimilarityFunction = VectorSimilarityFunction.COSINE) =
    KnnFloatVectorField(name,value,similarityFunction)
fun filter(name : String , value : String , store : Store = Store.NO) =
    arrayOf(
        StringField(name , value , store) ,
        SortedDocValuesField(name, BytesRef(value))
    )
fun plugins.Summary.vectors() = arrayOf(
    vector("name-embedding" , transformSentence(name?:"")),
    vector("description-embedding" , transformSentence(description.toPlainText())),
)

fun plugins.Summary.toDocument(vararg additionalFields : Field, useVectorEmbeddings : Boolean) =
    document(
        text("name", name),
        text("description", description.toPlainText()),
        *if (useVectorEmbeddings) vectors() else emptyArray(),
        string("type", type),
        string("url", url), // For sorting/pagination
        *filter("service", service ?: ""),
        StoredField("json", Json.encodeToString(this)),
        *categories.map { string("category", it.name) }.toTypedArray(),
        *additionalFields
    )

fun Document.toSummary() : plugins.Summary? =
    get("json")?.let(Json::decodeFromString)


class Lists(val indexDirectory : String ,val pageSize : Int = 50 ,val useVectorEmbeddings: Boolean=true) : AutoCloseable {
    private val indexes = HashMap<String, DocumentsIndex>()
    private fun indexPath(listName : String) = "$indexDirectory/$listName"
    private fun index(listName : String) =
        indexes.getOrPut(listName) {
            DocumentsIndex(indexPath(listName), pageSize = pageSize, uniqueFieldName = "url")
        }

    interface Commit {
        fun addToList(summary: plugins.Summary) : Any
        fun removeFromList(url : String) : Any
    }

    fun <T> commit(listName : String , operations : Commit.()->T ) =
        index(listName).commit {
            object : Commit {
                override fun addToList(summary : plugins.Summary) =
                    add(summary.eraseDynamicFields().toDocument(useVectorEmbeddings = useVectorEmbeddings))
                override fun removeFromList(url : String) =
                    remove(url)
            }.operations()
        }

    private val fuzzyQueryWeights = if(useVectorEmbeddings) 1.0 else 0.1
    private fun makeQuery(listName : String , query : String , useVectorEmbeddings: Boolean = this.useVectorEmbeddings): Query =
        combineQueries(
            boostWith = listOf(
                *fuzzyPhraseQueries("name", query, fuzzyQueryWeights),
                *fuzzyPhraseQueries("description", query, fuzzyQueryWeights),
                *if (useVectorEmbeddings)
                    arrayOf(
                        transformSentence(query)?.let {
                            vectorNearestNeighbourQuery(
                                "name-embedding",
                                it,
                                pageSize, /*TermQuery(Term("list", listName))*/
                            )
                        },
                        transformSentence(query)?.let {
                            vectorNearestNeighbourQuery(
                                "description-embedding",
                                it,
                                pageSize, /*TermQuery(Term("list", listName))*/
                            )
                        },
                    )
                else emptyArray()
            ),
        )

    /** Use empty [listName] to search in all lists */
    fun search(listName : String , query : String , useVectorEmbeddings: Boolean = this.useVectorEmbeddings) : plugins.Items =
        index(listName).search(makeQuery(listName,query,useVectorEmbeddings)).also {
            it.nextPage?.metaData?.putAll(
                mapOf(
                    "listName" to listName,
                    "query" to query
                )
            )
        }.toItems()

    fun lists() = subdirs(indexDirectory)
    fun services(listName : String) = index(listName).getAllValuesOf("service")
    fun channels(listName : String) =
        index(listName).search(
            TermQuery(Term("type", emptyChannelSummary.type)) ,
            pageSize = Int.MAX_VALUE
        ).items.toSummaries()

    fun commit(listName: String) = index(listName).commit()
//
//    fun <T> commit(listName: String , func : (Lists)->T) : T =
//        func(this).also {
//            commit(listName)
//        }
    fun overwrite(listName : String , func : (Commit)->Unit) {
        commit (listName) {
            File(indexDirectory).deleteRecursively()
            func(this)
        }
    }
    fun getPage(page: Page) =
        try {
            index(page.metaData["listName"]!!)
                .getPage(page , makeQuery(page.metaData["listName"]!!,page.metaData["query"]!!))
        } catch (e : Exception) {
            throw when {
                e is NullPointerException -> InvalidPageToken()
                else -> e
            }
        }
    fun getAll(listName: String) = index(listName).getAll().toSummaries()
    fun deleteList(listName: String) = deleteDir(indexPath(listName))

    override fun close() {
        indexes.forEach { (listName , index) ->
            commit(listName)
            index.close()
        }
    }

    fun reindex(listName : String) {
        index(listName).reindex { doc ->
            doc.toSummary()!!
               .toDocument(useVectorEmbeddings = useVectorEmbeddings)
        }
    }
}

private fun subdirs(path: String): List<String> =
    runCatching {
        File(path)
            .listFiles { it.isDirectory }
            ?. map { it.name }
            ?: emptyList()
    }.getOrElse { emptyList() }

private fun deleteDir(path: String) =
    runCatching { java.io.File(path).deleteRecursively() }.getOrDefault(false)

fun List<ScoredDocument>.toSummaries() = mapNotNull { it.document.toSummary() }
fun Sequence<ScoredDocument>.toSummaries() = mapNotNull { it.document.toSummary() }

fun LuceneSearchResults.toItems() : plugins.Items =
    _root_ide_package_.plugins.Items(
        items = items.toSummaries(),
        emptyList(),
        nextPageToken = Json.encodeToString(nextPage) //nextPage?.toBinaryString()
    )
