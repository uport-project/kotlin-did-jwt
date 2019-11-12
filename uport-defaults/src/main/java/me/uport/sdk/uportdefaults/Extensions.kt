package me.uport.sdk.uportdefaults

import me.uport.sdk.core.EthNetwork
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.httpsdid.WebDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.universaldid.DIDResolver
import me.uport.sdk.uportdid.UportDIDResolver

/**
 *
 * An extension function used to configure default [DIDResolver] for
 * all known networks and add using the DIDResolver Builder
 *
 */
fun DIDResolver.configureDefaultsWithInfura(infuraProjectId: String): DIDResolver {

    val mainnet = EthNetwork(
        name = "mainnet",
        networkId = "0x1",
        rpcUrl = "https://mainnet.infura.io/v3/${infuraProjectId}",
        ethrDidRegistry = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
        uPortRegistry = "2ngV6QowStW3ebKXYUhy43wCqeLXcuTfHj2"
    )

    val rinkeby = EthNetwork(
        name = "rinkeby",
        networkId = "0x4",
        rpcUrl = "https://rinkeby.infura.io/v3/${infuraProjectId}",
        ethrDidRegistry = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
        uPortRegistry = "2ogxWTKKfy6kwfqdgdEE6GCdoFD4vm4YRZe"
    )

    val ropsten = EthNetwork(
        name = "ropsten",
        networkId = "0x3",
        rpcUrl = "https://ropsten.infura.io/v3/${infuraProjectId}",
        ethrDidRegistry = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
        uPortRegistry = "2oKVhUttUcwaFAopRBGA21NDJoYcBb3a6iz"
    )

    val kovan = EthNetwork(
        name = "kovan",
        networkId = "0x2a",
        rpcUrl = "https://kovan.infura.io/v3/${infuraProjectId}",
        ethrDidRegistry = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
        uPortRegistry = "354S1QuCzkmKoQ3ADSLp1KtqAe8gZ74F9am"
    )

    return DIDResolver.Builder()
        .addResolver(
            // register default Ethr DID resolver for all known networks
            EthrDIDResolver.Builder()
                .addNetwork(mainnet)
                .addNetwork(rinkeby)
                .addNetwork(ropsten)
                .addNetwork(kovan)
                .build()
        )
        .addResolver(
            // register default Uport DID resolver for mainnet
            UportDIDResolver(JsonRPC(mainnet.rpcUrl))
        )
        .addResolver(
            // register default Uport DID resolver for rinkeby
            UportDIDResolver(JsonRPC(rinkeby.rpcUrl))
        )
        .addResolver(
            // register default Uport DID resolver for kovan
            UportDIDResolver(JsonRPC(kovan.rpcUrl))
        )
        .addResolver(
            // register default Uport DID resolver for ropsten
            UportDIDResolver(JsonRPC(ropsten.rpcUrl))
        )
        .addResolver(
            // register default Web DID resolver
            WebDIDResolver()
        )
        .build()
}
