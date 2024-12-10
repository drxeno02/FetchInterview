package com.fetch.framework.configuration

import com.fetch.framework.constants.BASE_URL
import com.fetch.framework.http.configuration.ClientConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests to confirm ability to create and instantiate [ClientConfiguration] object and that fields
 * set to the object are retrievable and correct.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ClientConfigurationTest {

    @Test
    fun valid_ClientConfiguration_ReturnsTrue() {
        // initialize ClientConfiguration
        val clientConfiguration = ClientConfiguration.Builder()
            .setBaseUrl(BASE_URL)
            .create()
        // client configuration should not be null
        assertNotNull(clientConfiguration)

        // confirm information
        assertEquals(BASE_URL, clientConfiguration.baseUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun initialize_ClientConfiguration_MissingBaseUrl_IllegalArgumentException() {
        // initialize ClientConfiguration
        ClientConfiguration.Builder()
            .create()
    }
}
