package com.fetch.framework.http

import com.fetch.framework.http.responses.internal.HttpStatusCode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

const val MESSAGE_UNKNOWN_STATUS_CODE = "Unknown Status Code"

class HttpStatusCodeTest {

    @Test
    fun verify_200_Status_Code_Successful_Http_Request_ReturnsTrue() {
        assertTrue(HttpStatusCode.fromStatusCode(200).isSuccessful)
    }

    @Test
    fun verify_300_Status_Code_Failed_Http_Request_ReturnsTrue() {
        assertFalse(HttpStatusCode.fromStatusCode(300).isSuccessful)
    }

    @Test
    fun verify_Same_Status_Codes_Are_Equal_ReturnsTrue() {
        assertTrue(HttpStatusCode.fromStatusCode(200) == HttpStatusCode.fromStatusCode(200))
    }

    @Test
    fun verify_Different_Status_Codes_Are_Not_Equal_ReturnsTrue() {
        assertFalse(HttpStatusCode.fromStatusCode(200) == HttpStatusCode.fromStatusCode(300))
    }

    @Test
    fun verify_200_Status_Code_Gets_Correctly_Mapped_ReturnsTrue() {
        val successfulStatusCode = HttpStatusCode.fromStatusCode(200)
        assertTrue(successfulStatusCode.message == "OK")
        assertTrue(successfulStatusCode.code == 200)
    }

    @Test
    fun verify_Unknown_Status_Code_Gets_Mapped_As_An_Unknown_Error_ReturnsTrue() {
        val invalidStatusCode = HttpStatusCode.fromStatusCode(600)
        assertTrue(invalidStatusCode.code == 600)
        assertTrue(invalidStatusCode.message == MESSAGE_UNKNOWN_STATUS_CODE)
    }
}
