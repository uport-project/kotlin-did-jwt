package me.uport.credential_status


/**
 * Represents a status method entry that could be embedded in a W3C Verifiable Credential.
 * Normally, only credentials that list a status method would need to be verified by it.
 *
 * ex:
 * ```json
 * status : { type: "EthrStatusRegistry2019", id: "rinkeby:0xregistryAddress" }
 * ```
 *
 */
data class StatusEntry (val type: String, val id: String)


/**
 *
 *  The interface expected for status resolvers.
 * `checkStatus` should be called with a raw credential and it should return a [CredentialStatus] result.
 * It is advisable that classes that implement this interface also provide a way to easily register the correct
 * Status method type.
 *
 */
interface StatusResolver {

    /*
     * Holds the revocation method name
     */
    val method: String


    fun checkStatus(credential: String): Boolean
}