package me.uport.sdk.uportdefaults

import me.uport.sdk.core.Networks
import me.uport.sdk.ethrdid.EthrDIDNetwork
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.universaldid.DIDResolver


fun DIDResolver.configureDefaultsWithInfura(infuraProjectId : String) : DIDResolver {

    // blank did declarations
    /*val blankUportDID = "did:uport:2nQs23uc3UN6BBPqGHpbudDxBkeDRn553BB"
    val blankEthrDID = "did:ethr:0x0000000000000000000000000000000000000000"
    val blankHttpsDID = "did:https:example.com"

    // register default Ethr DID resolver if Universal DID is unable to resolve blank Ethr DID
    if (!.canResolve(blankEthrDID)) {

    }

    // register default Uport DID resolver if Universal DID is unable to resolve blank Uport DID
    if (!UniversalDID.canResolve(blankUportDID)) {
        val defaultRPC = JsonRPC(preferredNetwork?.rpcUrl ?: Networks.rinkeby.rpcUrl)
        UniversalDID.registerResolver(UportDIDResolver(defaultRPC))
    }

    // register default https DID resolver if Universal DID is unable to resolve blank https DID
    if (!UniversalDID.canResolve(blankHttpsDID)) {
        UniversalDID.registerResolver(WebDIDResolver())
    }
    */



    val defaultRPC = JsonRPC(preferredNetwork?.rpcUrl ?: Networks.mainnet.rpcUrl)
    val defaultRegistry = preferredNetwork?.ethrDidRegistry
        ?: Networks.mainnet.ethrDidRegistry
        EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", defaultRegistry, defaultRPC, "0x1"))
            .build()


    val resolver : DIDResolver = DIDResolver.Builder()
        .build()

    return resolver
}