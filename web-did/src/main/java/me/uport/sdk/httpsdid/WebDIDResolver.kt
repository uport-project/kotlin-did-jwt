package me.uport.sdk.httpsdid

import me.uport.sdk.core.HttpClient
import me.uport.sdk.universaldid.BlankDocumentError
import me.uport.sdk.universaldid.DIDDocument
import me.uport.sdk.universaldid.DIDDocumentImpl
import me.uport.sdk.universaldid.DIDResolver
import me.uport.sdk.universaldid.DidResolverError

/**
 * This is a DID resolver implementation that supports the "web" DID method.
 * It accepts web-did strings and produces a document described at:
 * https://w3c-ccg.github.io/did-spec/#did-documents
 *
 * Example https did: "did:web:example.com"
 *
 * For compatibility reasons, this resolver also resolves the deprecated "https" method
 */
open class WebDIDResolver(private val httpClient: HttpClient = HttpClient()) : DIDResolver {
    override val method: String = "web"

    override suspend fun resolve(did: String): DIDDocument {
        if (canResolve(did)) {
            val (_, domain) = parseDIDString(did)
            val ddoString = getProfileDocument(domain)
            if (ddoString.isBlank()) {
                throw BlankDocumentError("no profile document found for `$did`")
            }
            return DIDDocumentImpl.fromJson(ddoString)
        } else {
            throw DidResolverError("The DID('$did') cannot be resolved by the HTTPS DID resolver")
        }
    }

    override fun canResolve(potentialDID: String): Boolean {
        val (method, _) = parseDIDString(potentialDID)
        return (method == this.method || method == DEPRECATED_METHOD)
    }

    private suspend fun getProfileDocument(domain: String): String {
        val url = "https://$domain/.well-known/did.json"
        return httpClient.urlGet(url)
    }

    companion object {

        private const val DEPRECATED_METHOD = "https"

        private val webDIDPattern =
            "^(did:(web|https):)?([-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])".toRegex()

        internal fun parseDIDString(did: String): Pair<String, String> {
            val matchResult = webDIDPattern.find(did) ?: return ("" to did)
            val (_, method, domain) = matchResult.destructured
            return (method to domain)
        }
    }
}
