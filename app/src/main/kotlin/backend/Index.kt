package backend

import backend.query
import com.ibm.icu.text.Normalizer2
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.icu.ICUFoldingFilter
import org.apache.lucene.analysis.icu.ICUNormalizer2Filter
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizer
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.SortedDocValuesField
import org.apache.lucene.document.StringField
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
import org.apache.lucene.search.SortField
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.BytesRef
import kotlin.io.path.Path

/**
 * TODO:
 * 1. Add pagination support
 */

/**Credits: ChatGPT */
class MultiLangAnalyzer : Analyzer() {
    override fun createComponents(fieldName: String?): TokenStreamComponents {
        val t = ICUTokenizer()
        var s: TokenStream = ICUFoldingFilter(t) // diacritics
        s = LowerCaseFilter(s) // lowercase
        s = ICUNormalizer2Filter(
            s,
            Normalizer2.getNFKCInstance()
        ) // canonical normalization
        return TokenStreamComponents(t, s)
    }
}


/**
 * Copied from: https://stackoverflow.com/a/76282522/13589728
 * Full example: https://github.com/deepjavalibrary/djl-demo/tree/master/huggingface/nlp/src/main/java/com/examples
 */
//val sentenceTransformer: Predictor<String?, FloatArray?>? by lazy {
//    Criteria<String, FloatArray>.builder()
//        .setTypes(String::class.java, FloatArray::class.java)
//        //.optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
//        .optModelUrls("djl://ai.djl.huggingface.pytorch/google/embeddinggemma-300m")
//        .optEngine("PyTorch")
//        .optTranslatorFactory(TextEmbeddingTranslatorFactory())
//        .build()
//        .loadModel()
//        .newPredictor()
//}
//
//fun transformSentence(text : String) : FloatArray? =
//        sentenceTransformer?.predict(text)



class DocumentsIndex(val path : String, val uniqueFieldName : String,  val pageSize : Int = 20, val analyzer : Analyzer = MultiLangAnalyzer()) {
    val directory = FSDirectory.open(Path(path))
    /** The point of making this lazy, is preventing writer from accidentally
     * opening and committing without actually even being referenced */
    val writer by lazy { IndexWriter(directory, IndexWriterConfig(analyzer))  }
    val reader by lazy { DirectoryReader.open(directory) }
    val leafReader : LeafReader by lazy { DirectoryReader.open(directory).leaves().get(0).reader() }
    val searcher by lazy { IndexSearcher(reader) }

    val Document.uniqueField get() = get(uniqueFieldName)
    val ScoreDoc.document get() = searcher.storedFields().document(doc)
    fun ScoreDoc.toResult() = Result(document,score)
    fun Array<ScoreDoc>.toResults(query: Query , sortBy: Sort = Sort(), pageSize: Int = this@DocumentsIndex.pageSize ) =
        Results( map{ it.toResult() } ,if (isEmpty()) null else Page(last() , query , sortBy , pageSize))
}

fun DocumentsIndex.update(uniqueField : String, document : Document) =
    writer.updateDocument(Term(uniqueFieldName , uniqueField) , document)

fun DocumentsIndex.remove(uniqueField : String) =
    writer.deleteDocuments(Term(uniqueFieldName , uniqueField))

fun DocumentsIndex.query(query: Query, sortBy : Sort = Sort(), pageSize: Int = this.pageSize) =
    searcher.search(query , pageSize , sortBy)
        .scoreDocs.map { it.toResult() }

fun DocumentsIndex.search(query: Query, sortBy : Sort = Sort()) =
    searcher.search(query , pageSize , sortBy)
            .scoreDocs.toResults(query , sortBy , pageSize)

fun DocumentsIndex.reindex(transformer : (old : Document)->Document) =
    getAll().forEach {
        update(it.document.uniqueField , transformer(it.document))
    }

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
fun DocumentsIndex.getAllValuesOf(fieldName : String) {
    leafReader.getSortedDocValues(fieldName)
}

fun DocumentsIndex.page(page: Page) = with(page) {
    searcher.searchAfter(after , query , pageSize , sortBy)
}


data class Result(val document: Document , val score : Float)
//data class LazyResult(val documentDynamicID : Int, val score : Float, private val searcher : IndexSearcher) {
//    val document by lazy {
//        searcher.storedFields().document(documentDynamicID)
//    }
//}
//val LazyResult.scoredDocument get() = Result(document,score)

data class Results(val items : List<Result>, val nextPage : Page?)
data class Page(val after : ScoreDoc, val query : Query, val sortBy: Sort, val pageSize : Int)

//fun List<ScoredDocument>.nextPage(vararg sortField : SortField , )


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

//fun BooleanQuery.Builder.must(vararg queries : Query) =
//    apply { add(queries.map { BooleanClause(it, BooleanClause.Occur.MUST) }) }

fun must(vararg queries : Query) =
    queries.map { BooleanClause(it, BooleanClause.Occur.MUST) }
fun boostWith(vararg queries : Query) =
    queries.map { BooleanClause(it, BooleanClause.Occur.SHOULD) }
fun except(vararg queries : Query) =
    queries.map { BooleanClause(it, BooleanClause.Occur.MUST_NOT) }
fun filterWith(vararg queries : Query) =
    queries.map { BooleanClause(it, BooleanClause.Occur.FILTER) }

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


fun document(vararg fields : Field) =
    Document().apply {
        fields.forEach(::add)
    }

fun filterField(name : String , value : String , store : Store = Store.NO) =
    arrayOf(
        StringField(name , value , store) ,
        SortedDocValuesField(name, BytesRef(value))
    )

fun test() {
    SortField.FIELD_SCORE
    val parser = QueryParser("title" , MultiLangAnalyzer())
    parser.parse("hello")

}