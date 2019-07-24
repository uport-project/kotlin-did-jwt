@file:Suppress("EXPERIMENTAL_API_USAGE")

package me.uport.sdk.universaldid

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

/**
 * Abstraction for DID documents
 */
interface DIDDocument {
    val context: String?
    val id: String?
    val publicKey: List<PublicKeyEntry>
    val authentication: List<AuthenticationEntry>
    val service: List<ServiceEntry>
}

/**
 * Encapsulates the fields of a Decentralized Identity Document
 * This is a shorthand implementation usable in most cases
 */
@Serializable
data class DIDDocumentImpl(
    @SerialName("@context")
    override val context: String = "https://w3id.org/did/v1",

    @SerialName("id")
    override val id: String, //ex: "did:https:example.com#owner"

    @SerialName("publicKey")
    override val publicKey: List<PublicKeyEntry> = emptyList(),

    @SerialName("authentication")
    override val authentication: List<AuthenticationEntry> = emptyList(),

    @SerialName("service")
    override val service: List<ServiceEntry> = emptyList()

) : DIDDocument {

    /**
     * Serializes this [DIDDocument] into a JSON string
     */
    fun toJson(): String = JSON
        .stringify(serializer(), this)

    companion object {

        private val JSON = Json(JsonConfiguration(
            encodeDefaults = true,
            strictMode = false,
            useArrayPolymorphism = false
        ))

        /**
         * Attempts to deserialize a given [json] string into a [DIDDocument]
         */
        fun fromJson(json: String) = JSON.parse(serializer(), json)
    }

}

@Serializable
data class PublicKeyEntry(
    @SerialName("id")
    val id: String,

    @SerialName("type")
    val type: PublicKeyType,

    @SerialName("owner")
    val owner: String,

    @SerialName("ethereumAddress")
    val ethereumAddress: String? = null,

    @SerialName("publicKeyHex")
    val publicKeyHex: String? = null,

    @SerialName("publicKeyBase64")
    val publicKeyBase64: String? = null,

    @SerialName("publicKeyBase58")
    val publicKeyBase58: String? = null,

    @SerialName("value")
    val value: String? = null
)

@Serializable
data class AuthenticationEntry(
    val type: PublicKeyType,
    val publicKey: String
)

@Serializable
data class ServiceEntry(
    val type: String,
    val serviceEndpoint: String
)

/**
 * This is a wrapper class for PublicKeyType
 * It is meant to provide a more type-safe way of dealing with these strings.
 * This will be ported to inline classes when that feature of kotlin stabilizes and works properly with serialization
 *
 * see DID Document spec:
 * https://w3c-ccg.github.io/did-spec/#did-documents
 */
@Serializable
data class PublicKeyType(val name: String) {

    @Serializer(forClass = PublicKeyType::class)
    companion object : KSerializer<PublicKeyType> {

        override fun serialize(encoder: Encoder, obj: PublicKeyType) =
            encoder.encodeString(obj.name)

        override fun deserialize(decoder: Decoder): PublicKeyType =
            PublicKeyType(decoder.decodeString())


        //////////////////////////////
        //some known key types
        //////////////////////////////

        /**
         * Default JWT signing key type used by uPort DIDs
         *
         * see usage in uPort spec: https://github.com/uport-project/specs/blob/develop/pki/diddocument.md
         */
        val Secp256k1VerificationKey2018 = PublicKeyType("Secp256k1VerificationKey2018")

        /**
         * references a [Secp256k1VerificationKey2018] in a DID document [AuthenticationEntry]
         */
        val Secp256k1SignatureAuthentication2018 = PublicKeyType("Secp256k1SignatureAuthentication2018")

        /**
         * While not directly generated here, it is treated as [Secp256k1VerificationKey2018]
         */
        val Secp256k1SignatureVerificationKey2018 = PublicKeyType("Secp256k1SignatureVerificationKey2018")

        /**
         * While not directly generated here, it is treated as [Secp256k1VerificationKey2018]
         */
        val EcdsaPublicKeySecp256k1 = PublicKeyType("EcdsaPublicKeySecp256k1")

        /**
         * encryption key. Usage described here: https://github.com/uport-project/specs/blob/develop/pki/diddocument.md
         */
        val Curve25519EncryptionPublicKey = PublicKeyType("Curve25519EncryptionPublicKey")

        /**
         * Used in ethr-did events to signal the existence of a [Secp256k1VerificationKey2018]
         * in the DID document.
         * Using a shorter key name reduces gas cost in DID operations.
         */
        val veriKey = PublicKeyType("veriKey")

        /**
         * Used in ethr-did events to signal the existence of a [Secp256k1SignatureAuthentication2018]
         * in the DID document.
         * Using a shorter key name reduces gas cost in DID operations.
         */
        val sigAuth = PublicKeyType("sigAuth")
    }
}
