package com.fetch.framework.configuration.url

import android.content.Context
import android.util.Patterns
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.fetch.framework.constants.BASE_URL
import com.fetch.framework.http.client.FetchApiClient
import com.fetch.framework.http.configuration.ClientConfiguration
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class UrlValidatorTest {

    private lateinit var context: Context
    private lateinit var fetchApiClient: FetchApiClient
    private lateinit var clientConfiguration: ClientConfiguration

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            // Use a SynchronousExecutor here to make it easier to write tests
            .setExecutor(SynchronousExecutor())
            .build()

        // initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // initialize ClientConfiguration
        clientConfiguration = ClientConfiguration.Builder()
            .setBaseUrl(BASE_URL)
            .create()

        // instantiate api client
        fetchApiClient = FetchApiClient(
            clientConfiguration
        )
    }

    /**
     * Method is used to validate url structure and creates a matcher that will match the
     * given input against [Patterns.WEB_URL] pattern.
     *
     * @param url The url to validate.
     */
    private fun isUrlValid(url: String): Boolean {
        val urlParts: List<String> = url.split("//")
        // confirm url structure
        val isUrlStructureValid = urlParts.size < 3 &&
                (urlParts[0].equals("http:", ignoreCase = true) ||
                        urlParts[0].equals("https:", ignoreCase = true))

        return isUrlStructureValid && Patterns.WEB_URL.matcher(
            url.lowercase(Locale.getDefault())
        ).matches()
    }

    @Test
    fun validateAllRequestUrls_ReturnsTrue() {
        assertTrue(isUrlValid(clientConfiguration.itemsUrl))
    }
}
