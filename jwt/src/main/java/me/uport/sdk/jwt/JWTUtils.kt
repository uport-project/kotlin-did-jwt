package me.uport.sdk.jwt

import me.uport.mnid.MNID

/**
 * Utilities for dealing with known JWT and DID types and formats
 */
class JWTUtils {

    companion object {

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
         * Attempts to normalize a [potentialDID] to a known format.
         *
         * @return This will transform an ethereum address into an ethr-did
         * and an MNID string into a uport-did
         * Other cases return the original string
         */
        fun normalizeKnownDID(potentialDID: String): String {

            //ignore if it's already a did
            if (potentialDID.matches("^did:(.*)?:.*".toRegex()))
                return potentialDID

            //match an ethereum address
            "^(0[xX])*([0-9a-fA-F]{40})".toRegex().find(potentialDID)?.let {
                val (_, hexDigits) = it.destructured
                return "did:ethr:0x$hexDigits"
            }

            //match an MNID
            if (MNID.isMNID(potentialDID)) {
                return "did:uport:$potentialDID"
            }

            return potentialDID
        }

    }
}
