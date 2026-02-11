import ai.djl.Application
import ai.djl.repository.zoo.ModelZoo
import capabilities.shell
import kotlin.collections.forEach
import kotlin.test.Test

fun printTextEmbeddingModelsInZoo() {
    ModelZoo.listModels()[Application.NLP.TEXT_EMBEDDING]?.let { models ->
        println("number of text embedding models models: ${models.size}")
        models.forEach { model ->
            println(model)
            println()
        }
    }
}

fun runEmbeddingModelBenchMark() {
    listOf(
        ""
    )
}

class ScratchFile {
//    @Test
    fun scratchFile() {
        println(
            shell("gh", "release", "view", "--json", "tagName", "-q", ".tagName")?.output?.trim()?.split(".")?.runCatching {
                let { (major,minor,patch) ->
                    BuildInfo.Version(major.toInt(), minor.toInt(), patch.toInt())
                }
            } ?.getOrNull()?: BuildInfo.Version(0, 0, 0)
        )
    }
}