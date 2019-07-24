package me.uport.sdk.jwt.model

import kotlinx.serialization.*
import kotlinx.serialization.internal.LinkedHashMapClassDesc
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.getContextualOrDefault

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
    @ImplicitReflectionSerializer
    private fun Map<*, *>.toJsonObject(): JsonObject = JsonObject(map {
        it.key.toString() to it.value.toJsonElement()
    }.toMap())

    @ImplicitReflectionSerializer
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
            val jsonParser = Json(JsonConfiguration.Stable)
            val serializer = jsonParser.context.getContextualOrDefault(this)
            jsonParser.toJson(serializer, this)
        }
    }

    //////////////////////////////////////////////////
    // converting back to primitives (no type info) //
    //////////////////////////////////////////////////
    private fun JsonObject.toPrimitiveMap(): Map<String, Any?> =
        this.content.map {
            it.key to it.value.toPrimitive()
        }.toMap()

    private fun JsonElement.toPrimitive(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> this.toPrimitiveMap()
        is JsonArray -> this.map { it.toPrimitive() }
        is JsonLiteral -> {
            if (isString) {
                contentOrNull
            } else {
                booleanOrNull ?: longOrNull ?: doubleOrNull
            }
        }
        else -> null
    }

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val asJsonObject: JsonObject = decoder.decodeSerializableValue(JsonObjectSerializer)
        return asJsonObject.toPrimitiveMap()
    }

    override val descriptor: SerialDescriptor
        get() = LinkedHashMapClassDesc(StringDescriptor, PolymorphicClassDescriptor)

    @ImplicitReflectionSerializer
    override fun serialize(encoder: Encoder, obj: Map<String, Any?>) {
        val asJsonObj: JsonObject = obj.toJsonObject()
        encoder.encode(JsonObjectSerializer, asJsonObj)
    }

}