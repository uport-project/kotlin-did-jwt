package me.uport.sdk.ethrdid

import me.uport.sdk.core.EthNetwork
import me.uport.sdk.jsonrpc.JsonRPC

/**
 * Encapsulates the configuration necessary to resolve an ethr-did (ERC1056)
 *
 * You can specify multiple of these configurations to an [EthrDIDResolver.Builder] to allow it to resolve ethr-DIDs
 * anchored in the respective networks.
 * example: `did:ethr:rinkeby:0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed`
 *
 * Please see https://github.com/uport-project/ethr-did-registry#contract-deployments for the known registry addresses.
 */
class EthrDIDNetwork(
    /**
     * name of the ethereum network (ex: "mainnet", "rinkeby")
     */
    val name: String,

    /**
     * address of the ERC1056 contract.
     * The default for the more popular networks is "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b"
     */
    val registryAddress: String,

    /**
     * A JsonRPC instance
     *
     * for example JsonRPC("https://rinkeby.infura.io/v3/<your infura api key>")
     */
    val rpc: JsonRPC,

    /**
     * The chain ID (see EIP-155 https://github.com/ethereum/EIPs/blob/master/EIPS/eip-155.md)
     */
    val chainId: String? = null
)

/**
 * converts a network description to an [EthrDIDNetwork] instance
 */
fun EthNetwork.toEthrDIDNetwork() =
    EthrDIDNetwork(
        name = name,
        chainId = networkId,
        registryAddress = ethrDidRegistry,
        rpc = JsonRPC(rpcUrl)
    )