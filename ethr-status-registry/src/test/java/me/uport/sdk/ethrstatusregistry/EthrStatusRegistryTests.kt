package me.uport.sdk.ethrstatusregistry

import kotlinx.coroutines.runBlocking
import org.junit.Test

class EthrStatusRegistryTests {

    @Test
    fun `can check for revocation`() = runBlocking {

        val validShareReqToken =
            "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJjbGFpbXMiOnsibmFtZSI6IlIgRGFuZWVsIE9saXZhdyJ9LCJzdGF0dXMiOnsidHlwZSI6IkV0aHJTdGF0dXNSZWdpc3RyeTIwMTkiLCJpZCI6MTI4ODg3fSwiaWF0IjoxMjM0NTY3OCwiZXhwIjoxMjM0NTk3OCwiaXNzIjoiZGlkOmV0aHI6MHg0MTIzY2JkMTQzYjU1YzA2ZTQ1MWZmMjUzYWYwOTI4NmI2ODdhOTUwIn0.egVYU2TWReCgI7EfRYo2P9q0Oz8Lgopl3SOORv9miQVvQKLRNX4HUdnBC4KPjeX3gcIz9wEgH_cgQL5zhcxiTQE"
        val statusRegistry = EthrStatusRegistry()
        val result = statusRegistry.checkStatus(validShareReqToken)
    }
}