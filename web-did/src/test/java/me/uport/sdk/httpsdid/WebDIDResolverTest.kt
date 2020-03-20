package me.uport.sdk.httpsdid

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import me.uport.sdk.core.HttpClient
import me.uport.sdk.testhelpers.coAssert
import me.uport.sdk.testhelpers.isInstanceOf
import me.uport.sdk.universaldid.AuthenticationEntry
import me.uport.sdk.universaldid.DIDDocumentImpl
import me.uport.sdk.universaldid.DidResolverError
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.PublicKeyType
import me.uport.sdk.universaldid.UniversalDID
import org.junit.Test
import java.io.IOException

class WebDIDResolverTest {

    //language=json
    private val exampleDidDocString = """{
  "@context": "https://w3id.org/did/v1",
  "id": "did:web:uport.me",
  "publicKey": [
    {
      "id": "did:web:uport.me#owner",
      "type": "Secp256k1VerificationKey2018",
      "owner": "did:web:uport.me",
      "publicKeyHex": "042b0af9b3ae6c7c3a90b01a3879d9518081bc0dcdf038488db9cb109b082a77d97ea3373e3dfde0eccd9adbdce11d0302ea5c098dbb0b310234c86895c8641622"
    }
  ],
  "authentication": [
    {
      "type": "Secp256k1SignatureAuthentication2018",
      "publicKey": "did:web:uport.me#owner"
    }
  ],
  "service": []
}""".trimIndent()

    private val exampleDidDoc = DIDDocumentImpl(
        context = "https://w3id.org/did/v1",
        id = "did:web:uport.me",
        publicKey = listOf(
            PublicKeyEntry(
                id = "did:web:uport.me#owner",
                type = PublicKeyType.Secp256k1VerificationKey2018,
                owner = "did:web:uport.me",
                publicKeyHex = "042b0af9b3ae6c7c3a90b01a3879d9518081bc0dcdf038488db9cb109b082a77d97ea3373e3dfde0eccd9adbdce11d0302ea5c098dbb0b310234c86895c8641622"
            )
        ),
        authentication = listOf(
            AuthenticationEntry(
                type = PublicKeyType.Secp256k1SignatureAuthentication2018,
                publicKey = "did:web:uport.me#owner"
            )
        ),
        service = emptyList()
    )


    @Test
    fun `can resolve valid dids`() {
        listOf(
            "did:https:example.com",
            "did:https:example.ngrok.com#owner",
            "did:web:example.com",
            "did:web:example.com#owner"
        ).forEach {
            assertThat(WebDIDResolver().canResolve(it)).isTrue()
        }

    }

    @Test
    fun `fails on invalid dids`() {
        listOf(
            "did:something:example.com", //different method
            "example.com"
        ).forEach {
            assertThat(WebDIDResolver().canResolve(it)).isFalse()
        }
    }

    @Test
    fun `fails when the endpoint doesn't provide a DID document`() = runBlocking {
        val http = mockk<HttpClient>()
        val tested = WebDIDResolver(http)
        coEvery { http.urlGet(any()) } returns ""

        coAssert {
            tested.resolve("did:web:example.com")
        }.thrownError {
            isInstanceOf(
                listOf(
                    IllegalArgumentException::class,
                    IOException::class,
                    SerializationException::class,
                    DidResolverError::class
                )
            )
        }
    }

    @Test
    fun `fails when the endpoint doesn't provide a DID document for deprecated https method`() =
        runBlocking {
            val http = mockk<HttpClient>()
            val tested = WebDIDResolver(http)
            coEvery { http.urlGet(any()) } returns ""

            coAssert {
                tested.resolve("did:https:example.com")
            }.thrownError {
                isInstanceOf(
                    listOf(
                        IllegalArgumentException::class,
                        IOException::class,
                        SerializationException::class,
                        DidResolverError::class
                    )
                )
            }
        }

    @Test
    fun `resolves document for deprecated https method`() = runBlocking {

        val http = mockk<HttpClient>()
        val tested = WebDIDResolver(http)

        coEvery { http.urlGet(any()) } returns exampleDidDocString

        val response = tested.resolve("did:https:example.com")
        assertThat(response).isEqualTo(exampleDidDoc)
    }

    @Test
    fun `resolves document`() = runBlocking {

        val http = mockk<HttpClient>()
        val tested = WebDIDResolver(http)

        coEvery { http.urlGet(any()) } returns exampleDidDocString

        val response = tested.resolve("did:web:example.com")
        assertThat(response).isEqualTo(exampleDidDoc)
    }


    @Test
    fun `resolves documents through universal resolver`() = runBlocking {

        val http = mockk<HttpClient>()
        val tested = WebDIDResolver(http)

        coEvery { http.urlGet(any()) } returns exampleDidDocString

        UniversalDID.registerResolver(tested)

        val responseWeb = UniversalDID.resolve("did:web:example.com")
        assertThat(responseWeb).isEqualTo(exampleDidDoc)

        val responseHttps = UniversalDID.resolve("did:https:example.com")
        assertThat(responseHttps).isEqualTo(exampleDidDoc)
    }

}


