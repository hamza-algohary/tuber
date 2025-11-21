package services.newpipe


import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import org.schabi.newpipe.extractor.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.Base64


public val json = Json {
    serializersModule = SerializersModule {
        contextual(Page::class, NewPipePageSerializer)
    }
}

//internal inline fun <reified T> T.toJson() = json.encodeToString(this)
internal inline fun <reified T> fromJson(text : String) : T = json.decodeFromString<T>(text)

object NewPipePageSerializer : KSerializer<Page> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Page", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: org.schabi.newpipe.extractor.Page) =
        encoder.encodeString(value.toBinaryString())
    override fun deserialize(decoder: Decoder): org.schabi.newpipe.extractor.Page =
        fromBinaryString<Page>(decoder.decodeString())
}

private fun <T : Any> T.toBinaryString() : String {
    val byteOut = ByteArrayOutputStream()
    ObjectOutputStream(byteOut).use { it.writeObject(this) }
    return Base64.getUrlEncoder().encodeToString(byteOut.toByteArray())
}

private inline fun <reified T : java.io.Serializable> fromBinaryString(encoded: String): T {
    val bytes = Base64.getUrlDecoder().decode(encoded)
    val obj = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
    return obj as T
}
