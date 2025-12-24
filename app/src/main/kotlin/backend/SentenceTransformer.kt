import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer
import ai.djl.ndarray.*
import ai.djl.ndarray.types.DataType
import ai.djl.translate.*
import kotlin.math.sqrt
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.inference.Predictor
import ai.djl.repository.zoo.Criteria

class EmbeddingGemmaTranslator(
    private val promptPrefix: String? = null
) : Translator<String, FloatArray> {

    private lateinit var tokenizer: HuggingFaceTokenizer
    private lateinit var lastAttentionMask: NDArray

    override fun prepare(ctx: TranslatorContext) {
        tokenizer = HuggingFaceTokenizer.newInstance(
            ctx.model.modelPath
        )
    }

    override fun processInput(ctx: TranslatorContext, input: String): NDList {
        val text = if (promptPrefix != null) {
            "$promptPrefix$input"
        } else {
            input
        }

        val encoding = tokenizer.encode(text)
        val manager = ctx.ndManager

        val inputIds = manager.create(encoding.ids).expandDims(0)
        lastAttentionMask = manager
            .create(encoding.attentionMask)
            .toType(DataType.FLOAT32, false)
            .expandDims(0)

        return NDList(inputIds, lastAttentionMask)
    }

    override fun processOutput(ctx: TranslatorContext, outputs: NDList): FloatArray {
        val lastHiddenState = outputs[0]           // (1, seq_len, 768)
        val tokenEmbeddings = lastHiddenState.squeeze(0)

        val attentionMask = lastAttentionMask.squeeze(0)
        val maskExpanded = attentionMask.expandDims(1)

        val maskedEmbeddings = tokenEmbeddings.mul(maskExpanded)
        val sumEmbeddings = maskedEmbeddings.sum()
        val tokenCount = maskExpanded.sum()

        val meanEmbedding = sumEmbeddings.div(tokenCount)

        // ---- L2 normalization (manual) ----
        val squared = meanEmbedding.mul(meanEmbedding)
        val sumSquares = squared.sum()
        val norm = sqrt(sumSquares.getFloat())

        val normalized = meanEmbedding.div(norm)

        return normalized.toFloatArray()
    }

    override fun getBatchifier(): Batchifier? = null
}

//val sentenceTransformer: Predictor<String, FloatArray>? by lazy {
//    Criteria.builder()
//        .setTypes(String::class.java, FloatArray::class.java)
////        .optModelUrls("djl://ai.djl.huggingface.pytorch/google/embeddinggemma-300m")
//        .optModelUrls("https://huggingface.co/google/embeddinggemma-300m")
//        .optEngine("PyTorch")
//        .optOption("modelType", "huggingface")
//        .optTranslator(
//            EmbeddingGemmaTranslator(
//                "task: search result | query: "
//            )
//        )
//        .build()
//        .loadModel()
//        .newPredictor()
//}

val sentenceTransformer: Predictor<String?, FloatArray?>? by lazy {
    Criteria<String, FloatArray>.builder()
        .setTypes(String::class.java, FloatArray::class.java)
        //.optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
        .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
        .optEngine("PyTorch")
        .optTranslatorFactory(TextEmbeddingTranslatorFactory())
        .build()
        .loadModel()
        .newPredictor()
}

fun transformSentence(text: String): FloatArray? =
    sentenceTransformer?.predict(text)
