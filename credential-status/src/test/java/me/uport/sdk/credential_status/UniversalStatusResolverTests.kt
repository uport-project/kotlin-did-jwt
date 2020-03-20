@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")

package me.uport.sdk.credential_status

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.runBlocking
import me.uport.credentialstatus.CredentialStatus
import me.uport.credentialstatus.StatusResolver
import me.uport.credentialstatus.UniversalStatusResolver
import me.uport.sdk.ethrdid.EthrDIDDocument
import me.uport.sdk.testhelpers.coAssert
import me.uport.sdk.universaldid.DIDDocument
import org.junit.Test
import java.math.BigInteger

class UniversalStatusResolverTests {

    private val successfulCred =
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJzdGF0dXMiOnsidHlwZSI6InRlc3QiLCJpZCI6InRlc3Q6MHgxMjM0NSJ9fQ.MFTcBa_41fN5NoNYxTNYi9WmssHIOHz7Y8CPTDTpG-E"

    private val didDoc = EthrDIDDocument.fromJson(
        """{
                "id": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74",
                "publicKey": [],
                "authentication": [{
                    "type": "Secp256k1SignatureAuthentication2018",
                    "publicKey": "did:ethr:0xf3beac30c498d9e26865f34fcaa57dbb935b0d74#owner"
                }],
                "service": [],
                "@context": "https://w3id.org/did/v1"
            }"""
    )

    private val testResolver = object : StatusResolver {

        override val method: String = "test"

        override suspend fun checkStatus(
            credential: String,
            didDoc: DIDDocument
        ): CredentialStatus {
            if (credential == successfulCred) {
                return TestStatus(BigInteger.ONE)
            } else {
                throw IllegalStateException("")
            }
        }
    }

    private data class TestStatus(
        val blockNumber: BigInteger
    ) : CredentialStatus

    @Test
    fun `throws error when no resolvers registered`() = runBlocking {
        coAssert {
            UniversalStatusResolver().checkStatus(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzMwNDczNTEsInN0YXR1cyI6eyJ0eXBlIjoiRXRoclN0YXR1c1JlZ2lzdHJ5MjAxOSIsImlkIjoicmlua2VieToweDFFNDY1MWRjYTVFZjM4NjM2ZTJFNEQ3QTZGZjRkMjQxM2ZDNTY0NTAifSwiaXNzIjoiZGlkOmV0aHI6MHgxZmNmOGZmNzhhYzUxMTdkOWM5OWI4MzBjNzRiNjY2OGQ2YWMzMjI5In0.MHabafA0UxJuQJ0Z-7Egb57WRlgj4_zf96B0LUhRyXgVDU5RABIczTTTXWjcuKVzhJc_-FuhRI8uQYmQQNxKzgA"
                ,
                didDoc
            )
        }.isFailure().all {
            isInstanceOf(IllegalStateException::class)
        }
    }

    @Test
    fun `throws error when status entry is blank`() = runBlocking {
        val resolver = UniversalStatusResolver()
        resolver.registerResolver(testResolver)
        coAssert {
            resolver.checkStatus(
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
                didDoc
            )
        }.isFailure().all {
            isInstanceOf(IllegalArgumentException::class)
        }
    }

    @Test
    fun `can register resolvers and use to check status`() = runBlocking {
        val resolver = UniversalStatusResolver()
        resolver.registerResolver(testResolver)
        val result = resolver.checkStatus(successfulCred, didDoc)
        assertThat(result).isEqualTo(TestStatus(BigInteger.ONE))
    }
}
