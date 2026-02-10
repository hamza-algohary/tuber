package plugins.newpipe


//val json = Json {
//    serializersModule = SerializersModule {
//        contextual(
//            kStringSerializer(
//                "Page",
//                { it.toBinaryString() },
//                { fromBinaryString<org.schabi.newpipe.extractor.Page>(it) }
//            )
//        )
//    }
//}

//internal inline fun <reified T> T.toJson() = json.encodeToString(this)
//internal inline fun <reified T> fromJson(text : String) : T = json.decodeFromString<T>(text)

//object NewPipePageSerializer : KSerializer<Page> {
//    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Page", PrimitiveKind.STRING)
//    override fun serialize(encoder: Encoder, value: org.schabi.newpipe.extractor.Page) =
//        encoder.encodeString(value.toBinaryString())
//    override fun deserialize(decoder: Decoder): org.schabi.newpipe.extractor.Page =
//        fromBinaryString<Page>(decoder.decodeString())
//}

//private fun <T : Any> T.toBinaryString() : String {
//    val byteOut = ByteArrayOutputStream()
//    ObjectOutputStream(byteOut).use { it.writeObject(this) }
//    return Base64.getUrlEncoder().encodeToString(byteOut.toByteArray())
//}
//
//private inline fun <reified T : java.io.Serializable> fromBinaryString(encoded: String): T {
//    val bytes = Base64.getUrlDecoder().decode(encoded)
//    val obj = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }
//    return obj as T
//}
