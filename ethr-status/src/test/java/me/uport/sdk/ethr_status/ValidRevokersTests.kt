@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass", "StringLiteralDuplication")
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

    @Test
    fun `can generate list of revokers with publickey entries and ethr-did issuer`() {

        val didDoc = EthrDIDDocument.fromJson(
            """{
                        "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                        "publicKey": [{
                            "id": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                            "ethereumAddress": "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
                        },{
                            "id": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                            "ethereumAddress": "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
                        }],
                        "authentication": [{
                            "type": "Secp256k1SignatureAuthentication2018",
                            "publicKey": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner"
                        }],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }"""
        )

        val revokers = ethrStatus.getValidRevokers(didDoc)
        assertThat(revokers).isEqualTo(listOf("0xf3beac30c498d9e26865f34fcaa57dbb935b0d74", "0x108209f4247b7fe6605b0f58f9145ec3269d0154"))
    }

    @Test
    fun `can generate list of revokers with public key entries and non ethr-did issuer`() {

        val didDoc = EthrDIDDocument.fromJson(
            """{
                        "id": "did:uport:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                        "publicKey": [{
                            "id": "did:uport:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:uport:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                            "ethereumAddress": "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
                        },{
                            "id": "did:uport:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:uport:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                            "ethereumAddress": "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
                        }],
                        "authentication": [{
                            "type": "Secp256k1SignatureAuthentication2018",
                            "publicKey": "did:uport:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner"
                        }],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }"""
        )

        val revokers = ethrStatus.getValidRevokers(didDoc)
        assertThat(revokers).isEqualTo(listOf("0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"))
    }

    @Test
    fun `generates a distinct list of revokers`() {

        val didDoc = EthrDIDDocument.fromJson(
            """{
                        "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                        "publicKey": [{
                            "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#keys-1",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                            "ethereumAddress": "0x108209f4247b7fe6605b0f58f9145ec3269d0154"
                        },{
                            "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#keys-2",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                            "ethereumAddress": "0x108209f4247b7fe6605b0f58f9145ec3269d0154"
                        },{
                            "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#keys-2",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                            "ethereumAddress": "0x108209f4247b7fe6605b0f58f9145ec3269d0154"
                        },{
                            "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#keys-3",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                            "ethereumAddress": "0x108209f4247b7fe6605b0f58f9145ec3269d0154"
                        },{
                            "id": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#keys-4",
                            "type": "Secp256k1VerificationKey2018",
                            "owner": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154",
                            "ethereumAddress": "0x108209f4247b7fe6605b0f58f9145ec3269d0154"
                        }],
                        "authentication": [{
                            "type": "Secp256k1SignatureAuthentication2018",
                            "publicKey": "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#owner"
                        }],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }"""
        )

        val revokers = ethrStatus.getValidRevokers(didDoc)
        assertThat(revokers).isEqualTo(listOf("0x108209f4247b7fe6605b0f58f9145ec3269d0154"))
    }
}