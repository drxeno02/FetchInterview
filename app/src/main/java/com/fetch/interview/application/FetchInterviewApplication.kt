package com.fetch.interview.application

import android.app.Application
import com.fetch.framework.http.configuration.ClientConfiguration
import com.fetch.framework.http.provider.FetchApiClientProvider
import com.fetch.interview.constants.Constants

class FetchInterviewApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // build client configuration
        val clientConfiguration = ClientConfiguration.Builder()
            .setBaseUrl(Constants.BASE_URL)
            .create()

        // initialize API client provider
        FetchApiClientProvider.initialize(
            clientConfiguration
        )
    }
}
