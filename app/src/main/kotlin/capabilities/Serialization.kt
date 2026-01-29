package capabilities

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.schabi.newpipe.extractor.Page
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.util.Base64
import kotlin.reflect.KClass

/**
 * Conventions to stick to:
 * 1. Json serialization is by providing toJson() method, by implementing [JsonSerializable] interface.
 * 2. Json deserialization is by providing fromJson() method for Companion object of intended type.
 */

interface JsonSerializable {
    fun toJson() : String
}


fun <T> kStringSerializer(serialName : String , serialize : (T)->String , deserialize : (String)->T) =
    object : KSerializer<T> {
        override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: T) =
            encoder.encodeString(serialize(value))
        override fun deserialize(decoder: Decoder): T =
            deserialize(decoder.decodeString())
    }

/** Use with caution */
fun <T : Any> T.toBinaryString() : String =
    ByteArrayOutputStream().let { byteArrayOutputStream ->
        ObjectOutputStream(byteArrayOutputStream).use { it.writeObject(this) }
        Base64.getUrlEncoder().encodeToString(byteArrayOutputStream.toByteArray())
    }

/** Use with caution */
inline fun <reified T : java.io.Serializable> fromBinaryString(encoded: String): T =
    ObjectInputStream(
        ByteArrayInputStream(
            Base64.getUrlDecoder().decode(encoded)
        )
    ).use { it.readObject() } as T

/** Alternative to SerializersModuleBuilder for JSON **/
//fun serializersModule(contextuals : List<Contextual<*>>) : SerializersModule =
//    SerializersModule {
//        contextuals.forEach { contextual(it.kClass,it.serializer) }
//    }

//data class ContextualSerializer<T : Any>(val kClass: KClass<T>, val serializer : KSerializer<T>)
