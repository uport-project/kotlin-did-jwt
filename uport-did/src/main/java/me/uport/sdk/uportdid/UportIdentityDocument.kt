@file:Suppress("EXPERIMENTAL_API_USAGE")

package me.uport.sdk.uportdid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import me.uport.sdk.universaldid.AuthenticationEntry
import me.uport.sdk.universaldid.DIDDocument
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.PublicKeyType.Companion.Curve25519EncryptionPublicKey
import me.uport.sdk.universaldid.PublicKeyType.Companion.Secp256k1SignatureAuthentication2018
import me.uport.sdk.universaldid.PublicKeyType.Companion.Secp256k1VerificationKey2018
import me.uport.sdk.uportdid.UportDIDResolver.Companion.parseDIDString
import org.komputing.khex.extensions.clean0xPrefix

/**
 * A class that encapsulates the legacy uport-did profile document
 *
 * See [identity_document spec](https://github.com/uport-project/specs/blob/develop/pki/identitydocument.md)
 *
 */
@Suppress("DEPRECATION")
@Serializable
@Deprecated(message = "this was replaced by UportDIDDocument. use `convertToDIDDocument` to make the transition")
data class UportIdentityDocument(
    @SerialName("@context")
    val context: String? = "http://schema.org",

    @SerialName("@type")
    val type: String, //ex: "Organization", "Person"

    val publicKey: String? = null,  //ex: "0x04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab062"

    val publicEncKey: String? = null,  //ex: "0x04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab062"

    val image: ProfilePicture? = null,     //ex: {"@type":"ImageObject","name":"avatar","contentUrl":"/ipfs/QmSCnmXC91Arz2gj934Ce4DeR7d9fULWRepjzGMX6SSazB"}

    val name: String? = null, //ex: "uPort @ Devcon3" , "Vitalik Buterout"

    val description: String? = null // ex: "uPort Attestation"
) {

    /**
     * Converts the deprecated profile document model to a DID standard compliant [DIDDocument]
     */
    fun convertToDIDDocument(did: String): DIDDocument {

        val normalizedDid = normalizeDID(did)

        val publicVerificationKey = PublicKeyEntry(
            id = "$normalizedDid#keys-1",
            type = Secp256k1VerificationKey2018,
            owner = normalizedDid,
            publicKeyHex = this.publicKey?.clean0xPrefix()
        )
        val authEntries = listOf(
            AuthenticationEntry(
                type = Secp256k1SignatureAuthentication2018,
                publicKey = "$normalizedDid#keys-1"
            )
        )

        val pkEntries = listOf(publicVerificationKey).toMutableList()

        if (publicEncKey != null) {
            pkEntries.add(
                PublicKeyEntry(
                    id = "$normalizedDid#keys-2",
                    type = Curve25519EncryptionPublicKey,
                    owner = normalizedDid,
                    publicKeyBase64 = publicEncKey
                )
            )
        }

        return UportDIDDocument(
            context = "https://w3id.org/did/v1",
            id = normalizedDid,
            publicKey = pkEntries,
            authentication = authEntries,
            uportProfile = copy(
                context = null,
                publicEncKey = null,
                publicKey = null
            )
        )
    }

    private fun normalizeDID(did: String): String {
        val (_, mnid) = parseDIDString(did)
        return "did:uport:$mnid"
    }

    companion object {

        fun fromJson(json: String): UportIdentityDocument? = jsonParser.parse(serializer(), json)

        /**
         * Attempts to deserialize a json string into a profile document
         */
        private val jsonParser = Json(JsonConfiguration(encodeDefaults = false, strictMode = false))
    }
}

/**
 * encapsulates a profile picture field of a profile document
 */
@Suppress("unused")
@Serializable
class ProfilePicture(
    @SerialName("@type")
    val type: String? = "ImageObject",

    val name: String? = "avatar",

    @Suppress("unused")
    val contentUrl: String? = ""
)

