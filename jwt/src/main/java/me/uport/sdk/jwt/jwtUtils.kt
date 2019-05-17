package me.uport.sdk.jwt

import me.uport.mnid.MNID

/**
 * convenience method used during token processing.
 * Splits JWT into parts.
 * @throws IllegalArgumentException if it can't split or if the number of parts != 3
 */
fun splitToken(token: String): Triple<String, String, String> {
    val parts: List<String>? = token.split('.', limit = 3)
    if (parts !== null && parts.size == 3) {
        return Triple(parts[0], parts[1], parts[2])
    } else {
        throw IllegalArgumentException("Token must have 3 parts: Header, Payload, and Signature")
    }
}

/**
 * convenience method used during token verification.
 * It normalizes uport and ethr DIDs.
 * It returns the inital string if it is unable to normalize it
 */
fun normalize(did: String): String {
  if (did.startsWith("did:")) {
    return did
  }
    if (MNID.isMNID(did)) {
        return "did:uport:$did"
    }

    val ethrAddressPattern = "(0x[0-9a-fA-F]{40})".toRegex()
    if (ethrAddressPattern.matches(did)) {
        return "did:ethr:$did"
    }

    return did
}
