package me.uport.sdk.jwt

/**
 * Utilities for dealing with known JWT and DID types and formats
 */
object JWTUtils {

    /**
     * convenience method used during token processing.
     * Splits JWT into parts.
     * @throws IllegalArgumentException if it can't split or if the number of parts != 3
     */
    @Suppress("MagicNumber")
    fun splitToken(token: String): Triple<String, String, String> {
        val parts: List<String>? = token.split('.', limit = 3)
        if (parts !== null && parts.size == 3) {
            return Triple(parts[0], parts[1], parts[2])
        } else {
            throw IllegalArgumentException("Token must have 3 parts: Header, Payload, and Signature")
        }
    }

    /**
     * Attempts to normalize a [potentialDID] to a known format.
     *
     * @return This will transform an ethereum address into an ethr-did
     * Other cases return the original string
     */
    fun normalizeKnownDID(potentialDID: String): String {

        val ethPattern = "^(0[xX])*([0-9a-fA-F]{40})".toRegex()

        val matchResult = ethPattern.matchEntire(potentialDID)
        if (matchResult != null) {
            val (_, hexDigits) = matchResult.destructured
            return "did:ethr:0x$hexDigits"
        }

        return potentialDID
    }
}
