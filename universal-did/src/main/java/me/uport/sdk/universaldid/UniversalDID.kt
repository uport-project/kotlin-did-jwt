package me.uport.sdk.universaldid

import me.uport.sdk.universaldid.UniversalDID.method
import me.uport.sdk.universaldid.UniversalDID.registerResolver

/**
 * A class to abstract resolving Decentralized Identity (DID) documents
 * from specific implementations based on the [method] component of a DID [String]
 *
 * [DIDResolver] implementations need to be registered using [registerResolver]
 *
 * Known implementations of [DIDResolver] are [ethr-did] and [uport-did]
 */
@Suppress("DEPRECATION")
@Deprecated(
    "Resolving DIDs using the UniversalDID singleton is deprecated " +
            "in favor of using the DIDResolver Builder." +
            "This will be removed in the next major release.",
    ReplaceWith(
        """val resolver : DIDResolver = DIDResolver.Builder
                                .addResolver(ethrDidResolver)
                                .addResolver(/*...*/)
                                .build()"""
    )
)
object UniversalDID : DIDResolver {

    private val resolvers = mapOf<String, DIDResolver>().toMutableMap()

    /**
     * Register a resolver for a particular DID [method]
     */
    fun registerResolver(resolver: DIDResolver) {
        if (resolver.method.isBlank()) {
            return
        }
        resolvers[resolver.method] = resolver
    }

    /**
     * @hide
     */
    fun clearResolvers() = resolvers.clear()

    /**
     * This universal resolver can't be used for any one particular did but for all [DIDResolver]s
     * that have been added using [registerResolver]
     */
    override val method: String = ""

    /**
     * Checks if any of the registered resolvers can resolve
     */
    override fun canResolve(potentialDID: String): Boolean {
        val resolver = resolvers.values.find {
            it.canResolve(potentialDID)
        }
        return (resolver != null)
    }

    /**
     * Looks for a [DIDResolver] that can resolve the provided [did] either by method if the did contains one or by trial
     *
     * @throws IllegalStateException if the proper resolver is not registered or produces `null`
     * @throws IllegalArgumentException if the given [did] has no `method` but could be resolved by one of the registered resolvers and that one fails with `null`
     */
    override suspend fun resolve(did: String): DIDDocument {
        val (method, _) = parse(did)

        if (method.isBlank() || !resolvers.containsKey(method)) {
            //if there is no clear mapping to a resolver, try each one that claims it can resolve
            return resolvers.filterValues {
                it.canResolve(did)
            }.values.mapNotNull {
                try {
                    it.resolve(did)
                } catch (ex: Exception) {
                    null
                }
            }.firstOrNull()
                ?: throw IllegalArgumentException("The provided did ($did) could not be resolved by any of the ${resolvers.size} registered resolvers")
        }  //no else clause, carry on

        if (resolvers.containsKey(method)) {
            return resolvers[method]?.resolve(did)
                ?: throw IllegalStateException("There DIDResolver for '$method' failed to resolve '$did' for an unknown reason.")
        } else {
            throw IllegalStateException("There is no DIDResolver registered to resolve '$method' DIDs and none of the other ${resolvers.size} registered ones can do it.")
        }
    }

    /**
     * @hide
     */
    internal fun parse(did: String): Pair<String, String> {
        val matchResult = didPattern.find(did) ?: return ("" to "")
        val (method, identifier) = matchResult.destructured
        return (method to identifier)
    }

    //language=RegExp
    private val didPattern = "^did:(.*?):(.+)".toRegex()
}