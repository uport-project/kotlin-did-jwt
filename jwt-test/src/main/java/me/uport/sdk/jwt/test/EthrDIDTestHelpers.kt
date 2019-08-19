package me.uport.sdk.jwt.test

import io.mockk.coEvery
import io.mockk.spyk
import me.uport.sdk.core.Networks
import me.uport.sdk.ethrdid.EthrDIDDocument
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC

/**
 * This class provides some helper methods that may be used during testing to ease the burden of
 * mocking EthrDID documents.
 *
 *
 *
 * **This is usable only during testing.**
 *
 *
 *
 */
open class EthrDIDTestHelpers {

    companion object {

        /**
         * Builds the default DID doc for a given eth address.
         * This is the document that would be resolved for the address if no DID operations are ever performed.
         *
         *
         *
         * **This is usable only during testing.**
         *
         *
         */
        @JvmStatic
        fun mockDocForAddress(address: String): EthrDIDDocument {
            val did = "did:ethr:$address"
            return EthrDIDDocument.fromJson(
                """
            {
              "@context": "https://w3id.org/did/v1",
              "id": "$did",
              "publicKey": [{
                   "id": "$did#owner",
                   "type": "Secp256k1VerificationKey2018",
                   "owner": "$did",
                   "ethereumAddress": "$address"}],
              "authentication": [{
                   "type": "Secp256k1SignatureAuthentication2018",
                   "publicKey": "$did#owner"}]
            }
        """.trimIndent()
            )
        }

        /**
         * Creates a mock EthrDIDResolver that resolves to the default DID document for a particular address
         * without accessing the network.
         *
         * This has to be registered to [UniversalDID] for it to be used during JWT verification.
         *
         *
         *
         * **This is usable only during testing.**
         *
         *
         *
         *
         */
        @JvmStatic
        fun getMockResolverForAddress(address: String): EthrDIDResolver {
            val resolver = spyk(EthrDIDResolver(JsonRPC(Networks.mainnet.rpcUrl)))
            coEvery {
                resolver.resolve("did:ethr:$address")
            } returns mockDocForAddress(address)
            return resolver
        }
    }

}