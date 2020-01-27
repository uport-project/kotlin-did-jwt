package me.uport.sdk.ethr_status

import assertk.assertThat
import assertk.assertions.isEqualTo
import me.uport.sdk.ethrdid.EthrDIDDocument
import org.junit.Test

class ValidRevokersTests {

    private val ethrStatus = EthrStatusResolver()

    @Test
    fun `can generate list of revokers when no publickey entries`() {

        val didDoc = EthrDIDDocument.fromJson(
            """{
                "id": "did:ethr:0x1fcf8ff78ac5117d9c99b830c74b6668d6ac3229",
                "publicKey": [],
                "authentication": [{
                    "type": "Secp256k1SignatureAuthentication2018",
                    "publicKey": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner"
                }],
                "service": [],
                "@context": "https://w3id.org/did/v1"
            }"""
        )

        val revokers = ethrStatus.getValidRevokers(didDoc)
        assertThat(revokers).isEqualTo(listOf("0x1fcf8ff78ac5117d9c99b830c74b6668d6ac3229"))
    }
}