package me.uport.sdk.jwt.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import me.uport.sdk.ethrdid.EthrDIDDocument
import me.uport.sdk.universaldid.AuthenticationEntry
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.PublicKeyType
import org.junit.Test

class EthrDIDTestHelpersTest {

    @Test
    fun `can create mock ethr-did document from an address`() {
        val address = "0xb9c5714089478a327f09197987f16f9e5d936e8a"
        val did = "did:ethr:$address"

        val referenceDDO = EthrDIDDocument(
                did,
                listOf(PublicKeyEntry(
                        "$did#owner",
                        PublicKeyType.Secp256k1VerificationKey2018,
                        did,
                        address
                )),
                listOf(AuthenticationEntry(
                        PublicKeyType.Secp256k1SignatureAuthentication2018,
                        "$did#owner"
                ))
        )

        val ddo = EthrDIDTestHelpers.mockDocForAddress(address)

        assertThat(ddo).isEqualTo(referenceDDO)
    }
}