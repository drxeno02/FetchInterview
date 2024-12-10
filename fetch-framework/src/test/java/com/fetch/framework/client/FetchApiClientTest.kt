package com.fetch.framework.client

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.fetch.framework.constants.BASE_URL
import com.fetch.framework.http.client.FetchApiClient
import com.fetch.framework.http.configuration.ClientConfiguration
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class FetchApiClientTest {

    private lateinit var context: Context
    private lateinit var fetchApiClient: FetchApiClient
    private lateinit var clientConfiguration: ClientConfiguration
    private lateinit var mockServer: MockWebServer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        val config = Configuration.Builder()
            // Use a SynchronousExecutor here to make it easier to write tests
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // custom dispatcher for mock web server
        // the advantage of a dispatcher is that we get to map url paths to specific responses
        mockServer = MockWebServer()
        mockServer.start()

        // instantiate client configuration
        clientConfiguration = ClientConfiguration.Builder()
            .setBaseUrl(BASE_URL)
            .create()

        // instantiate api client
        fetchApiClient = FetchApiClient(clientConfiguration)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun setClientConfiguration() {
        val newConfig = ClientConfiguration.Builder()
            .setBaseUrl("base_url")
            .create()
        assertNotEquals(newConfig, fetchApiClient.getClientConfiguration())
        fetchApiClient.updateClientConfiguration(newConfig)
        assertEquals(newConfig, fetchApiClient.getClientConfiguration())
    }
}
