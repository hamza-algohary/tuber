import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.inference.Predictor
import ai.djl.repository.zoo.Criteria

val sentenceTransformer: Predictor<String?, FloatArray?>? by lazy {
    Criteria<String, FloatArray>.builder()
        .setTypes(String::class.java, FloatArray::class.java)
        .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
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