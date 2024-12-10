package com.fetch.framework.http.client.interfaces

import com.fetch.framework.http.interfaces.ClientConfigurationInterfaces
import com.fetch.framework.http.responses.GetItemsResponse
import com.fetch.framework.http.responses.Item
import okhttp3.OkHttpClient

/**
 * Interface for HTTP request executor implementation for [OkHttpClient].
 */
interface FetchApiClientInterfaces : ClientConfigurationInterfaces {

    /**
     * Method is used make HTTP POST requests.
     *
     * @return [GetItemsResponse]
     */
    suspend fun getItems(): GetItemsResponse<Item>?
}
