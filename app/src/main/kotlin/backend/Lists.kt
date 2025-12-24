package backend

import backend.add
import backend.query
import kotlinx.serialization.json.Json
import org.apache.lucene.document.Document
import org.apache.lucene.document.Field
import org.apache.lucene.document.Field.Store
import org.apache.lucene.document.KnnFloatVectorField
import org.apache.lucene.document.LongPoint
import org.apache.lucene.document.StoredField
import org.apache.lucene.document.StringField
import org.apache.lucene.document.TextField
import org.apache.lucene.index.Term
import org.apache.lucene.index.VectorSimilarityFunction
import org.apache.lucene.search.Sort
import org.apache.lucene.search.SortField
import org.apache.lucene.search.TermQuery
import services.Summary
import services.emptyChannelSummary
import services.toPlainText
import services.type
import transformSentence

fun Summary.toDocument(vararg additionalFields : Field) =
    document(
        TextField("name", name, Store.NO) ,
        TextField("description" , description.toPlainText() , Store.NO),
//        KnnFloatVectorField("name-embedding" , transformSentence(name?:"") , VectorSimilarityFunction.COSINE),
//        KnnFloatVectorField("description-embedding" , transformSentence(description.toPlainText()) , VectorSimilarityFunction.COSINE),
        StringField("type",type , Store.NO),
        StringField("url", url , Store.NO), // For sorting/pagination
        StoredField("json" , Json.encodeToString(this)),
        *categories.map {
            StringField("category" , it.name , Store.NO)
        }.toTypedArray(),
        *additionalFields
    )

fun Document.toSummary() : Summary? =
    get("json")?.let(Json::decodeFromString)

class Lists(indexDirectory : String , pageSize : Int = 20) {
    val index = DocumentsIndex(indexDirectory , uniqueFieldName = "url" , pageSize = pageSize)

    fun add(summary : Summary , listName : String) =
        index.add(
            summary.toDocument(*filterField("list",listName))
        )

    fun search(listName : String , query : String) =
        index.search(
            combine(
                must = listOf(
                    anyOf(
                        fuzzyPhraseQuery("name", query),
                        fuzzyPhraseQuery("description", query),
//                        transformSentence(query)?.let { vectorNearestNeighbourQuery("name-embedding",it , index.pageSize , TermQuery(Term("list", listName))) },
//                        transformSentence(query)?.let { vectorNearestNeighbourQuery("description-embedding",it , index.pageSize , TermQuery(Term("list", listName))) },
                    ),
                ),
                scorelessFilters = listOf(
                    TermQuery(Term("list", listName))
                )
            )
        )

    fun lists() = index.getAllValuesOf("list")
    fun channels(listName : String) =
        index.query(
            TermQuery(Term("type", emptyChannelSummary.type)) ,
            pageSize = Int.MAX_VALUE
        )

}