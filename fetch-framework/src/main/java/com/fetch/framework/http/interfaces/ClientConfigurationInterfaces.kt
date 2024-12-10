package com.fetch.framework.http.interfaces

import com.fetch.framework.http.client.FetchApiClient
import com.fetch.framework.http.configuration.ClientConfiguration

interface ClientConfigurationInterfaces {

    /**
     * Returns [ClientConfiguration] information used by the [FetchApiClient].
     *
     * @return [ClientConfiguration]
     */
    fun getClientConfiguration(): ClientConfiguration

    /**
     * Update client configuration.
     *
     * The updated changes only apply if [FetchApiClient] is initialized.
     *
     * @param clientConfiguration REQUIRED: Configuration that collects information necessary to
     * perform request operations.
     */
    fun updateClientConfiguration(clientConfiguration: ClientConfiguration)
}
