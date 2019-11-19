package me.uport.sdk.uportdefaults

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import me.uport.sdk.core.Networks
import me.uport.sdk.ethrdid.EthrDIDNetwork
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.httpsdid.WebDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.universaldid.DIDResolver
import me.uport.sdk.uportdid.UportDIDResolver
import org.junit.Test


class DIDResolverTest {


    @Test
    fun `can resolve valid dids after adding resolvers`() {
        val resolver = DIDResolver.Builder()
            .addResolver(
                EthrDIDResolver.Builder()
                    .addNetwork(EthrDIDNetwork("", "0xregistry", JsonRPC("")))
                    .build()
            ).addResolver(
                UportDIDResolver(JsonRPC(Networks.rinkeby.rpcUrl))
            )
            .addResolver(
                WebDIDResolver()
            ).build()

        val dids = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:https:example.com",
            "did:https:example.ngrok.com#owner",
            "did:web:example.com",
            "did:web:example.com#owner",
            "2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX",
            "5A8bRWU3F7j3REx3vkJWxdjQPp4tqmxFPmab1Tr",
            "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX",
            "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX#owner"
        )

        dids.forEach {
            assertThat(resolver.canResolve(it)).isTrue()
        }
    }


    @Test
    fun `fails to resolve valid dids without adding resolvers`() {

        val resolver = DIDResolver.Builder().build()

        val dids = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:https:example.com",
            "did:https:example.ngrok.com#owner",
            "did:web:example.com",
            "did:web:example.com#owner",
            "2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX",
            "5A8bRWU3F7j3REx3vkJWxdjQPp4tqmxFPmab1Tr",
            "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX",
            "did:uport:2nQtiQG6Cgm1GYTBaaKAgr76uY7iSexUkqX#owner",
            "did:generic:0x0011223344556677889900112233445566778899",
            "did:generic:01234",
            "did:generic:has spaces",
            "did:generic:more:colons",
            "did:generic:01234#fragment-attached",
            "did:generic:01234?key=value",
            "did:generic:01234?key=value&other-key=other-value"
        )

        dids.forEach {
            assertThat(resolver.canResolve(it)).isFalse()
        }
    }


    @Test
    fun `fails on invalid dids`() {

        val resolver = DIDResolver.Builder().build()

        val dids = listOf(
            "",
            "0x0011223344556677889900112233445566778899",
            "ethr:0x0011223344556677889900112233445566778899",
            "did:ethr",
            "did::something",
            "did:ethr:"
        )

        dids.forEach {
            assertThat(resolver.canResolve(it)).isFalse()
        }
    }
}

