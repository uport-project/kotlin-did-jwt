package me.uport.sdk.jwt.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import org.junit.Test

class JWTAlgorithmTest {

    @Test
    fun `can encode to json`() {
        val result = JwtHeader("hello", "world").toJson()
        //language=JSON
        assertThat(result).isEqualTo("""{"typ":"hello","alg":"world"}""")
    }

    @Test
    fun `can decode json`() {
        //language=JSON
        val result = JwtHeader.fromJson("""{"typ":"hello","alg":"world"}""")
        assertThat(result).isNotNull()
    }

    @Test
    fun `can decode lenient json`() {
        val result = JwtHeader.fromJson("""{typ:"hello",alg:"world"}""")
        assertThat(result).isNotNull()
    }
}