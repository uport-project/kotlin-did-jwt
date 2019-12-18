package me.uport.sdk.ethr_status

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlinx.coroutines.runBlocking
import me.uport.sdk.testhelpers.coAssert
import org.junit.Test

class EthrStatusTests {

    private val ethrStatus = EthrStatus()

    @Test
    fun `returns false for valid credentials`() = runBlocking {

        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzI5NzM2MjMsInN0YXR1cyI6eyJ0eXBlIjoiRXRoclN0YXR1c1JlZ2lzdHJ5MjAxOSIsImlkIjoicmlua2VieToweDFFNDY1MWRjYTVFZjM4NjM2ZTJFNEQ3QTZGZjRkMjQxM2ZDNTY0NTAifSwiaXNzIjoiZGlkOmV0aHI6MHhmM2JlYWMzMGM0OThkOWUyNjg2NWYzNGZjYWE1N2RiYjkzNWIwZDc0In0.CFDlVKGWBiJwUwq14waLQ2fqLljhJG3Qci5KFhcF8zM916sN7MWFESdF1TseIOPmIcteQ_99m61dTTJ0YMY0rwE"
        val result = ethrStatus.checkStatus(token)
        assertThat(result).isFalse()
    }

    @Test
    fun `returns true for revoked credentials`() = runBlocking {

        val token =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzMwNDczNTEsInN0YXR1cyI6eyJ0eXBlIjoiRXRoclN0YXR1c1JlZ2lzdHJ5MjAxOSIsImlkIjoicmlua2VieToweDFFNDY1MWRjYTVFZjM4NjM2ZTJFNEQ3QTZGZjRkMjQxM2ZDNTY0NTAifSwiaXNzIjoiZGlkOmV0aHI6MHgxZmNmOGZmNzhhYzUxMTdkOWM5OWI4MzBjNzRiNjY2OGQ2YWMzMjI5In0.MHabafA0UxJuQJ0Z-7Egb57WRlgj4_zf96B0LUhRyXgVDU5RABIczTTTXWjcuKVzhJc_-FuhRI8uQYmQQNxKzgA"
        val result = ethrStatus.checkStatus(token)
        assertThat(result).isTrue()
    }

    @Test
    fun `throws error for unknown status entry`() = runBlocking {

        val token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzI5NjY3ODAsInN0YXR1cyI6eyJ0eXBlIjoidW5rbm93biIsImlkIjoic29tZXRoaW5nIHNvbWV0aGluZyJ9LCJpc3MiOiJkaWQ6ZXRocjoweGYzYmVhYzMwYzQ5OGQ5ZTI2ODY1ZjM0ZmNhYTU3ZGJiOTM1YjBkNzQifQ.WO4kUEYy3xzZR1VlofOm3e39e1XM227uIr-Z7Yb9YQcJJ-2PRcnQmecW5fDjIfF3EInS3rRd4TZmuVQOnhaKQAE\n"
        coAssert {
            ethrStatus.checkStatus(token)
        }.thrownError {
            isInstanceOf(IllegalStateException::class)
        }
    }
}