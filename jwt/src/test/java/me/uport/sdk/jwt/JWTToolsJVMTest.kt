package me.uport.sdk.jwt

import assertk.assertThat
import assertk.assertions.*
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import me.uport.sdk.ethrdid.EthrDIDDocument
import me.uport.sdk.ethrdid.EthrDIDNetwork
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.JWTUtils.Companion.normalizeKnownDID
import me.uport.sdk.jwt.model.JwtPayload
import me.uport.sdk.jwt.test.EthrDIDTestHelpers
import me.uport.sdk.signer.KPSigner
import me.uport.sdk.testhelpers.TestTimeProvider
import me.uport.sdk.testhelpers.coAssert
import me.uport.sdk.universaldid.UniversalDID
import me.uport.sdk.uportdid.UportDIDDocument
import me.uport.sdk.uportdid.UportDIDResolver
import org.junit.Test

class JWTToolsJVMTest {

    private val tokens = listOf(
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1MzUwMTY3MDIsImV4cCI6MTUzNTEwMzEwMiwiYXVkIjoiZGlkOmV0aHI6MHhhOWUzMjMyYjYxYmRiNjcyNzEyYjlhZTMzMTk1MDY5ZDhkNjUxYzFhIiwidHlwZSI6InNoYXJlUmVzcCIsIm5hZCI6IjJvZHpqVGFpOFJvNFYzS3hrbTNTblppdjlXU1l1Tm9aNEFoIiwib3duIjp7Im5hbWUiOiJ1UG9ydCBVc2VyIn0sInJlcSI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzdFVpSjkuZXlKcFlYUWlPakUxTXpVd01UWTJPREVzSW1WNGNDSTZNVFV6TlRBeE56STRNU3dpY21WeGRXVnpkR1ZrSWpwYkltNWhiV1VpTENKd2FHOXVaU0lzSW1OdmRXNTBjbmtpWFN3aWNHVnliV2x6YzJsdmJuTWlPbHNpYm05MGFXWnBZMkYwYVc5dWN5SmRMQ0pqWVd4c1ltRmpheUk2SW1oMGRIQnpPaTh2WTJoaGMzRjFhUzUxY0c5eWRDNXRaUzloY0drdmRqRXZkRzl3YVdNdmJVVXpTbVpXZWxOMFNuUnFhbnBvWWpSYVRFRnhkeUlzSW1GamRDSTZJbXRsZVhCaGFYSWlMQ0owZVhCbElqb2ljMmhoY21WU1pYRWlMQ0pwYzNNaU9pSmthV1E2WlhSb2Nqb3dlR0U1WlRNeU16SmlOakZpWkdJMk56STNNVEppT1dGbE16TXhPVFV3Tmpsa09HUTJOVEZqTVdFaWZRLnVScUdGd01XNnpWSDR4OWFmTDAtS29qSEYwVF9GbW9QWnR6OG5uSjRFXzhNY2cxejBBZ21aMnplOE5iS05wVUNnRHRwTU9RNzVGSjU4WmhzbWFxQUxBRSIsImNhcGFiaWxpdGllcyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5rc3RVaUo5LmV5SnBZWFFpT2pFMU16VXdNVFkzTURFc0ltVjRjQ0k2TVRVek5qTXhNamN3TVN3aVlYVmtJam9pWkdsa09tVjBhSEk2TUhoaE9XVXpNak15WWpZeFltUmlOamN5TnpFeVlqbGhaVE16TVRrMU1EWTVaRGhrTmpVeFl6RmhJaXdpZEhsd1pTSTZJbTV2ZEdsbWFXTmhkR2x2Ym5NaUxDSjJZV3gxWlNJNkltRnlianBoZDNNNmMyNXpPblZ6TFhkbGMzUXRNam94TVRNeE9UWXlNVFkxTlRnNlpXNWtjRzlwYm5RdlIwTk5MM1ZRYjNKMEx6UXpNRGsxTWpZMkxUSmhPR1F0TTJFMFpTMWlaRFV3TFRka01USm1ZVE00TWpRNFlpSXNJbWx6Y3lJNkltUnBaRHBsZEdoeU9qQjRNVEE0TWpBNVpqUXlORGRpTjJabE5qWXdOV0l3WmpVNFpqa3hORFZsWXpNeU5qbGtNREUxTkNKOS5Lc0F6TmVDeHFDaF9rMkt4aTYtWHFveFNXZjBCLWFFR0xXdi1ldHVXQlF2QU5neDFTMG5oZ0ppRkllUnRXakw4ekdnVVV3MUlsSWJtYUZrOEo5aGdhd0UiXSwiYm94UHViIjoiY2g3aGI2S3hsakJ2bXh5UDJXZENWTFNTLzQ2S1hCcmdkWG1Mcm03VEpIST0iLCJpc3MiOiJkaWQ6ZXRocjoweDEwODIwOWY0MjQ3YjdmZTY2MDViMGY1OGY5MTQ1ZWMzMjY5ZDAxNTQifQ.Ncf8B_y0Ha8gdaYyCaL5jLX2RsKTMwxTQ8KlybXFygsxKUUQm9OXo4lU65fduIaFvVyPOP6Oe2adar8m0m2aiwA",
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1MzUwMTY1OTcsImV4cCI6MTUzNTEwMjk5NywiYXVkIjoiZGlkOmV0aHI6MHhhOWUzMjMyYjYxYmRiNjcyNzEyYjlhZTMzMTk1MDY5ZDhkNjUxYzFhIiwidHlwZSI6InNoYXJlUmVzcCIsIm5hZCI6IjJvd2hGdGRtc0VVNVVWMVNCbld0RnZZcHlUcjNqNHd5TmR2Iiwib3duIjp7Im5hbWUiOiJ1UG9ydCBVc2VyIn0sInJlcSI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzdFVpSjkuZXlKcFlYUWlPakUxTXpVd01UWTFPRGdzSW1WNGNDSTZNVFV6TlRBeE56RTRPQ3dpY21WeGRXVnpkR1ZrSWpwYkltNWhiV1VpTENKd2FHOXVaU0lzSW1OdmRXNTBjbmtpWFN3aWNHVnliV2x6YzJsdmJuTWlPbHNpYm05MGFXWnBZMkYwYVc5dWN5SmRMQ0pqWVd4c1ltRmpheUk2SW1oMGRIQnpPaTh2WTJoaGMzRjFhUzUxY0c5eWRDNXRaUzloY0drdmRqRXZkRzl3YVdNdldtMXNOa2xuUWsxYU9XUXhWbGgwV0ZsYVJUTlBkeUlzSW1GamRDSTZJbXRsZVhCaGFYSWlMQ0owZVhCbElqb2ljMmhoY21WU1pYRWlMQ0pwYzNNaU9pSmthV1E2WlhSb2Nqb3dlR0U1WlRNeU16SmlOakZpWkdJMk56STNNVEppT1dGbE16TXhPVFV3Tmpsa09HUTJOVEZqTVdFaWZRLnRNbWh6cjFkbER0YUhUeWUtVDAxOGp2N0NUYlRhVWY4ZzlhbHNxVWJ6VGpGUkFsbV9qZ2RaR3pVVEVzeGtBY0ZmVy1ZSmpIVGtwSmtjNFNWREc3REJBQSIsImlzcyI6ImRpZDpldGhyOjB4ZThjOTFiZGU3NjI1YWIyYzBlZDlmMjE0ZGViMzk0NDBkYTdlMDNjNCJ9.-04Z_m2kgFBwF1Elh3jmv1_44jdGjEczf4x3c5Z4TxwiMP8nXZsIDVgsp3PS34DPGfpR4OkZ6LBozBBER3TABAA"
    )

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `verifies simple tokens (deprecated)`() = runBlocking {
        mockkObject(UniversalDID)

        coEvery { UniversalDID.resolve("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154") }.returns(
            EthrDIDTestHelpers.mockDocForAddress("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154")
        )
        coEvery { UniversalDID.resolve("did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4") }.returns(
            EthrDIDTestHelpers.mockDocForAddress("did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4")
        )

        tokens.forEach { token ->
            val payload = JWTTools(TestTimeProvider(1535102500000L)).verify(
                token = token,
                audience = "did:ethr:0xa9e3232b61bdb672712b9ae33195069d8d651c1a"
            )
            assertThat(payload).isNotNull()
        }
    }

    @Test
    fun `verifies simple tokens`() = runBlocking {

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154") }.returns(
            EthrDIDTestHelpers.mockDocForAddress("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154")
        )
        coEvery { resolver.resolve("did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4") }.returns(
            EthrDIDTestHelpers.mockDocForAddress("did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4")
        )

        tokens.forEach { token ->
            val payload = JWTTools(TestTimeProvider(1535102500000L)).verify(
                token = token,
                resolver = resolver,
                audience = "did:ethr:0xa9e3232b61bdb672712b9ae33195069d8d651c1a"
            )
            assertThat(payload).isNotNull()
        }
    }

    private val validShareReqToken1 =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJpc3MiOiIyb2VYdWZIR0RwVTUxYmZLQnNaRGR1N0plOXdlSjNyN3NWRyIsImlhdCI6MTUyMDM2NjQzMiwicmVxdWVzdGVkIjpbIm5hbWUiLCJwaG9uZSIsImNvdW50cnkiLCJhdmF0YXIiXSwicGVybWlzc2lvbnMiOlsibm90aWZpY2F0aW9ucyJdLCJjYWxsYmFjayI6Imh0dHBzOi8vY2hhc3F1aS51cG9ydC5tZS9hcGkvdjEvdG9waWMvWG5IZnlldjUxeHNka0R0dSIsIm5ldCI6IjB4NCIsImV4cCI6MTUyMDM2NzAzMiwidHlwZSI6InNoYXJlUmVxIn0.C8mPCCtWlYAnroduqysXYRl5xvrOdx1r4iq3A3SmGDGZu47UGTnjiZCOrOQ8A5lZ0M9JfDpZDETCKGdJ7KUeWQ"
    private val expectedShareReqPayload1 = JwtPayload(
        iss = "2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG",
        iat = 1520366432,
        sub = null,
        aud = null,
        exp = 1520367032,
        callback = "https://chasqui.uport.me/api/v1/topic/XnHfyev51xsdkDtu",
        type = "shareReq",
        net = "0x4",
        act = null,
        requested = listOf("name", "phone", "country", "avatar"),
        verified = null,
        permissions = listOf("notifications"),
        req = null,
        nad = null,
        dad = null,
        own = null,
        capabilities = null,
        claims = null,
        ctl = null,
        reg = null,
        rel = null,
        fct = null,
        acc = null
    )

    private val incomingJwt =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJpc3MiOiIyb21SSlpMMjNaQ1lnYzFyWnJGVnBGWEpwV29hRUV1SlVjZiIsImlhdCI6MTUxOTM1MDI1NiwicGVybWlzc2lvbnMiOlsibm90aWZpY2F0aW9ucyJdLCJjYWxsYmFjayI6Imh0dHBzOi8vYXBpLnVwb3J0LnNwYWNlL29sb3J1bi9jcmVhdGVJZGVudGl0eSIsIm5ldCI6IjB4MzAzOSIsImFjdCI6ImRldmljZWtleSIsImV4cCI6MTUyMjU0MDgwMCwidHlwZSI6InNoYXJlUmVxIn0.EkqNUyrZhcDbTQl73XpL2tp470lCo2saMXzuOZ91UI2y-XzpcBMzhhSeUORnoJXJhHnkGGpshZlESWUgrbuiVQ"
    private val expectedJwtPayload = JwtPayload(
        iss = "2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf",
        iat = 1519350256,
        sub = null,
        aud = null,
        exp = 1522540800,
        callback = "https://api.uport.space/olorun/createIdentity",
        type = "shareReq",
        net = "0x3039",
        act = "devicekey",
        requested = null,
        verified = null,
        permissions = listOf("notifications"),
        req = null,
        nad = null,
        dad = null,
        own = null,
        capabilities = null,
        claims = null,
        ctl = null,
        reg = null,
        rel = null,
        fct = null,
        acc = null
    )

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `returns correct payload after successful verification (deprecated)`() = runBlocking {
        mockkObject(UniversalDID)

        coEvery { UniversalDID.resolve("2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG") }.returns(UportDIDDocument.fromJson("""{"id":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG","publicKey":[{"id":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG#keys-1","type":"Secp256k1VerificationKey2018","controller":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG","publicKeyHex":"04171fcc7654cad14745b9835bc534d8e59038ae6929c793d7f8dd2c934580ca39ff1e2de3d7ef69a8daba5e5590d3ec80486a273cbe2bd1b76ebd01f949b41463"}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG#keys-1"}],"service":[],"@context":"https://w3id.org/did/v1","uportProfile":{"@type":"App","image":{"@type":"ImageObject","name":"avatar","contentUrl":"/ipfs/Qmez4bdFmxPknbAoGzHmpjpLjQFChq39h5UMPGiwUHgt8f"},"name":"uPort Demo","description":"Demo App"}}"""))
        coEvery { UniversalDID.resolve("2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf") }.returns(UportDIDDocument.fromJson("""{"id":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf","publicKey":[{"id":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf#keys-1","type":"Secp256k1VerificationKey2018","controller":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf","publicKeyHex":"0422d2edad30d8588c0661e032f37af8177cfa0a056ee9b8ca953b07c6600a637c002ed22fe6612e28e558aee130d399ba97f89e25c16661a99c8af8c832b88568"}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf#keys-1"}],"service":[],"@context":"https://w3id.org/did/v1","uportProfile":{"@type":"App","image":{"@type":"ImageObject","name":"avatar","contentUrl":"/ipfs/QmdznPoNSnc6RawSzzvNFX5HerF62Vu7sbP3vSur9gDFGb"},"name":"Olorun","description":"Private network configuration service"}}"""))

        val shareReqPayload = JWTTools(TestTimeProvider(1520366666000L)).verify(validShareReqToken1)
        assertThat(shareReqPayload).isEqualTo(expectedShareReqPayload1)

        val incomingJwtPayload = JWTTools(TestTimeProvider(1522540300000L)).verify(incomingJwt)
        assertThat(incomingJwtPayload).isEqualTo(expectedJwtPayload)
    }

    @Test
    fun `returns correct payload after successful verification`() = runBlocking {

        val resolver = spyk(UportDIDResolver(JsonRPC("")))

        coEvery {
            resolver.resolve("2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG")
        }.returns(
            UportDIDDocument.fromJson("""{"id":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG","publicKey":[{"id":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG#keys-1","type":"Secp256k1VerificationKey2018","controller":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG","publicKeyHex":"04171fcc7654cad14745b9835bc534d8e59038ae6929c793d7f8dd2c934580ca39ff1e2de3d7ef69a8daba5e5590d3ec80486a273cbe2bd1b76ebd01f949b41463"}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG#keys-1"}],"service":[],"@context":"https://w3id.org/did/v1","uportProfile":{"@type":"App","image":{"@type":"ImageObject","name":"avatar","contentUrl":"/ipfs/Qmez4bdFmxPknbAoGzHmpjpLjQFChq39h5UMPGiwUHgt8f"},"name":"uPort Demo","description":"Demo App"}}""")
        )

        coEvery {
            resolver.resolve("2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf")
        }.returns(
            UportDIDDocument.fromJson("""{"id":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf","publicKey":[{"id":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf#keys-1","type":"Secp256k1VerificationKey2018","controller":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf","publicKeyHex":"0422d2edad30d8588c0661e032f37af8177cfa0a056ee9b8ca953b07c6600a637c002ed22fe6612e28e558aee130d399ba97f89e25c16661a99c8af8c832b88568"}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:uport:2omRJZL23ZCYgc1rZrFVpFXJpWoaEEuJUcf#keys-1"}],"service":[],"@context":"https://w3id.org/did/v1","uportProfile":{"@type":"App","image":{"@type":"ImageObject","name":"avatar","contentUrl":"/ipfs/QmdznPoNSnc6RawSzzvNFX5HerF62Vu7sbP3vSur9gDFGb"},"name":"Olorun","description":"Private network configuration service"}}""")
        )

        val shareReqPayload = JWTTools(TestTimeProvider(1520366666000L)).verify(validShareReqToken1, resolver)
        assertThat(shareReqPayload).isEqualTo(expectedShareReqPayload1)

        val incomingJwtPayload = JWTTools(TestTimeProvider(1522540300000L)).verify(incomingJwt, resolver)
        assertThat(incomingJwtPayload).isEqualTo(expectedJwtPayload)
    }

    @Test
    fun `throws when given an unknown algorithm to create tokens`() {
        val tested = JWTTools()

        val payload = emptyMap<String, Any>()
        val signer = KPSigner("0x1234")
        val issuerDID = "did:ethr:${signer.getAddress()}"

        coAssert {
            tested.createJWT(payload, issuerDID, signer, algorithm = "some fancy but unknown algorithm")
        }.thrownError {
            isInstanceOf(JWTEncodingException::class)
        }
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `throws error when aud is not available (deprecated)`() = runBlocking {
        mockkObject(UniversalDID)

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1MzUwMTY3MDIsImV4cCI6MTUzNTEwMzEwMiwiYXVkIjoiZGlkOmV0aHI6MHhhOWUzMjMyYjYxYmRiNjcyNzEyYjlhZTMzMTk1MDY5ZDhkNjUxYzFhIiwidHlwZSI6InNoYXJlUmVzcCIsIm5hZCI6IjJvZHpqVGFpOFJvNFYzS3hrbTNTblppdjlXU1l1Tm9aNEFoIiwib3duIjp7Im5hbWUiOiJ1UG9ydCBVc2VyIn0sInJlcSI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzdFVpSjkuZXlKcFlYUWlPakUxTXpVd01UWTJPREVzSW1WNGNDSTZNVFV6TlRBeE56STRNU3dpY21WeGRXVnpkR1ZrSWpwYkltNWhiV1VpTENKd2FHOXVaU0lzSW1OdmRXNTBjbmtpWFN3aWNHVnliV2x6YzJsdmJuTWlPbHNpYm05MGFXWnBZMkYwYVc5dWN5SmRMQ0pqWVd4c1ltRmpheUk2SW1oMGRIQnpPaTh2WTJoaGMzRjFhUzUxY0c5eWRDNXRaUzloY0drdmRqRXZkRzl3YVdNdmJVVXpTbVpXZWxOMFNuUnFhbnBvWWpSYVRFRnhkeUlzSW1GamRDSTZJbXRsZVhCaGFYSWlMQ0owZVhCbElqb2ljMmhoY21WU1pYRWlMQ0pwYzNNaU9pSmthV1E2WlhSb2Nqb3dlR0U1WlRNeU16SmlOakZpWkdJMk56STNNVEppT1dGbE16TXhPVFV3Tmpsa09HUTJOVEZqTVdFaWZRLnVScUdGd01XNnpWSDR4OWFmTDAtS29qSEYwVF9GbW9QWnR6OG5uSjRFXzhNY2cxejBBZ21aMnplOE5iS05wVUNnRHRwTU9RNzVGSjU4WmhzbWFxQUxBRSIsImNhcGFiaWxpdGllcyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5rc3RVaUo5LmV5SnBZWFFpT2pFMU16VXdNVFkzTURFc0ltVjRjQ0k2TVRVek5qTXhNamN3TVN3aVlYVmtJam9pWkdsa09tVjBhSEk2TUhoaE9XVXpNak15WWpZeFltUmlOamN5TnpFeVlqbGhaVE16TVRrMU1EWTVaRGhrTmpVeFl6RmhJaXdpZEhsd1pTSTZJbTV2ZEdsbWFXTmhkR2x2Ym5NaUxDSjJZV3gxWlNJNkltRnlianBoZDNNNmMyNXpPblZ6TFhkbGMzUXRNam94TVRNeE9UWXlNVFkxTlRnNlpXNWtjRzlwYm5RdlIwTk5MM1ZRYjNKMEx6UXpNRGsxTWpZMkxUSmhPR1F0TTJFMFpTMWlaRFV3TFRka01USm1ZVE00TWpRNFlpSXNJbWx6Y3lJNkltUnBaRHBsZEdoeU9qQjRNVEE0TWpBNVpqUXlORGRpTjJabE5qWXdOV0l3WmpVNFpqa3hORFZsWXpNeU5qbGtNREUxTkNKOS5Lc0F6TmVDeHFDaF9rMkt4aTYtWHFveFNXZjBCLWFFR0xXdi1ldHVXQlF2QU5neDFTMG5oZ0ppRkllUnRXakw4ekdnVVV3MUlsSWJtYUZrOEo5aGdhd0UiXSwiYm94UHViIjoiY2g3aGI2S3hsakJ2bXh5UDJXZENWTFNTLzQ2S1hCcmdkWG1Mcm03VEpIST0iLCJpc3MiOiJkaWQ6ZXRocjoweDEwODIwOWY0MjQ3YjdmZTY2MDViMGY1OGY5MTQ1ZWMzMjY5ZDAxNTQifQ.Ncf8B_y0Ha8gdaYyCaL5jLX2RsKTMwxTQ8KlybXFygsxKUUQm9OXo4lU65fduIaFvVyPOP6Oe2adar8m0m2aiwA"

        coEvery { UniversalDID.resolve("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154") }.returns(
            EthrDIDTestHelpers.mockDocForAddress("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154")
        )

        coAssert {
            JWTTools(TestTimeProvider(1535102500000L)).verify(
                token = token
            )
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Test
    fun `throws error when aud is not available`() = runBlocking {

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1MzUwMTY3MDIsImV4cCI6MTUzNTEwMzEwMiwiYXVkIjoiZGlkOmV0aHI6MHhhOWUzMjMyYjYxYmRiNjcyNzEyYjlhZTMzMTk1MDY5ZDhkNjUxYzFhIiwidHlwZSI6InNoYXJlUmVzcCIsIm5hZCI6IjJvZHpqVGFpOFJvNFYzS3hrbTNTblppdjlXU1l1Tm9aNEFoIiwib3duIjp7Im5hbWUiOiJ1UG9ydCBVc2VyIn0sInJlcSI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzdFVpSjkuZXlKcFlYUWlPakUxTXpVd01UWTJPREVzSW1WNGNDSTZNVFV6TlRBeE56STRNU3dpY21WeGRXVnpkR1ZrSWpwYkltNWhiV1VpTENKd2FHOXVaU0lzSW1OdmRXNTBjbmtpWFN3aWNHVnliV2x6YzJsdmJuTWlPbHNpYm05MGFXWnBZMkYwYVc5dWN5SmRMQ0pqWVd4c1ltRmpheUk2SW1oMGRIQnpPaTh2WTJoaGMzRjFhUzUxY0c5eWRDNXRaUzloY0drdmRqRXZkRzl3YVdNdmJVVXpTbVpXZWxOMFNuUnFhbnBvWWpSYVRFRnhkeUlzSW1GamRDSTZJbXRsZVhCaGFYSWlMQ0owZVhCbElqb2ljMmhoY21WU1pYRWlMQ0pwYzNNaU9pSmthV1E2WlhSb2Nqb3dlR0U1WlRNeU16SmlOakZpWkdJMk56STNNVEppT1dGbE16TXhPVFV3Tmpsa09HUTJOVEZqTVdFaWZRLnVScUdGd01XNnpWSDR4OWFmTDAtS29qSEYwVF9GbW9QWnR6OG5uSjRFXzhNY2cxejBBZ21aMnplOE5iS05wVUNnRHRwTU9RNzVGSjU4WmhzbWFxQUxBRSIsImNhcGFiaWxpdGllcyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5rc3RVaUo5LmV5SnBZWFFpT2pFMU16VXdNVFkzTURFc0ltVjRjQ0k2TVRVek5qTXhNamN3TVN3aVlYVmtJam9pWkdsa09tVjBhSEk2TUhoaE9XVXpNak15WWpZeFltUmlOamN5TnpFeVlqbGhaVE16TVRrMU1EWTVaRGhrTmpVeFl6RmhJaXdpZEhsd1pTSTZJbTV2ZEdsbWFXTmhkR2x2Ym5NaUxDSjJZV3gxWlNJNkltRnlianBoZDNNNmMyNXpPblZ6TFhkbGMzUXRNam94TVRNeE9UWXlNVFkxTlRnNlpXNWtjRzlwYm5RdlIwTk5MM1ZRYjNKMEx6UXpNRGsxTWpZMkxUSmhPR1F0TTJFMFpTMWlaRFV3TFRka01USm1ZVE00TWpRNFlpSXNJbWx6Y3lJNkltUnBaRHBsZEdoeU9qQjRNVEE0TWpBNVpqUXlORGRpTjJabE5qWXdOV0l3WmpVNFpqa3hORFZsWXpNeU5qbGtNREUxTkNKOS5Lc0F6TmVDeHFDaF9rMkt4aTYtWHFveFNXZjBCLWFFR0xXdi1ldHVXQlF2QU5neDFTMG5oZ0ppRkllUnRXakw4ekdnVVV3MUlsSWJtYUZrOEo5aGdhd0UiXSwiYm94UHViIjoiY2g3aGI2S3hsakJ2bXh5UDJXZENWTFNTLzQ2S1hCcmdkWG1Mcm03VEpIST0iLCJpc3MiOiJkaWQ6ZXRocjoweDEwODIwOWY0MjQ3YjdmZTY2MDViMGY1OGY5MTQ1ZWMzMjY5ZDAxNTQifQ.Ncf8B_y0Ha8gdaYyCaL5jLX2RsKTMwxTQ8KlybXFygsxKUUQm9OXo4lU65fduIaFvVyPOP6Oe2adar8m0m2aiwA"

        coEvery { resolver.resolve("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154") }.returns(
            EthrDIDTestHelpers.mockDocForAddress("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154")
        )

        coAssert {
            JWTTools(TestTimeProvider(1535102500000L)).verify(
                token = token,
                resolver = resolver
            )
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `throws error when aud and payload aud do not match (deprecated)`() = runBlocking {
        mockkObject(UniversalDID)

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1MzUwMTY3MDIsImV4cCI6MTUzNTEwMzEwMiwiYXVkIjoiZGlkOmV0aHI6MHhhOWUzMjMyYjYxYmRiNjcyNzEyYjlhZTMzMTk1MDY5ZDhkNjUxYzFhIiwidHlwZSI6InNoYXJlUmVzcCIsIm5hZCI6IjJvZHpqVGFpOFJvNFYzS3hrbTNTblppdjlXU1l1Tm9aNEFoIiwib3duIjp7Im5hbWUiOiJ1UG9ydCBVc2VyIn0sInJlcSI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzdFVpSjkuZXlKcFlYUWlPakUxTXpVd01UWTJPREVzSW1WNGNDSTZNVFV6TlRBeE56STRNU3dpY21WeGRXVnpkR1ZrSWpwYkltNWhiV1VpTENKd2FHOXVaU0lzSW1OdmRXNTBjbmtpWFN3aWNHVnliV2x6YzJsdmJuTWlPbHNpYm05MGFXWnBZMkYwYVc5dWN5SmRMQ0pqWVd4c1ltRmpheUk2SW1oMGRIQnpPaTh2WTJoaGMzRjFhUzUxY0c5eWRDNXRaUzloY0drdmRqRXZkRzl3YVdNdmJVVXpTbVpXZWxOMFNuUnFhbnBvWWpSYVRFRnhkeUlzSW1GamRDSTZJbXRsZVhCaGFYSWlMQ0owZVhCbElqb2ljMmhoY21WU1pYRWlMQ0pwYzNNaU9pSmthV1E2WlhSb2Nqb3dlR0U1WlRNeU16SmlOakZpWkdJMk56STNNVEppT1dGbE16TXhPVFV3Tmpsa09HUTJOVEZqTVdFaWZRLnVScUdGd01XNnpWSDR4OWFmTDAtS29qSEYwVF9GbW9QWnR6OG5uSjRFXzhNY2cxejBBZ21aMnplOE5iS05wVUNnRHRwTU9RNzVGSjU4WmhzbWFxQUxBRSIsImNhcGFiaWxpdGllcyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5rc3RVaUo5LmV5SnBZWFFpT2pFMU16VXdNVFkzTURFc0ltVjRjQ0k2TVRVek5qTXhNamN3TVN3aVlYVmtJam9pWkdsa09tVjBhSEk2TUhoaE9XVXpNak15WWpZeFltUmlOamN5TnpFeVlqbGhaVE16TVRrMU1EWTVaRGhrTmpVeFl6RmhJaXdpZEhsd1pTSTZJbTV2ZEdsbWFXTmhkR2x2Ym5NaUxDSjJZV3gxWlNJNkltRnlianBoZDNNNmMyNXpPblZ6TFhkbGMzUXRNam94TVRNeE9UWXlNVFkxTlRnNlpXNWtjRzlwYm5RdlIwTk5MM1ZRYjNKMEx6UXpNRGsxTWpZMkxUSmhPR1F0TTJFMFpTMWlaRFV3TFRka01USm1ZVE00TWpRNFlpSXNJbWx6Y3lJNkltUnBaRHBsZEdoeU9qQjRNVEE0TWpBNVpqUXlORGRpTjJabE5qWXdOV0l3WmpVNFpqa3hORFZsWXpNeU5qbGtNREUxTkNKOS5Lc0F6TmVDeHFDaF9rMkt4aTYtWHFveFNXZjBCLWFFR0xXdi1ldHVXQlF2QU5neDFTMG5oZ0ppRkllUnRXakw4ekdnVVV3MUlsSWJtYUZrOEo5aGdhd0UiXSwiYm94UHViIjoiY2g3aGI2S3hsakJ2bXh5UDJXZENWTFNTLzQ2S1hCcmdkWG1Mcm03VEpIST0iLCJpc3MiOiJkaWQ6ZXRocjoweDEwODIwOWY0MjQ3YjdmZTY2MDViMGY1OGY5MTQ1ZWMzMjY5ZDAxNTQifQ.Ncf8B_y0Ha8gdaYyCaL5jLX2RsKTMwxTQ8KlybXFygsxKUUQm9OXo4lU65fduIaFvVyPOP6Oe2adar8m0m2aiwA"

        coEvery { UniversalDID.resolve("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154") }.returns(
            EthrDIDDocument.fromJson(
                """{"id":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154","publicKey":[{"id":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#owner","type":"Secp256k1VerificationKey2018","controller":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154","ethereumAddress":"0x108209f4247b7fe6605b0f58f9145ec3269d0154","publicKeyHex":null,"publicKeyBase64":null,"publicKeyBase58":null,"value":null}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#owner"}],"service":[],"@context":"https://w3id.org/did/v1"}"""
            )
        )

        coAssert {
            JWTTools(TestTimeProvider(1535102500000L)).verify(
                token = token,
                audience = "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154"
            )
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Test
    fun `throws error when aud and payload aud do not match`() = runBlocking {

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1MzUwMTY3MDIsImV4cCI6MTUzNTEwMzEwMiwiYXVkIjoiZGlkOmV0aHI6MHhhOWUzMjMyYjYxYmRiNjcyNzEyYjlhZTMzMTk1MDY5ZDhkNjUxYzFhIiwidHlwZSI6InNoYXJlUmVzcCIsIm5hZCI6IjJvZHpqVGFpOFJvNFYzS3hrbTNTblppdjlXU1l1Tm9aNEFoIiwib3duIjp7Im5hbWUiOiJ1UG9ydCBVc2VyIn0sInJlcSI6ImV5SjBlWEFpT2lKS1YxUWlMQ0poYkdjaU9pSkZVekkxTmtzdFVpSjkuZXlKcFlYUWlPakUxTXpVd01UWTJPREVzSW1WNGNDSTZNVFV6TlRBeE56STRNU3dpY21WeGRXVnpkR1ZrSWpwYkltNWhiV1VpTENKd2FHOXVaU0lzSW1OdmRXNTBjbmtpWFN3aWNHVnliV2x6YzJsdmJuTWlPbHNpYm05MGFXWnBZMkYwYVc5dWN5SmRMQ0pqWVd4c1ltRmpheUk2SW1oMGRIQnpPaTh2WTJoaGMzRjFhUzUxY0c5eWRDNXRaUzloY0drdmRqRXZkRzl3YVdNdmJVVXpTbVpXZWxOMFNuUnFhbnBvWWpSYVRFRnhkeUlzSW1GamRDSTZJbXRsZVhCaGFYSWlMQ0owZVhCbElqb2ljMmhoY21WU1pYRWlMQ0pwYzNNaU9pSmthV1E2WlhSb2Nqb3dlR0U1WlRNeU16SmlOakZpWkdJMk56STNNVEppT1dGbE16TXhPVFV3Tmpsa09HUTJOVEZqTVdFaWZRLnVScUdGd01XNnpWSDR4OWFmTDAtS29qSEYwVF9GbW9QWnR6OG5uSjRFXzhNY2cxejBBZ21aMnplOE5iS05wVUNnRHRwTU9RNzVGSjU4WmhzbWFxQUxBRSIsImNhcGFiaWxpdGllcyI6WyJleUowZVhBaU9pSktWMVFpTENKaGJHY2lPaUpGVXpJMU5rc3RVaUo5LmV5SnBZWFFpT2pFMU16VXdNVFkzTURFc0ltVjRjQ0k2TVRVek5qTXhNamN3TVN3aVlYVmtJam9pWkdsa09tVjBhSEk2TUhoaE9XVXpNak15WWpZeFltUmlOamN5TnpFeVlqbGhaVE16TVRrMU1EWTVaRGhrTmpVeFl6RmhJaXdpZEhsd1pTSTZJbTV2ZEdsbWFXTmhkR2x2Ym5NaUxDSjJZV3gxWlNJNkltRnlianBoZDNNNmMyNXpPblZ6TFhkbGMzUXRNam94TVRNeE9UWXlNVFkxTlRnNlpXNWtjRzlwYm5RdlIwTk5MM1ZRYjNKMEx6UXpNRGsxTWpZMkxUSmhPR1F0TTJFMFpTMWlaRFV3TFRka01USm1ZVE00TWpRNFlpSXNJbWx6Y3lJNkltUnBaRHBsZEdoeU9qQjRNVEE0TWpBNVpqUXlORGRpTjJabE5qWXdOV0l3WmpVNFpqa3hORFZsWXpNeU5qbGtNREUxTkNKOS5Lc0F6TmVDeHFDaF9rMkt4aTYtWHFveFNXZjBCLWFFR0xXdi1ldHVXQlF2QU5neDFTMG5oZ0ppRkllUnRXakw4ekdnVVV3MUlsSWJtYUZrOEo5aGdhd0UiXSwiYm94UHViIjoiY2g3aGI2S3hsakJ2bXh5UDJXZENWTFNTLzQ2S1hCcmdkWG1Mcm03VEpIST0iLCJpc3MiOiJkaWQ6ZXRocjoweDEwODIwOWY0MjQ3YjdmZTY2MDViMGY1OGY5MTQ1ZWMzMjY5ZDAxNTQifQ.Ncf8B_y0Ha8gdaYyCaL5jLX2RsKTMwxTQ8KlybXFygsxKUUQm9OXo4lU65fduIaFvVyPOP6Oe2adar8m0m2aiwA"

        coEvery { resolver.resolve("did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154") }.returns(
            EthrDIDDocument.fromJson(
                """{"id":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154","publicKey":[{"id":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#owner","type":"Secp256k1VerificationKey2018","controller":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154","ethereumAddress":"0x108209f4247b7fe6605b0f58f9145ec3269d0154","publicKeyHex":null,"publicKeyBase64":null,"publicKeyBase58":null,"value":null}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154#owner"}],"service":[],"@context":"https://w3id.org/did/v1"}"""
            )
        )

        coAssert {
            JWTTools(TestTimeProvider(1535102500000L)).verify(
                token = token,
                resolver = resolver,
                audience = "did:ethr:0x108209f4247b7fe6605b0f58f9145ec3269d0154"
            )
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `throws when a token is issued in the future (deprecated)`() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkaWQ6ZXRocjoweGE5ZTMyMzJiNjFiZGI2NzI3MTJiOWFlMzMxOTUwNjlkOGQ2NTFjMWEiLCJpYXQiOjE1NDU1Njk1NDEsImV4cCI6MTU0NjA4Nzk0MSwiYXVkIjoiZGlkOmV0aHI6MHgxMDgyMDlmNDI0N2I3ZmU2NjA1YjBmNThmOTE0NWVjMzI2OWQwMTU0Iiwic3ViIjoiIn0.Bt9Frc1QabJfpXYBoU4sns8WPeRLdKU87FncgMFq1lY"

        coAssert {
            JWTTools(TestTimeProvider(977317692000L)).verify(token)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Test
    fun `throws when a token is issued in the future`() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkaWQ6ZXRocjoweGE5ZTMyMzJiNjFiZGI2NzI3MTJiOWFlMzMxOTUwNjlkOGQ2NTFjMWEiLCJpYXQiOjE1NDU1Njk1NDEsImV4cCI6MTU0NjA4Nzk0MSwiYXVkIjoiZGlkOmV0aHI6MHgxMDgyMDlmNDI0N2I3ZmU2NjA1YjBmNThmOTE0NWVjMzI2OWQwMTU0Iiwic3ViIjoiIn0.Bt9Frc1QabJfpXYBoU4sns8WPeRLdKU87FncgMFq1lY"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coAssert {
            JWTTools(TestTimeProvider(977317692000L)).verify(token, resolver)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `throws when a token is expired (deprecated)`() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkaWQ6ZXRocjoweGE5ZTMyMzJiNjFiZGI2NzI3MTJiOWFlMzMxOTUwNjlkOGQ2NTFjMWEiLCJpYXQiOjE1NDU1Njk1NDEsImV4cCI6MTU0NjA4Nzk0MSwiYXVkIjoiZGlkOmV0aHI6MHgxMDgyMDlmNDI0N2I3ZmU2NjA1YjBmNThmOTE0NWVjMzI2OWQwMTU0Iiwic3ViIjoiIn0.Bt9Frc1QabJfpXYBoU4sns8WPeRLdKU87FncgMFq1lY"

        coAssert {
            JWTTools(TestTimeProvider(1576847292000L)).verify(token)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Test
    fun `throws when a token is expired`() {
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJkaWQ6ZXRocjoweGE5ZTMyMzJiNjFiZGI2NzI3MTJiOWFlMzMxOTUwNjlkOGQ2NTFjMWEiLCJpYXQiOjE1NDU1Njk1NDEsImV4cCI6MTU0NjA4Nzk0MSwiYXVkIjoiZGlkOmV0aHI6MHgxMDgyMDlmNDI0N2I3ZmU2NjA1YjBmNThmOTE0NWVjMzI2OWQwMTU0Iiwic3ViIjoiIn0.Bt9Frc1QabJfpXYBoU4sns8WPeRLdKU87FncgMFq1lY"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coAssert {
            JWTTools(TestTimeProvider(1576847292000L)).verify(token, resolver)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `throws when the signature does not match any public keys belonging to the issuer (deprecated)`() {

        mockkObject(UniversalDID)
        coEvery { UniversalDID.resolve("did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9") }.returns(
            EthrDIDDocument.fromJson(
                """{"id":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9","publicKey":[{"id":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9#owner","type":"Secp256k1VerificationKey2018","controller":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9","ethereumAddress":"0x6985a110df37555235d7d0de0a0fb28c9848dfa9","publicKeyHex":null,"publicKeyBase64":null,"publicKeyBase58":null,"value":null}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9#owner"}],"service":[],"@context":"https://w3id.org/did/v1"}"""
            )
        )

        // JWT token with a Signer that doesn't have anything to do with the issuerDID.
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NDY4NTkxNTksImV4cCI6MTg2MjIxOTE1OSwiaXNzIjoiZGlkOmV0aHI6MHg2OTg1YTExMGRmMzc1NTUyMzVkN2QwZGUwYTBmYjI4Yzk4NDhkZmE5In0.fe1rvAHsoJsJzwSFAmVFTz9uxhncNY65jpbb2cS9jcY08xphpU3rOy1N85_IbEjhIZw-FrPeFgxJLoDLw6itcgE"

        coAssert {
            JWTTools(TestTimeProvider(1547818630000L)).verify(token)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }

    @Test
    fun `throws when the signature does not match any public keys belonging to the issuer`() {

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve("did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9") }.returns(
            EthrDIDDocument.fromJson(
                """{"id":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9","publicKey":[{"id":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9#owner","type":"Secp256k1VerificationKey2018","controller":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9","ethereumAddress":"0x6985a110df37555235d7d0de0a0fb28c9848dfa9","publicKeyHex":null,"publicKeyBase64":null,"publicKeyBase58":null,"value":null}],"authentication":[{"type":"Secp256k1SignatureAuthentication2018","publicKey":"did:ethr:0x6985a110df37555235d7d0de0a0fb28c9848dfa9#owner"}],"service":[],"@context":"https://w3id.org/did/v1"}"""
            )
        )

        // JWT token with a Signer that doesn't have anything to do with the issuerDID.
        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NDY4NTkxNTksImV4cCI6MTg2MjIxOTE1OSwiaXNzIjoiZGlkOmV0aHI6MHg2OTg1YTExMGRmMzc1NTUyMzVkN2QwZGUwYTBmYjI4Yzk4NDhkZmE5In0.fe1rvAHsoJsJzwSFAmVFTz9uxhncNY65jpbb2cS9jcY08xphpU3rOy1N85_IbEjhIZw-FrPeFgxJLoDLw6itcgE"

        coAssert {
            JWTTools(TestTimeProvider(1547818630000L)).verify(token, resolver)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }


    @Test
    fun `finds public key`() = runBlocking {

        val alg = "ES256K"
        val issuer = "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val auth = false
        val doc = EthrDIDDocument.fromJson(
            """
                    {
                        "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                        "publicKey": [{
                            "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-1",
                            "type": "Secp256k1VerificationKey2018",
                            "controller": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                            "publicKeyHex": "04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab061"
                        }],
                        "authentication": [],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }
                    """
        )

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve(issuer) }.returns(doc)

        val authenticators = JWTTools().resolveAuthenticator(alg, issuer, auth, resolver)
        assertThat(authenticators).isEqualTo(doc.publicKey)
    }


    @Test
    fun `only list authenticators able to authenticate a user`() = runBlocking {

        val alg = "ES256K"
        val issuer = "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val auth = true
        val doc = EthrDIDDocument.fromJson(
            """
            {
                "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                "publicKey": [{
                    "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-1",
                    "type": "Secp256k1VerificationKey2018",
                    "controller": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                    "publicKeyHex": "04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab061"
                }, {
                    "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-2",
                    "type": "Secp256k1SignatureVerificationKey2018",
                    "controller": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                    "publicKeyHex": "04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab061"
                }, {
                    "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-3",
                    "type": "Secp256k1SignatureAuthentication2018",
                    "controller": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                    "publicKeyHex": "04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab061"
                }, {
                    "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-3",
                    "type": "Curve25519EncryptionPublicKey",
                    "controller": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                    "publicKeyHex": "04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab061"
                }],
                "authentication": [{
                    "type": "Secp256k1VerificationKey2018",
                    "publicKey": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-1"
                }, {
                    "type": "Secp256k1SignatureVerificationKey2018",
                    "publicKey": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-2"
                }],
                "service": [],
                "@context": "https://w3id.org/did/v1"
            }
            """
        )

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve(issuer) }.returns(doc)

        val authenticators = JWTTools().resolveAuthenticator(alg, issuer, auth, resolver)

        assertThat(authenticators).isEqualTo(listOf(doc.publicKey[0], doc.publicKey[1]))
    }


    @Test
    fun `errors if no suitable public keys exist for authentication`() = runBlocking {

        val alg = "ES256K"
        val issuer = "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val auth = true
        val doc = EthrDIDDocument.fromJson(
            """
                    {
                        "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                        "publicKey": [{
                            "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4#keys-1",
                            "type": "Secp256k1VerificationKey2018",
                            "controller": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                            "publicKeyHex": "04613bb3a4874d27032618f020614c21cbe4c4e4781687525f6674089f9bd3d6c7f6eb13569053d31715a3ba32e0b791b97922af6387f087d6b5548c06944ab061"
                        }],
                        "authentication": [],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }
                    """
        )

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(
                    EthrDIDNetwork(
                        "",
                        "",
                        JsonRPC("")
                    )
                )
                .build()
        )

        coEvery { resolver.resolve(issuer) }.returns(doc)

        coAssert {
            JWTTools().resolveAuthenticator(alg, issuer, auth, resolver)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }


    @Test
    fun `errors if no public keys exist`() = runBlocking {

        val alg = "ES256K"
        val issuer = "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val auth = false
        val doc = EthrDIDDocument.fromJson(
            """
                    {
                        "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                        "publicKey": [],
                        "authentication": [],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }
                    """
        )

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(
                    EthrDIDNetwork(
                        "",
                        "",
                        JsonRPC("")
                    )
                )
                .build()
        )

        coEvery { resolver.resolve(issuer) }.returns(doc)

        coAssert {
            JWTTools().resolveAuthenticator(alg, issuer, auth, resolver)
        }.thrownError {
            isInstanceOf(InvalidJWTException::class)
        }
    }


    @Test
    fun `errors if no supported signature types exist`() = runBlocking {

        val alg = "ESBAD"
        val issuer = "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val auth = false
        val doc = EthrDIDDocument.fromJson(
            """
                    {
                        "id": "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4",
                        "publicKey": [],
                        "authentication": [],
                        "service": [],
                        "@context": "https://w3id.org/did/v1"
                    }
                    """
        )

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { UniversalDID.resolve(issuer) }.returns(doc)

        coAssert {
            JWTTools().resolveAuthenticator(alg, issuer, auth, resolver)
        }.thrownError {
            isInstanceOf(JWTEncodingException::class)
        }
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
            "This will be removed in the next major release.")
    @Test
    fun `can verify a freshly minted token (deprecated)`() = runBlocking {
        val signer = KPSigner("0x1234")
        val did = "did:ethr:${signer.getAddress()}"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve(eq(did)) } returns
                EthrDIDTestHelpers.mockDocForAddress(signer.getAddress())

        UniversalDID.registerResolver(resolver)

        val token = JWTTools().createJWT(emptyMap(), did, signer)
        val payload = JWTTools().verify(token)
        assertThat(payload).isNotNull()
        Unit
    }

    @Test
    fun `can verify a freshly minted token`() = runBlocking {
        val signer = KPSigner("0x1234")
        val did = "did:ethr:${signer.getAddress()}"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve(eq(did)) } returns
                EthrDIDTestHelpers.mockDocForAddress(signer.getAddress())

        val token = JWTTools().createJWT(emptyMap(), did, signer)
        val payload = JWTTools().verify(token, resolver)
        assertThat(payload).isNotNull()
        Unit
    }

    @Deprecated("This test references the deprecated variant of JWTTools().verify()" +
                            "This will be removed in the next major release.")
    @Test
    fun `can verify a ES256K signature with only ethereumAddress in the DID doc (deprecated)`() = runBlocking {
        val address = "0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve(eq("did:ethr:$address")) } returns
                EthrDIDTestHelpers.mockDocForAddress(address)

        UniversalDID.registerResolver(resolver)

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJoZWxsbyI6IndvcmxkIiwiaWF0IjoxNTYxOTcxMTE5LCJpc3MiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.t5o1vzZExArlrrTVHmwtti7fnicXqvWrX6SS3F-Lu3budH7p6zQHjG8X7EvUTRUxhvr-eENCbXeteSE4rgF7MA"
        val payload = JWTTools().verify(token)
        assertThat(payload.iss).isEqualTo("did:ethr:$address")
    }

    @Test
    fun `can verify a ES256K signature with only ethereumAddress in the DID doc`() = runBlocking {
        val address = "0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coEvery { resolver.resolve(eq("did:ethr:$address")) } returns
                EthrDIDTestHelpers.mockDocForAddress(address)

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJoZWxsbyI6IndvcmxkIiwiaWF0IjoxNTYxOTcxMTE5LCJpc3MiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.t5o1vzZExArlrrTVHmwtti7fnicXqvWrX6SS3F-Lu3budH7p6zQHjG8X7EvUTRUxhvr-eENCbXeteSE4rgF7MA"
        val payload = JWTTools().verify(token, resolver)
        assertThat(payload.iss).isEqualTo("did:ethr:$address")
    }

    @Test
    fun `can create token from map of claims`() = runBlocking {

        val tested = JWTTools(TestTimeProvider(12345678000L))

        val payload = mapOf(
            "claims" to mapOf("name" to "R Daneel Olivaw")
        )

        val signer = KPSigner("0x54ece214d38fe6b46110a21c69fd55230f09688bf85b95fc7c1e4e160441ece1")
        val issuerDID = "did:ethr:${signer.getAddress()}"

        val jwt = tested.createJWT(payload, issuerDID, signer)
        assertThat(jwt)
            .isEqualTo("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJjbGFpbXMiOnsibmFtZSI6IlIgRGFuZWVsIE9saXZhdyJ9LCJpYXQiOjEyMzQ1Njc4LCJleHAiOjEyMzQ1OTc4LCJpc3MiOiJkaWQ6ZXRocjoweDQxMjNjYmQxNDNiNTVjMDZlNDUxZmYyNTNhZjA5Mjg2YjY4N2E5NTAifQ.o6eDKYjHJnak1ylkpe9g8krxvK9UEhKf-1T0EYhH8pGyb8MjOEepRJi8DYlVEnZno0DkVYXQCf3u1i_HThBKtAA")
    }

    @Test
    fun `can normalize dids`() {
        val uportDID = "did:uport:2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG"
        val mnid = "2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG"
        val ethrDID = "did:ethr:0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val ethrAddress = "0xe8c91bde7625ab2c0ed9f214deb39440da7e03c4"
        val invalidDID = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9"

        assertThat(normalizeKnownDID(uportDID)).isEqualTo(uportDID)
        assertThat(normalizeKnownDID(mnid)).isEqualTo(uportDID)
        assertThat(normalizeKnownDID(ethrDID)).isEqualTo(ethrDID)
        assertThat(normalizeKnownDID(ethrAddress)).isEqualTo(ethrDID)
        assertThat(normalizeKnownDID(invalidDID)).isEqualTo(invalidDID)
    }

    @Test
    fun `ignores expiredInSeconds param when exp is already set in payload`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("exp" to 432101234L)
        val signer = KPSigner("0x1234")
        val issuerDID = "did:ethr:${signer.getAddress()}"

        val jwt = tested.createJWT(payload, issuerDID, signer, expiresInSeconds = 1234L)

        val (_, dec, _) = tested.decode(jwt)
        assertThat(dec.exp).isEqualTo(432101234L)
    }

    @Test
    fun `sets exp based on expiresInSeconds`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(1111000L))

        val payload = mapOf("hello" to "world")
        val signer = KPSigner("0x1234")
        val issuerDID = "did:ethr:${signer.getAddress()}"

        val jwt = tested.createJWT(payload, issuerDID, signer, expiresInSeconds = 42L)

        val (_, dec, _) = tested.decode(jwt)
        assertThat(dec.exp).isEqualTo(1153L)
    }

    @Test
    fun `removes exp when payload sets it to null`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("exp" to null)
        val signer = KPSigner("0x1234")
        val issuerDID = "did:ethr:${signer.getAddress()}"

        val jwt = tested.createJWT(payload, issuerDID, signer)

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("exp")).isFalse()
    }

    @Test
    fun `removes exp when expiry interval is negative`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("exp" to 1234)
        val signer = KPSigner("0x1234")
        val issuerDID = "did:ethr:${signer.getAddress()}"

        val jwt = tested.createJWT(payload, issuerDID, signer, expiresInSeconds = -1)

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("exp")).isFalse()
    }

    @Test
    fun `removes iat when explicitly set as null`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("iat" to null)

        val jwt = tested.createJWT(payload, "did:ex:ex", KPSigner("0x1234"))

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("iat")).isFalse()
    }

    @Test
    fun `iat set by default when not specified`() = runBlocking {
        val tested = JWTTools()

        val jwt = tested.createJWT(emptyMap(), "did:ex:ex", KPSigner("0x1234"))

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("iat")).isTrue()
    }

    @Test
    fun `default iat is overridden by payload`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("iat" to 5678L)
        val jwt = tested.createJWT(payload, "did:ex:ex", KPSigner("0x1234"))

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["iat"]).isEqualTo(5678L)
    }

    @Test
    fun `default iss is overridden by payload`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("iss" to "example.issuer")
        val jwt = tested.createJWT(payload, "did:ex:ex", KPSigner("0x1234"))

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["iss"]).isEqualTo("example.issuer")
    }

    @Test
    fun `default iss is removed by payload`() = runBlocking {
        val tested = JWTTools()

        val payload = mapOf("iss" to null)
        val jwt = tested.createJWT(payload, "did:ex:ex", KPSigner("0x1234"))

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("iss")).isFalse()
    }

    @Test
    fun `iss is set by default`() = runBlocking {
        val tested = JWTTools()

        val jwt = tested.createJWT(emptyMap(), "did:ex:ex", KPSigner("0x1234"))

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["iss"]).isEqualTo("did:ex:ex")
    }
}


