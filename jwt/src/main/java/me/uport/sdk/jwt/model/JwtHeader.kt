package me.uport.sdk.jwt.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Standard JWT header
 */
@Serializable
class JwtHeader(
    val typ: String = "JWT",

    val alg: String = ES256K
) {

    fun toJson(): String = jsonAdapter.encodeToString(serializer(), this)

    companion object {
        const val ES256K = "ES256K"
        const val ES256K_R = "ES256K-R"

        fun fromJson(headerString: String): JwtHeader =
            jsonAdapter.decodeFromString(serializer(), headerString)

        private val jsonAdapter =
            Json { isLenient = true; ignoreUnknownKeys = true; }
    }
}
