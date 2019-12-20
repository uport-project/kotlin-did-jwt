package me.uport.sdk.jwt.test

import me.uport.sdk.ethrdid.EthrDIDDocument
import me.uport.sdk.universaldid.AuthenticationEntry
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.PublicKeyType

open class EthrDIDTestHelpers {

    companion object {

        //language=RegExp
        private val didParsePattern = "^(did:)?((\\w+):)?((\\w+):)?(0x[0-9a-fA-F]{40})".toRegex()

        fun mockDocForAddress(potentialDID: String): EthrDIDDocument {

            val matchResult = didParsePattern.find(potentialDID)
                ?: throw IllegalArgumentException("can't parse potentialDID or did")
            val (_, _, _, _, network, address) = matchResult.destructured
            val did =
                if (network.isBlank() || network == "mainnet") "did:ethr:$address" else "did:ethr:$network:$address"

            return EthrDIDDocument(
                id = did,
                publicKey = listOf(
                    PublicKeyEntry(
                        id = "$did#owner",
                        type = PublicKeyType.Secp256k1VerificationKey2018,
                        controller = did,
                        ethereumAddress = address
                    )
                ),
                authentication = listOf(
                    AuthenticationEntry(
                        type = PublicKeyType.Secp256k1SignatureAuthentication2018,
                        publicKey = "$did#owner"
                    )
                )
            )
        }
    }

}