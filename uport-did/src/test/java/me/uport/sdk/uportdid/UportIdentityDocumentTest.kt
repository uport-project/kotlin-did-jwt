@file:Suppress("DEPRECATION")

package me.uport.sdk.uportdid

import assertk.assertThat
import assertk.assertions.isNotNull
import org.junit.Test

class UportIdentityDocumentTest {

    @Test
    fun `can parse legacy Identity document`() {
        val legacyDoc =
            """{"name":"uPort Demo","@type":"App","publicKey":"0x04171fcc7654cad14745b9835bc534d8e59038ae6929c793d7f8dd2c934580ca39ff1e2de3d7ef69a8daba5e5590d3ec80486a273cbe2bd1b76ebd01f949b41463","description":"Demo App","url":"demo.uport.me","image":{"contentUrl":"/ipfs/Qmez4bdFmxPknbAoGzHmpjpLjQFChq39h5UMPGiwUHgt8f"},"address":"2oeXufHGDpU51bfKBsZDdu7Je9weJ3r7sVG"}"""
        val parsed = UportIdentityDocument.fromJson(legacyDoc)
        assertThat(parsed).isNotNull()
    }
}