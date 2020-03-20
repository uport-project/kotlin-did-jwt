package me.uport.sdk.universaldid

/**
 * Abstraction of various methods of resolving DIDs
 *
 * Each resolver should know the [method] it is supposed to resolve
 * and implement a [resolve] coroutine to eventually return a [DIDDocument] or throw an error
 */
interface DIDResolver {
    /**
     * The DID method that a particular implementation can resolve
     */
    val method: String

    /**
     * Resolve a given [did] in a coroutine and return the [DIDDocument] or throw an error
     */
    suspend fun resolve(did: String): DIDDocument

    /**
     * Check if the [potentialDID] can be resolved by this resolver.
     */
    fun canResolve(potentialDID: String): Boolean

    /**
     *
     * Builds a [DIDResolverImpl]
     * This class creates an[DIDResolverImpl] object and enables the registration of [DIDResolver]
     * using the Builder pattern
     *
     */
    class Builder {

        private val didResolver = DIDResolverImpl()

        /**
         *
         * Register's a [DIDResolver] to the [DIDResolverImpl]
         * @return this [Builder] instance
         *
         */
        fun addResolver(resolver: DIDResolver): Builder {
            didResolver.registerResolver(resolver)
            return this
        }

        /**
         * @return returns the configured [DIDResolver] object
         */
        fun build(): DIDResolver {
            return didResolver
        }
    }
}

