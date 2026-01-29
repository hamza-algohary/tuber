package backend

import com.ibm.icu.text.Normalizer2
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.icu.ICUFoldingFilter
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.index.DirectoryReader
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.index.LeafReader
import org.apache.lucene.index.Term
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.DisjunctionMaxQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.KnnFloatVectorQuery
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.Sort
import org.apache.lucene.store.FSDirectory
import kotlin.io.path.Path
import kotlinx.serialization.Serializable
import org.apache.lucene.search.FieldDoc
import java.io.File
import kotlin.apply


/** Credits: ChatGPT */
class MultiLangAnalyzer : Analyzer() {
    override fun createComponents(fieldName: String?) =
        ICUTokenizer().let { tokenizer ->
            TokenStreamComponents(
                tokenizer,
                ICUNormalizer2Filter(
                    LowerCaseFilter(ICUFoldingFilter(tokenizer)),
                    Normalizer2.getNFKCInstance()
                )
            )
        }
}

class CanNotCreateDirectory(path : String) : Exception("Unable to create directory: $path (maybe permission issue)")
class DocumentsIndex(val path : String, val uniqueFieldName : String,  val pageSize : Int = 20, val analyzer : Analyzer = MultiLangAnalyzer()) {
    init {
        File(path).apply {
            if(!exists()) if (!mkdirs()) throw CanNotCreateDirectory(path)
        }
    }
    val directory = FSDirectory.open(Path(path))
    /** The point of making this lazy, is preventing writer from accidentally opening and committing without actually even being referenced */
    val writer by lazy { IndexWriter(directory, IndexWriterConfig(analyzer))  }
    val reader by lazy { DirectoryReader.open(directory) }
    val leafReader : LeafReader by lazy { DirectoryReader.open(directory).leaves().get(0).reader() }
    val searcher by lazy { IndexSearcher(reader) }

    val Document.uniqueField get() = get(uniqueFieldName)
    val ScoreDoc.document get() = searcher.storedFields().document(doc)
    fun ScoreDoc.toResult() = ScoredDocument(document,score)
//    fun Array<ScoreDoc>.toLuceneSearchResults(/*query: Query , sortBy: Sort = Sort(), */pageSize: Int = this@DocumentsIndex.pageSize ) =
//        LuceneSearchResults( map{ it.toResult() } ,if (isEmpty()) null else Page(last(), pageSize))

    val DEFAULT_SORT = Sort()

//    fun getScoreDoc
}

fun DocumentsIndex.update(uniqueField : String, document : Document) =
    writer.updateDocument(Term(uniqueFieldName , uniqueField) , document)

fun DocumentsIndex.remove(uniqueField : String) =
    writer.deleteDocuments(Term(uniqueFieldName , uniqueField))

fun DocumentsIndex.query(query: Query, sortBy : Sort = DEFAULT_SORT, pageSize: Int = this.pageSize , pageNumber : Int = 0) =
    searcher.search(query , pageSize * (pageNumber+1) , sortBy)
        .scoreDocs.drop(pageSize * pageNumber)
                  .takeLast(pageSize)
                  .map { it.toResult() }

fun DocumentsIndex.search(query: Query, sortBy : Sort = DEFAULT_SORT , pageSize: Int = this.pageSize , pageNumber : Int = 0) =
    query(query,sortBy,pageSize,pageNumber).let{ results ->
        LuceneSearchResults(results , Page(pageNumber+1).takeIf { results.size >= pageSize })
    }
//    searcher.search(query , pageSize , sortBy)
//        .scoreDocs.toLuceneSearchResults(pageSize)

fun DocumentsIndex.reindex(transformer : (old : Document)->Document) =
    getAll().forEach {
        update(it.document.uniqueField , transformer(it.document))
    }

fun DocumentsIndex.add(documents : List<Document>) =
    writer.addDocuments(documents)

fun DocumentsIndex.add(document : Document) =
    writer.addDocument(document)

fun DocumentsIndex.getAll() = query(MatchAllDocsQuery() , pageSize = Int.MAX_VALUE)
/**
 * According to the gentleman ChatGPT;
 * If the field is not tokenized (e.g. StringField) and
 * you want to iterate / collect all its values across
 * documents, you must index it with DocValues.
 *
 * Example:
 * ```
 * doc.add(new StringField("category", value, Field.Store.NO));
 * doc.add(new SortedDocValuesField("category", new BytesRef(value)));
 * ```
 */
fun DocumentsIndex.getAllValuesOf(fieldName : String) =
    leafReader.getSortedDocValues(fieldName)?.let { docValues ->
        List(docValues.valueCount) { index ->
            docValues.lookupOrd(index).utf8ToString()
        }.filterNotNull()
    }?:emptyList()

fun DocumentsIndex.getPage(page: Page , query: Query , sortBy: Sort = DEFAULT_SORT) = with(page) {
//    searcher.searchAfter(after , query , pageSize , sortBy).scoreDocs.toResults(pageSize)
    search(query , sortBy , pageSize , pageNumber).also { it.nextPage?.metaData = page.metaData }
}

data class ScoredDocument(val document: Document, val score : Float)
data class LuceneSearchResults(val items : List<ScoredDocument>, val nextPage : Page?)
@Serializable data class Page(val pageNumber : Int, var metaData : MutableMap<String,String> = HashMap())

//@Serializable data class Page(
//    val doc: Int,
//    val score: Float,
//    val shardIndex: Int,
//    val pageSize : Int,
//    val metaData : MutableMap<String,String> = HashMap()
//) : java.io.Serializable {
//    constructor(scoreDoc: ScoreDoc, pageSize: Int, metaData: MutableMap<String,String> = HashMap()) :
//            this(scoreDoc.doc , scoreDoc.score , scoreDoc.shardIndex , pageSize , metaData)
//}

/**
 * Using [ScoreDoc] here instead of [FieldDoc] will throw an exception from Lucene's side,
 * One problem here is that FieldDoc has Object[] field which is completely unserializable,
 * however I am using FieldDoc anyway where the Object[] will be lost in serialization,
 * let's hope it doesn't explode.
 * Note: To know what I am talking about take a look at [IndexSearcher.searchAfter] line 705
 */
//val Page.after get() = FieldDoc(doc,score,emptyArray(),shardIndex)

//@Serializable data class SerializableScoreDoc(val doc: Int, val score: Float, val shardIndex: Int) {
//    constructor(scoreDoc: ScoreDoc) : this(scoreDoc.doc , scoreDoc.score , scoreDoc.shardIndex)
//    fun get() = ScoreDoc(doc,score,shardIndex)
//}
//fun ScoreDoc.serializable() = SerializableScoreDoc(this)

//
//private val json = Json {
//    serializersModule  = SerializersModule {
//        contextual(Page::class , )
//    }
//}


/**
 * Multiplies a query's score by [weight]
 */
infix fun Query.weight(weight : Double) =
    BoostQuery(this , weight.toFloat())

/**
 * Combine multiple queries
 */
fun combine(
    must : List<Query> = emptyList(),
    boostWith : List<Query> = emptyList(),
    except : List<Query> = emptyList(),
    scorelessFilters : List<Query> = emptyList()
) =
    BooleanQuery.Builder().add(
        must.map { BooleanClause(it , BooleanClause.Occur.MUST) } +
                boostWith.map { BooleanClause(it , BooleanClause.Occur.SHOULD) } +
                except.map { BooleanClause(it , BooleanClause.Occur.MUST_NOT) } +
                scorelessFilters.map { BooleanClause(it , BooleanClause.Occur.FILTER) }
    ).build()

/**
 * Quoting ChatGPT:
 * #### Fuzziness with QueryParser
 * You can even support fuzziness in the query string itself:
 * ```
 * String fuzzyQuery = "funny~ cat~ video~";
 * Query query = parser.parse(fuzzyQuery);
 * ```
 * `~` tells Lucene to do fuzzy matching on that term.
 * The analyzer ensures it’s tokenized correctly before applying fuzziness.
 */
fun parseQuery(field : String , query : String , analyzer: Analyzer = MultiLangAnalyzer()) : Query =
    QueryParser(field , analyzer).parse(query)

fun vectorNearestNeighbourQuery(field : String , vector : FloatArray , numberOfDocuments : Int , prefilter : Query? = null) : Query =
    KnnFloatVectorQuery(field , vector , numberOfDocuments , prefilter)

/**
 * If a document is matched by multiple queries,
 * its total score = highest match score + [lowerScoresFactor] * sum of remaining matches scores
 */
fun anyOf(vararg queries : Query?, lowerScoresFactor : Double = 0.1) =
    DisjunctionMaxQuery(queries.toList().filterNotNull() , lowerScoresFactor.toFloat())

fun fuzzyPhraseQuery(field : String , string : String) =
    parseQuery(field , string.replace(" " , "~ "))

fun document(vararg fields : Field?) =
    Document().apply {
        fields.filterNotNull().forEach(::add)
    }
