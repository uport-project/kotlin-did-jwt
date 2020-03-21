package me.uport.sdk.jwt

import assertk.all
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import me.uport.sdk.ethrdid.EthrDIDNetwork
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.test.EthrDIDTestHelpers.mockDocForAddress
import me.uport.sdk.testhelpers.TestTimeProvider
import me.uport.sdk.testhelpers.coAssert
import me.uport.sdk.universaldid.DIDResolver
import org.junit.Before
import org.junit.Test

class TimestampTests {

    companion object {
        //mock timestamps
        private const val NOW: Long = 1_500_000L //milliseconds
        private const val PAST: Long = 1_100L //seconds
        private const val FUTURE: Long = 1_900L //seconds
    }

    lateinit var resolver: DIDResolver

    @Before
    fun `mock DID documents before every test`() {
        resolver = mockk<EthrDIDResolver>()
        coEvery {
            resolver.resolve(any())
        } returns mockDocForAddress("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed")
    }

    @Test
    fun `pass when nbf exists in the past`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(NOW))

        val jwt =
            """eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJuYmYiOjExMDAsImlzcyI6IjB4Y2YwM2RkMGE4OTRlZjc5Y2I1YjYwMWE0M2M0YjI1ZTNhZTRjNjdlZCJ9.w14TBtuK2k65tGYk1QmkehfQt3CBwTS0h23yKIliZZNB3wLgidyNpm8hIr04PLv4j-ayDlOLCZL73fGkd6YIJw"""
        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["nbf"]).isEqualTo(PAST)
        assertThat(decoded.containsKey("iat")).isEqualTo(false)

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(
                    EthrDIDNetwork(
                        "rinkeby",
                        "0xregistry",
                        JsonRPC("http://localhost:8545")
                    )
                )
                .build()
        )

        coEvery { resolver.resolve("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed") }.returns(
            mockDocForAddress("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed")
        )

        coAssert {
            tested.verify(jwt, resolver)
        }.isSuccess()
        Unit
    }

    @Test
    fun `pass when nbf exists in the past and iat in the future`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(NOW))

        val jwt =
            """eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJuYmYiOjExMDAsImlhdCI6MTkwMCwiaXNzIjoiMHhjZjAzZGQwYTg5NGVmNzljYjViNjAxYTQzYzRiMjVlM2FlNGM2N2VkIn0.qX5KAuGp7MZzpnH4AIeoy3qy9ndXD-A4F9SwsquYxDP4DKdfD32J1HqRmg55JDnRqAMwKy7OrhXL-RlaXqp7kA"""
        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["nbf"]).isEqualTo(PAST)
        assertThat(decoded["iat"]).isEqualTo(FUTURE)

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(
                    EthrDIDNetwork(
                        "rinkeby",
                        "0xregistry",
                        JsonRPC("http://localhost:8545")
                    )
                )
                .build()
        )

        coEvery { resolver.resolve("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed") }.returns(
            mockDocForAddress("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed")
        )

        coAssert {
            tested.verify(jwt, resolver)
        }.isSuccess()
        Unit
    }

    @Test
    fun `fail when nbf exists in the future`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(NOW))

        val jwt =
            """eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJuYmYiOjE5MDAsImlzcyI6IjB4Y2YwM2RkMGE4OTRlZjc5Y2I1YjYwMWE0M2M0YjI1ZTNhZTRjNjdlZCJ9.4Zk9GBiRuoTYT1PSCh5tlAYgjJmm9Oi-kA3--e2Jkw4MK3jUa09V-mDeIBHVxdmnKJ-F3XP2Rno3_gxcpuAciQ"""
        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["nbf"]).isEqualTo(FUTURE)
        assertThat(decoded.containsKey("iat")).isEqualTo(false)

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coAssert {
            tested.verify(jwt, resolver)
        }.isFailure().all {
            isInstanceOf(InvalidJWTException::class)
            hasMessage("Jwt not valid before nbf: $FUTURE")
        }
    }

    @Test
    fun `fail when nbf exists in the future and iat in the past`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(NOW))

        val jwt =
            """eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJuYmYiOjE5MDAsImlhdCI6MTEwMCwiaXNzIjoiMHhjZjAzZGQwYTg5NGVmNzljYjViNjAxYTQzYzRiMjVlM2FlNGM2N2VkIn0.jzpRnNfKQdamPMSg9Pcz8iz24H9qICMYdIquNb1kFPrE4S5RlX4jWAr5RKgakknmOanSzl2mM5znjjH8hfrj4Q"""
        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded["nbf"]).isEqualTo(FUTURE)
        assertThat(decoded["iat"]).isEqualTo(PAST)

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        coAssert {
            tested.verify(jwt, resolver)
        }.isFailure().all {
            isInstanceOf(InvalidJWTException::class)
            hasMessage("Jwt not valid before nbf: $FUTURE")
        }
    }

    @Test
    fun `pass when nbf missing and iat in the past`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(NOW))
        val jwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJpYXQiOjExMDAsImlzcyI6IjB4Y2YwM2RkMGE4OTRlZjc5Y2I1YjYwMWE0M2M0YjI1ZTNhZTRjNjdlZCJ9.PpTCCQ3goDOvFTZ-owZul6IXfA5Wk2sypnWLgyn2LWrS2Eu2bWBVZ8FIt52AzKoaX1yW79S3WT5ZRjdf4NtCzA"

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("nbf")).isEqualTo(false)
        assertThat(decoded["iat"]).isEqualTo(PAST)

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(
                    EthrDIDNetwork(
                        "rinkeby",
                        "0xregistry",
                        JsonRPC("http://localhost:8545")
                    )
                )
                .build()
        )

        coEvery { resolver.resolve("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed") }.returns(
            mockDocForAddress("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed")
        )

        coAssert {
            tested.verify(jwt, resolver)
        }.isSuccess()
        Unit
    }

    @Test
    fun `fail when nbf missing and iat in the future`() = runBlocking {
        val tested = JWTTools(TestTimeProvider(NOW))
        val jwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJpYXQiOjE5MDAsImlzcyI6IjB4Y2YwM2RkMGE4OTRlZjc5Y2I1YjYwMWE0M2M0YjI1ZTNhZTRjNjdlZCJ9.PJ9wHrlcL_ScDretwS8q5izSmi4CZXRTyXyuhmlbVLGI0v2bioqMYLJZS6aMMEN1A6s7HT10BPFFt8XxLdZuvw"

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                .build()
        )

        val (_, decoded, _) = tested.decodeRaw(jwt)
        assertThat(decoded.containsKey("nbf")).isEqualTo(false)
        assertThat(decoded["iat"]).isEqualTo(FUTURE)

        coAssert {
            tested.verify(jwt, resolver)
        }.isFailure().all {
            isInstanceOf(InvalidJWTException::class)
            hasMessage("Jwt not valid yet (issued in the future) iat: $FUTURE")
        }
    }

    @Test
    fun `pass when nbf and iat both missing`() = runBlocking {

        val resolver = spyk(
            EthrDIDResolver.Builder()
                .addNetwork(
                    EthrDIDNetwork(
                        "rinkeby",
                        "0xregistry",
                        JsonRPC("http://localhost:8545")
                    )
                )
                .build()
        )

        coEvery { resolver.resolve("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed") }.returns(
            mockDocForAddress("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed")
        )

        val jwt =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJpc3MiOiIweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.0z278XpJdjQIgvaqSiJMoqBPBjp5Fy-QqjyT8Sgcbe0KGpCyd7001vLXr09X5aJ5kdcQYnnJ6QFYZeStQWId4w"
        val tested = JWTTools(TestTimeProvider(NOW))
        coAssert {
            tested.verify(jwt, resolver)
        }.isSuccess()
        Unit
    }
}
