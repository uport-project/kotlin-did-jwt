package me.uport.credential_status


/**
 * Represents the result of a status check
 */
interface CredentialStatus {

}


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
interface StatusEntry {

}


/**
 * The method signature expected to be implemented by credential status resolvers
 */
interface StatusMethod {

}


/**
 *
 *  The interface expected for status resolvers.
 * `checkStatus` should be called with a raw credential and it should return a [CredentialStatus] result.
 * It is advisable that classes that implement this interface also provide a way to easily register the correct
 * Status method type.
 *
 */
interface StatusResolver {

}


interface JWTPayloadWithStatus {

}


interface StatusMethodRegistry {

}