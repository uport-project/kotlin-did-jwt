package me.uport.sdk.jwt.model

import com.squareup.moshi.JsonAdapter
import me.uport.sdk.jsonrpc.moshi

/**
 * Standard JWT header
 */
class JwtHeader(
        val typ: String = "JWT",

        val alg: String = ES256K
) {

    fun toJson(): String = jsonAdapter.toJson(this)

    companion object {
        const val ES256K = "ES256K"
        const val ES256K_R = "ES256K-R"

        fun fromJson(headerString: String): JwtHeader? = jsonAdapter.lenient().fromJson(headerString)

        private val jsonAdapter: JsonAdapter<JwtHeader> by lazy { moshi.adapter(JwtHeader::class.java) }
    }
}