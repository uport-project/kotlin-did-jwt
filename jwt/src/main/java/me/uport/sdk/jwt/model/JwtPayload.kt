package me.uport.sdk.jwt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.uport.sdk.jsonrpc.model.request.DynamicLookupSerializer

@Serializable
data class JwtPayload(

    /**
     * General
     */
    val iss: String = "", //Cannot be null for signature verification
    val iat: Long? = null,
    val nbf: Long? = null,
    val sub: String? = null,
    val aud: String? = null,
    val exp: Long? = null,
    val callback: String? = null,
    val type: String? = null,

    /**
     * Specific to selective disclosure REQUEST
     */
    val net: String? = null,
    val act: String? = null,
    val requested: List<String>? = null,
    val verified: List<String>? = null,
    val permissions: List<String>? = null,

    /*
     * Specific to selective disclosure RESPONSE
     * Also includes verified
     */
    /**
     * original jwt request, REQUIRED for sign selective disclosure responses
     */
    val req: String? = null,

    /**
     * The MNID of the Ethereum account requested using act in the Selective Disclosure Request
     */
    val nad: String? = null,

    /**
     * The devicekey as a regular hex encoded ethereum address as requested
     * using act='devicekey' in the Selective Disclosure Request
     */
    val dad: String? = null,

    @SerialName("own")
    val own: Map<String, String>? = null,

    /**
     * An array of JWT tokens giving client app the permissions requested.
     * Currently a token allowing them to send push notifications
     */
    val capabilities: List<String>? = null,

    /**
     * Specific to Verification
     * Also includes iss, sub, iat, exp, claim
     */
    @Serializable(with = ArbitraryMapSerializer::class)
    @SerialName("claim")
    val claims: Map<String, @Serializable(with = DynamicLookupSerializer::class) Any>? = null,
    /**
     * Specific to Private Chain
     * Also includes dad
     */
    val ctl: String? = null, //Ethereum address of the Meta Identity Manager used to control the account
    val reg: String? = null, //Ethereum address of the Uport Registry used on private chain
    val rel: String? = null, //Url of relay service for providing gas on private network
    val fct: String? = null, //Url of fueling service for providing gas on private network
    val acc: String? = null //Fuel token used to authenticate on above fct url
) {
    companion object {
        fun fromJson(payloadString: String): JwtPayload =
            jsonAdapter.decodeFromString(serializer(), payloadString)

        private val jsonAdapter =
            Json { isLenient = true; ignoreUnknownKeys = true }
    }
}
