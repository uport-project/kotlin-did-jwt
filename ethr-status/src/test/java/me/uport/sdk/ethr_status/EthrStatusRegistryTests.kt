package me.uport.sdk.ethr_status

import assertk.assertThat
import assertk.assertions.isFalse
import kotlinx.coroutines.runBlocking
import org.junit.Test

class EthrStatusRegistryTests {

    @Test
    fun `can check for revocation`() = runBlocking {

        val validShareReqToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJjbGFpbXMiOnsibmFtZSI6IlIgRGFuZWVsIE9saXZhdyJ9LCJzdGF0dXMiOnsidHlwZSI6IkV0aHJTdGF0dXNSZWdpc3RyeTIwMTkiLCJpZCI6ImRpZDpldGhyOjB4ZThjOTFiZGU3NjI1YWIyYzBlZDlmMjE0ZGViMzk0NDBkYTdlMDNjNCJ9LCJpYXQiOjEyMzQ1Njc4LCJleHAiOjEyMzQ1OTc4LCJpc3MiOiJkaWQ6ZXRocjoweDQxMjNjYmQxNDNiNTVjMDZlNDUxZmYyNTNhZjA5Mjg2YjY4N2E5NTAifQ.iRuhl5JHPIBLP_WSr3vRwM8MWTKwJUAUk5AzDDuRvyySsQXuZ-W3Ri3H9WISytZjwHy3ZAnBTcZ6_gkTfdvYhAE"
        val statusRegistry = EthrStatusRegistry()
        val result = statusRegistry.checkStatus(validShareReqToken)
        assertThat(result).isFalse()
    }
}