package me.uport.sdk.ethr_status

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.Test

class ParseRegistryIdTests {

    private val statusRegistry = EthrStatusRegistry()

    @Test
    fun `can parse simple address`() {

        val (address, network) = statusRegistry.parseRegistryId("0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
        assertThat(network).isEqualTo("mainnet")
        assertThat(address).isEqualTo("0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
    }

    @Test
    fun `can parse address with network name`() {

        val (address, network) = statusRegistry.parseRegistryId("rinkeby:0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
        assertThat(network).isEqualTo("rinkeby")
        assertThat(address).isEqualTo("0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
    }

    @Test
    fun `can parse address with chain id`() {

        val (address, network) = statusRegistry.parseRegistryId("0x3:0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
        assertThat(network).isEqualTo("0x3")
        assertThat(address).isEqualTo("0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
    }

    @Test
    fun `can parse address with byte chain id`() {

        val (address, network) = statusRegistry.parseRegistryId("0x2a:0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
        assertThat(network).isEqualTo("0x2a")
        assertThat(address).isEqualTo("0x63142bebe28e663ec4d007cd12ce5e77c37deafc")
    }


    @Test
    fun `throws error for invalid addresses`() {

        val invalidRegistryIDs = listOf(
            "",
            "0x",
            "mainnet:0x",
            "helloworld",
            "0xhelloworld"
        )
        invalidRegistryIDs.forEach {
            assertThat {
                statusRegistry.parseRegistryId(it)
            }.thrownError {
                isInstanceOf(IllegalStateException::class)
            }
        }
    }
}