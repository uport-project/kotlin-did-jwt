package me.uport.sdk.uportdefaults

import assertk.assertThat
import assertk.assertions.isTrue
import me.uport.sdk.universaldid.DIDResolver
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `can resolve valid dids configuring with infuraId`() {

        val infuraProjectId = "e72b472993ff46d3b5b88faa47214d7f"

        val resolver = DIDResolver.Builder().build().configureDefaultsWithInfura(infuraProjectId)

        val dids = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
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
            "did:web:example.com#owner"
        )

        dids.forEach {
            assertThat(resolver.canResolve(it)).isTrue()
        }
    }
}
