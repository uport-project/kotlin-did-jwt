package me.uport.sdk.jwt.test

import me.uport.sdk.ethrdid.EthrDIDDocument

open class EthrDIDTestHelpers {

    companion object {

        fun mockDocForAddress(address: String): EthrDIDDocument {
            val did = "did:ethr:$address"
            return EthrDIDDocument.fromJson("""
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
        """.trimIndent())
        }
    }

}