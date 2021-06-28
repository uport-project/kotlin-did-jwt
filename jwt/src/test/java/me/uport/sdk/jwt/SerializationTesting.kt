package me.uport.sdk.jwt

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.uport.sdk.jsonrpc.model.request.DynamicLookupSerializer
import me.uport.sdk.jwt.model.ArbitraryMapSerializer
import org.junit.Test

@ExperimentalSerializationApi
class SerializationTesting {

    @Serializable
    data class ClassWithSerializer(
        //test serialization of annotated types
        @SerialName("@context")
        val context: List<String>
    )

    @ExperimentalSerializationApi
    @Serializable
    data class CompoundTestObject(
        //use custom serializers for arbitrary map types
        @Serializable(with = ArbitraryMapSerializer::class)
        val generic: Map<String, @Serializable(DynamicLookupSerializer::class) Any?>
    )

    @Test
    fun `can serialize object with any map`() {
        val aa = CompoundTestObject(
            mapOf(
                "hello" to "world",
                "missing" to null,
                "some number" to 4321,
                "number as string" to "1234",
                "boolean" to false,
                "boolean as string" to "true",
                "custom object" to ClassWithSerializer(listOf("asdf")),
                "obj" to mapOf(
                    "a" to "b",
                    "c" to null
                )
            )
        )
        val serialized =
            Json.encodeToString(CompoundTestObject.serializer(), aa)
        assertThat(serialized).isEqualTo("""{"generic":{"hello":"world","missing":null,"some number":4321,"number as string":"1234","boolean":false,"boolean as string":"true","custom object":{"@context":["asdf"]},"obj":{"a":"b","c":null}}}""")
    }

    @Test
    fun `can deserialize known object`() {
        val input =
            """{"generic":{"hello":"world","missing":null,"some number":4321,"number as string":"1234","boolean":false,"boolean as string":"true","custom object":{"@context":["asdf"]},"obj":{"a":"b","c":null}}}"""
        val parsed = Json.decodeFromString(CompoundTestObject.serializer(), input)
        assertThat(parsed.generic).isEqualTo(
            mapOf(
                "hello" to "world",
                "missing" to null,
                "some number" to 4321L,
                "number as string" to "1234",
                "boolean" to false,
                "boolean as string" to "true",
                "custom object" to mapOf("@context" to listOf("asdf")),
                "obj" to mapOf(
                    "a" to "b",
                    "c" to null
                )
            )
        )
    }
}
