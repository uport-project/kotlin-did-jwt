package me.uport.sdk.jwt.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.encodeToJsonElement
import kotlin.reflect.typeOf

/**
 * This handles serialization and deserialization of arbitrary JSON trees represented as
 * `Map<String, Any?>`
 *
 * Serialization involves [ImplicitReflectionSerializer] so this will probably
 * not work in a multi-platform environment.
 *
 * Using `@Serializable` objects in the tree respects their declared serializers.
 *
 */
object ArbitraryMapSerializer : KSerializer<Map<String, Any?>> {

    ////////////////////////
    // converting to Json //
    ////////////////////////
    private fun Map<*, *>.toJsonObject(): JsonObject = JsonObject(map {
        it.key.toString() to it.value.toJsonElement()
    }.toMap())

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> this.toJsonObject()
        is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
        is Array<*> -> JsonArray(this.map { it.toJsonElement() })
        else -> {
            //supporting classes that declare serializers
            val jsonParser = Json
            jsonParser.encodeToJsonElement(this)
        }
    }

    //////////////////////////////////////////////////
    // converting back to primitives (no type info) //
    //////////////////////////////////////////////////
    private fun JsonObject.toPrimitiveMap(): Map<String, Any?> =
        this.map {
            it.key to it.value.toPrimitive()
        }.toMap()

    private fun JsonElement.toPrimitive(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> this.toPrimitiveMap()
        is JsonArray -> this.map { it.toPrimitive() }
        is JsonPrimitive -> {
            if (isString) {
                contentOrNull
            } else {
                booleanOrNull ?: longOrNull ?: doubleOrNull
            }
        }
        else -> null
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        assert(decoder is JsonDecoder) {"Only JsonDecoder supported, found " + decoder::class.java}
        val asJsonObject: JsonObject = (decoder as JsonDecoder).decodeJsonElement().jsonObject
        return asJsonObject.toPrimitiveMap()
    }

    @ExperimentalSerializationApi
    override val descriptor: SerialDescriptor
        get() = mapSerialDescriptor(PrimitiveSerialDescriptor("key", PrimitiveKind.STRING),
        PrimitiveSerialDescriptor("value", PrimitiveKind.STRING))

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        assert(encoder is JsonEncoder) {"Only JsonEncoder support, found " + encoder::class.java}
        val asJsonObj: JsonObject = value.toJsonObject()
        (encoder as JsonEncoder).encodeJsonElement(asJsonObj)
    }
}
