package capabilities


import CanNotCreateDirectory
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
import org.apache.lucene.search.BooleanClause
import org.apache.lucene.search.BooleanQuery
import org.apache.lucene.search.BoostQuery
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.search.KnnFloatVectorQuery
import org.apache.lucene.search.MatchAllDocsQuery
import org.apache.lucene.search.Query
import org.apache.lucene.search.ScoreDoc
import org.apache.lucene.search.Sort
import org.apache.lucene.store.FSDirectory
import kotlin.io.path.Path
import kotlinx.serialization.Serializable
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

class DocumentsIndex(val path : String, val uniqueFieldName : String,  val pageSize : Int = 20, val analyzer : Analyzer = MultiLangAnalyzer()) : AutoCloseable {
    init {
        File(path).apply {
            if(!exists()) if (!mkdirs()) throw CanNotCreateDirectory(path)
        }
    }
    private val directory = FSDirectory.open(Path(path))
    /** The point of making this lazy, is preventing writer from accidentally opening and committing without actually even being referenced */
    private val writer by lazy { IndexWriter(directory, IndexWriterConfig(analyzer))  }
    private val reader by lazy { DirectoryReader.open(directory) }
    private val leafReader : LeafReader by lazy { DirectoryReader.open(directory).leaves().get(0).reader() }
    private val searcher by lazy { IndexSearcher(reader) }

    private val ScoreDoc.document get() = searcher.storedFields().document(doc)
    val Document.uniqueField get() = get(uniqueFieldName)
    fun ScoreDoc.toResult() = ScoredDocument(document,score)

    val DEFAULT_SORT = Sort()

    interface Commit {
        fun add(document : Document) : Long
        fun update(uniqueField : String, document : Document) : Long
        fun remove(uniqueField : String) : Long
    }

    private val commitImpl = object : Commit {
        override fun add(document : Document) =
            update(document.uniqueField,document) // writer.addDocument(document)
        override fun update(uniqueField : String, document : Document) =
            writer.updateDocument(Term(uniqueFieldName , uniqueField) , document)
        override fun remove(uniqueField : String) =
            writer.deleteDocuments(Term(uniqueFieldName , uniqueField))
    }

    fun search(query: Query, sortBy : Sort = DEFAULT_SORT, pageSize: Int = this.pageSize , pageNumber : Int = 0) =
        searcher.search(query , pageSize * (pageNumber+1) , sortBy)
            .scoreDocs.drop(pageSize * pageNumber)
            .takeLast(pageSize)
            .map { it.toResult() }
            .let { results ->
                LuceneSearchResults(results , Page(pageNumber+1).takeIf { results.size >= pageSize })
            }

    fun getSortedDocValues(fieldName : String) = leafReader.getSortedDocValues(fieldName)

    fun <T> commit(operations : Commit.()->T) =
        commitImpl.operations().also {
            commit()
        }
    fun commit() = writer.commit()
    override fun close() {
        commit()
        writer.close()
    }
}

fun DocumentsIndex.reindex(transformer : (old : Document)->Document) =
    commit {
        getAll().forEach {
            update(it.document.uniqueField , transformer(it.document))
        }
    }

//fun DocumentsIndex.getAll() = search(MatchAllDocsQuery() , pageSize = Int.MAX_VALUE)
fun DocumentsIndex.getAll(): Sequence<ScoredDocument> = sequence {
    var pageNumber = 0
    while (true) {
        val page = search(MatchAllDocsQuery(), pageSize = pageSize, pageNumber = pageNumber)
        if (page.items.isEmpty()) break
        yieldAll(page.items)
        pageNumber++
        if (page.nextPage == null) break
    }
}
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
    getSortedDocValues(fieldName)?.let { docValues ->
        List(docValues.valueCount) { index ->
            docValues.lookupOrd(index).utf8ToString()
        }.filterNotNull()
    }?:emptyList()

fun DocumentsIndex.getPage(page: Page , query: Query , sortBy: Sort = DEFAULT_SORT) = with(page) {
    search(query , sortBy , pageSize , pageNumber).also { it.nextPage?.metaData = page.metaData }
}

data class ScoredDocument(val document: Document, val score : Float)
data class LuceneSearchResults(val items : List<ScoredDocument>, val nextPage : Page?)
@Serializable data class Page(val pageNumber : Int, var metaData : MutableMap<String,String> = HashMap())


/** Multiplies a query's score by [weight] */
infix fun Query.weight(weight : Double) =
    BoostQuery(this , weight.toFloat())

fun combineQueries(
    must : List<Query?> = emptyList(),
    boostWith : List<Query?> = emptyList(),
    except : List<Query?> = emptyList(),
    scorelessFilters : List<Query?> = emptyList()
) =
    BooleanQuery.Builder().add(
        must.mapNotNull { BooleanClause(it , BooleanClause.Occur.MUST) } +
        boostWith.mapNotNull { BooleanClause(it , BooleanClause.Occur.SHOULD) } +
        except.mapNotNull { BooleanClause(it , BooleanClause.Occur.MUST_NOT) } +
        scorelessFilters.mapNotNull { BooleanClause(it , BooleanClause.Occur.FILTER) }
    ).build()

fun vectorNearestNeighbourQuery(field : String , vector : FloatArray , numberOfDocuments : Int , prefilter : Query? = null) : Query =
    KnnFloatVectorQuery(field , vector , numberOfDocuments , prefilter)

fun document(vararg fields : Field?) =
    Document().apply { fields.filterNotNull().forEach(::add) }