import capabilities.cosineSimilarity
import capabilities.transformSentence
import kotlin.collections.map
import kotlin.test.Test
import kotlin.test.assertTrue

class SentenceTransformerTest {
    @Test fun embeddingQuickSanityCheck() {
        val v = transformSentence("Mars is the red planet")
        println(v!!.size)           // should be 768
        println(v.map{it*it}.sum()) // should be ~1.0
    }
    @Test fun `similar sentences are closer than unrelated ones`() {
        val s1 = "The cat is sitting on the sofa"
        val s2 = "A kitty is lying on a couch"
        val s3 = "Quantum mechanics describes subatomic particles"

        val e1 = transformSentence(s1)!!
        val e2 = transformSentence(s2)!!
        val e3 = transformSentence(s3)!!

        val sim12 = cosineSimilarity(e1, e2)
        val sim13 = cosineSimilarity(e1, e3)

        println("Similarity(s1, s2) = ${cosineSimilarity(e1, e2)}")
        println("Similarity(s1, s3) = ${cosineSimilarity(e1, e3)}")
        println("Similarity(s2, s3) = ${cosineSimilarity(e2, e3)}")

        assertTrue(
            sim12 > sim13,
            "Related sentences should be more similar than unrelated ones"
        )

        // Optional but useful threshold
        assertTrue(
            sim12 > 0.5f,
            "Expected reasonably high similarity for paraphrases"
        )
    }
}