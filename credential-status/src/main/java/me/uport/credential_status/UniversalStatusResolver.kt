package me.uport.credential_status

import me.uport.sdk.universaldid.DIDDocument
import me.uport.sdk.universaldid.UniversalDID.method
import me.uport.sdk.universaldid.UniversalDID.registerResolver

/**
 *
 * A class to abstract resolving credential statuses
 * from specific implementations based on the [method] component of a resolver
 *
 * [StatusResolver] implementations need to be registered using [registerResolver]
 *
 * Known implementations of [StatusResolver] are [ethr-status]
 *
 */
class UniversalStatusResolver : StatusResolver {


    private val resolvers = mapOf<String, StatusResolver>().toMutableMap()


    /**
     * This universal resolver can't be used for any one particular resolver but for all [StatusResolver]s
     * that have been added using [registerResolver]
     */
    override val method: String = ""


    /**
     * Looks for a [StatusResolver] that can check status using the provided [method]
     *
     * @throws IllegalStateException if the proper resolver is not registered or produces `null`
     */
    override suspend fun checkStatus(credential: String, didDoc: DIDDocument): CredentialStatus {

        val statusEntry = getStatusEntry(credential)

        if (statusEntry.type.isBlank() || !resolvers.containsKey(statusEntry.type)) {
            throw IllegalStateException("There is no StatusResolver registered to check status using '${statusEntry.type}' method.")
        } else {
            return resolvers[statusEntry.type]?.checkStatus(credential, didDoc)
                ?: throw IllegalStateException("There StatusResolver for '$statusEntry.type' failed to resolve for an unknown reason.")
        }
    }


    /**
     * Register a resolver for a particular [method]
     */
    fun registerResolver(resolver: StatusResolver) {
        if (resolver.method.isBlank()) {
            return
        }
        resolvers[resolver.method] = resolver
    }
}