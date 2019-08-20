package me.uport.sdk.ethrdid

import org.walleth.khex.clean0xPrefix
import org.walleth.khex.prepend0xPrefix

/**
 * This encapsulates a mapping of names and chainIDs to [EthrDIDNetwork] configuration instances
 */
internal class RegistryMap {

    private val _regMap = mutableMapOf<String, EthrDIDNetwork>()

    fun registerNetwork(networkConfig: EthrDIDNetwork): RegistryMap {
        _regMap[networkConfig.name] = networkConfig
        if (networkConfig.chainId != null) {
            _regMap[normalizeQuantity(networkConfig.chainId)] = networkConfig
        }
        return this
    }

    operator fun get(query: String): EthrDIDNetwork {
        val cleanNetId = normalizeQuantity(query)
        return _regMap[cleanNetId]
            ?: _regMap[query]
            ?: throw IllegalArgumentException("No known configuration for the `[$query]` ethereum network")

    }

    companion object {
        private fun normalizeQuantity(id: String) = id.clean0xPrefix().trimStart('0').prepend0xPrefix()
    }
}