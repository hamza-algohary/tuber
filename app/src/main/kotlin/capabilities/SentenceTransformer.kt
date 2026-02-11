package capabilities

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.inference.Predictor
import ai.djl.repository.zoo.Criteria

private object Models {
    const val All_MiniLM_L6V2 = "all-MiniLM-L6-v2"
    const val Paraphrase_Multilingual_MiniLM_L12_V2 = "paraphrase-multilingual-MiniLM-L12-v2"
}

val sentenceTransformer: Predictor<String?, FloatArray?>? by lazy {
    Criteria<String, FloatArray>.builder()
        .setTypes(String::class.java, FloatArray::class.java)
        .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/${Models.Paraphrase_Multilingual_MiniLM_L12_V2}")
        .optEngine("PyTorch")
        .optTranslatorFactory(TextEmbeddingTranslatorFactory())
        .build()
        .loadModel()
        .newPredictor()
}

fun transformSentence(text: String): FloatArray? =
    sentenceTransformer?.predict(text)

fun transformSentences(texts : List<String>) : List<FloatArray?> =
    sentenceTransformer?.batchPredict(texts)?:emptyList()

fun List<String>.sortBySimilarityTo(reference : String) =
    transformSentence(reference).let { reference ->
        sortedBy { sentence ->
            cosineSimilarity(transformSentence(sentence)!! , reference!!)
        }
    }


fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
    require(a.size == b.size)

    var dot = 0.0
    var normA = 0.0
    var normB = 0.0

    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }

    return (dot / (Math.sqrt(normA) * Math.sqrt(normB))).toFloat()
}