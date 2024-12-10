package com.fetch.framework.configuration

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.fetch.framework.constants.BASE_URL
import com.fetch.framework.constants.BASE_URL_UPDATE
import com.fetch.framework.http.configuration.ClientConfiguration
import com.fetch.framework.http.provider.FetchApiClientProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FetchApiClientConfigurationTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // set config
        val config = Configuration.Builder()
            // use a SynchronousExecutor here to make it easier to write tests
            .setExecutor(SynchronousExecutor())
            .build()

        // initialize WorkManager for instrumentation tests
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        // reset any pre-initialized instances of FetchApiClient
        FetchApiClientProvider.destroy()

        // initialize ClientConfiguration
        val clientConfiguration = ClientConfiguration.Builder()
            .setBaseUrl(BASE_URL)
            .create()
        // initialize FetchApiClientProvider
        FetchApiClientProvider.initialize(
            clientConfiguration
        )
    }

    @After
    fun destroy() {
        // reset instance for FetchApiClient
        FetchApiClientProvider.destroy()
    }

    @Test
    fun valid_ClientConfiguration_ReturnsTrue() {
        val fetchApiClient = FetchApiClientProvider.getInstance()
        // retrieve ClientConfiguration
        val clientConfiguration = fetchApiClient.getClientConfiguration()
        // client configuration should not be null
        assertNotNull(clientConfiguration)

        // confirm information
        assertEquals(BASE_URL, clientConfiguration.baseUrl)
    }

    @Test
    fun valid_UpdateClientConfiguration_ReturnsTrue() {
        val fetchApiClient = FetchApiClientProvider.getInstance()
        // create updated ClientConfiguration
        val clientConfiguration = ClientConfiguration.Builder()
            .setBaseUrl(BASE_URL_UPDATE)
            .create()
        // update api client with new configuration
        fetchApiClient.updateClientConfiguration(clientConfiguration)
        // retrieve ClientConfiguration
        val updatedClientConfiguration = fetchApiClient.getClientConfiguration()

        // confirm information
        assertEquals(BASE_URL_UPDATE, updatedClientConfiguration.baseUrl)
    }
}
