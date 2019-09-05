package me.uport.sdk.ethrdid

import org.komputing.khex.extensions.clean0xPrefix
import org.komputing.khex.extensions.prepend0xPrefix

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

    fun getOrNull(query: String): EthrDIDNetwork? = try {
        this[query]
    } catch (ex: IllegalArgumentException) {
        null
    }

    companion object {

        private fun normalizeQuantity(id: String) = id.clean0xPrefix().trimStart('0').prepend0xPrefix()

        /**
         * build a usable registry map from a list of network configurations.
         * In case it is not defined in the list, this method also tries to define
         * a default network based on any network resembling `mainnet`
         */
        fun fromNetworks(networkConfigs: MutableList<EthrDIDNetwork>) = RegistryMap().apply {
            //register given networks
            networkConfigs.forEach { registerNetwork(it) }
            //if no default was provided, try to copy mainnet
            (getOrNull("") ?: getOrNull("mainnet") ?: getOrNull("0x1"))
                ?.let {
                    registerNetwork(
                        EthrDIDNetwork("", it.registryAddress, it.rpc, it.chainId)
                    )
                }
        }
    }
}

