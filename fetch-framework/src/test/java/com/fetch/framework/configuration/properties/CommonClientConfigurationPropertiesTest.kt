package com.fetch.framework.configuration.properties

import com.fetch.framework.constants.BASE_URL
import com.fetch.framework.constants.BASE_URL_UPDATE
import com.fetch.framework.http.configuration.properties.CommonClientConfigurationProperties
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests to confirm ability to create and instantiate [CommonClientConfigurationPropertiesTest]
 * object and that fields set to the object are retrievable and correct.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class CommonClientConfigurationPropertiesTest {

    private var commonClientConfigurationProperties: CommonClientConfigurationProperties =
        CommonClientConfigurationProperties()

    @Test
    fun setBaseUrl_ReturnTrue() {
        // set common configuration properties
        commonClientConfigurationProperties.baseUrl = BASE_URL
        // confirm that property is not null
        assertNotNull(commonClientConfigurationProperties.baseUrl)
        // confirm that property was set correctly
        assertEquals(commonClientConfigurationProperties.baseUrl, BASE_URL)
        // update property
        commonClientConfigurationProperties.baseUrl = BASE_URL_UPDATE
        // confirm that property is not null
        assertNotNull(commonClientConfigurationProperties.baseUrl)
        // confirm that property was set correctly
        assertEquals(commonClientConfigurationProperties.baseUrl, BASE_URL_UPDATE)
    }
}
