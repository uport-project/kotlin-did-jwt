package me.uport.sdk.ethrdid

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.isSuccess
import me.uport.sdk.jsonrpc.JsonRPC
import org.junit.Test

@Suppress("UNUSED_VARIABLE")
class RegistryMapTest {
    private val mainnetLocal =
        EthrDIDNetwork("mainnet", "0x1234", JsonRPC("http://localhost:8545"), chainId = "0x1")
    private val rinkebyLocal =
        EthrDIDNetwork("rinkeby", "0x4321", JsonRPC("http://localhost:8545"), chainId = "0x04")

    @Test
    fun `can get network by direct ID`() {
        val tested = RegistryMap()
            .registerNetwork(mainnetLocal)
            .registerNetwork(rinkebyLocal)

        assertThat(tested["0x1"]).isEqualTo(mainnetLocal)
        assertThat(tested["0x04"]).isEqualTo(rinkebyLocal)
    }

    @Test
    fun `can get network by clean ID`() {
        val tested = RegistryMap()
            .registerNetwork(mainnetLocal)
            .registerNetwork(rinkebyLocal)

        assertThat(tested["0x01"]).isEqualTo(mainnetLocal)
        assertThat(tested["0x4"]).isEqualTo(rinkebyLocal)
    }

    @Test
    fun `can get network by name`() {
        val tested = RegistryMap()
            .registerNetwork(mainnetLocal)
            .registerNetwork(rinkebyLocal)

        assertThat(tested["mainnet"]).isEqualTo(mainnetLocal)
    }

    @Test
    fun `can get empty network name`() {
        val tested = RegistryMap()
            .registerNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("localhost")))

        assertThat {
            val unused = tested[""].registryAddress
        }.isSuccess()
    }

    @Test
    fun `throws when no network found`() {
        val tested = RegistryMap()
            .registerNetwork(mainnetLocal)
            .registerNetwork(rinkebyLocal)

        assertThat {
            val unused = tested["0x6123"]
        }.isFailure().all {
            isInstanceOf(IllegalArgumentException::class)
        }
    }
}
