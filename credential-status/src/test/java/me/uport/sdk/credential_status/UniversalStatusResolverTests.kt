package me.uport.sdk.credential_status

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlinx.coroutines.runBlocking
import me.uport.credential_status.UniversalStatusResolver
import me.uport.sdk.ethr_status.EthrStatus
import me.uport.sdk.ethr_status.EthrStatusResolver
import me.uport.sdk.testhelpers.coAssert
import org.junit.Test
import java.math.BigInteger

class UniversalStatusResolverTests {

    @Test
    fun `throws error when no resolvers registered`() = runBlocking {
        coAssert {
            UniversalStatusResolver().checkStatus(
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzMwNDczNTEsInN0YXR1cyI6eyJ0eXBlIjoiRXRoclN0YXR1c1JlZ2lzdHJ5MjAxOSIsImlkIjoicmlua2VieToweDFFNDY1MWRjYTVFZjM4NjM2ZTJFNEQ3QTZGZjRkMjQxM2ZDNTY0NTAifSwiaXNzIjoiZGlkOmV0aHI6MHgxZmNmOGZmNzhhYzUxMTdkOWM5OWI4MzBjNzRiNjY2OGQ2YWMzMjI5In0.MHabafA0UxJuQJ0Z-7Egb57WRlgj4_zf96B0LUhRyXgVDU5RABIczTTTXWjcuKVzhJc_-FuhRI8uQYmQQNxKzgA"
            )
        }.thrownError {
            isInstanceOf(IllegalStateException::class)
        }
    }

    @Test
    fun `throws error when credential is blank`() = runBlocking {
        val resolver = UniversalStatusResolver()
        resolver.registerResolver(EthrStatusResolver())
        coAssert {
            resolver.checkStatus("")
        }.thrownError {
            isInstanceOf(IllegalArgumentException::class)
        }
    }

    @Test
    fun `can register resolvers and use to check status`() = runBlocking {
        val resolver = UniversalStatusResolver()
        resolver.registerResolver(EthrStatusResolver())
        val result = resolver.checkStatus(
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzMwNDczNTEsInN0YXR1cyI6eyJ0eXBlIjoiRXRoclN0YXR1c1JlZ2lzdHJ5MjAxOSIsImlkIjoicmlua2VieToweDFFNDY1MWRjYTVFZjM4NjM2ZTJFNEQ3QTZGZjRkMjQxM2ZDNTY0NTAifSwiaXNzIjoiZGlkOmV0aHI6MHgxZmNmOGZmNzhhYzUxMTdkOWM5OWI4MzBjNzRiNjY2OGQ2YWMzMjI5In0.MHabafA0UxJuQJ0Z-7Egb57WRlgj4_zf96B0LUhRyXgVDU5RABIczTTTXWjcuKVzhJc_-FuhRI8uQYmQQNxKzgA"
        )
        assertThat(result).isEqualTo(EthrStatus(BigInteger.ONE))
    }
}