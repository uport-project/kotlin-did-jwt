package me.uport.credential_status

import me.uport.sdk.jwt.JWTTools

/**
 *
 * Convenience method which is used to extract the status entry from a credential
 */
fun getStatusEntry(credential: String): StatusEntry {
    val (_, payloadRaw) = JWTTools().decodeRaw(credential)
    val status = payloadRaw["status"] as Map<String, String>?
        ?: throw IllegalArgumentException("No entry for status found in jwt")

    return StatusEntry(
        status["type"] ?: "",
        status["id"] ?: ""
    )
}