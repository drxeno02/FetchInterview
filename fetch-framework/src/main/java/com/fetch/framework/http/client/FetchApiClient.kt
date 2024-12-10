package com.fetch.framework.http.client

import com.fetch.framework.http.client.interfaces.FetchApiClientInterfaces
import com.fetch.framework.http.configuration.ClientConfiguration
import com.fetch.framework.http.provider.FetchApiClientProvider
import com.fetch.framework.http.requests.internal.HttpMethod
import com.fetch.framework.http.requests.internal.HttpRequest
import com.fetch.framework.http.requests.internal.RequestPayload
import com.fetch.framework.http.responses.GetItemsResponse
import com.fetch.framework.http.responses.Item
import com.fetch.framework.http.responses.internal.Response
import com.fetch.framework.http.responses.internal.ResponseCallback
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Used to access Fetch APIs. The [FetchApiClient] is a common entry point to Fetch API services
 * and automatically manages network connections. The [FetchApiClient] is cheap to create, and
 * is accessible through the singleton provider, [FetchApiClientProvider].
 *
 * @property clientConfiguration Contains necessary configuration data for making requests.
 * @constructor
 */
open class FetchApiClient(
    private var clientConfiguration: ClientConfiguration
) : FetchApiClientInterfaces, BaseApiClient<ClientConfiguration>(
    configuration = clientConfiguration
) {

    /**
     * Method is used make HTTP POST requests.
     *
     * @return [GetItemsResponse]
     */
    override suspend fun getItems(): GetItemsResponse<Item> = suspendCoroutine { continuation ->
        // compose HTTP request
        val httpRequest = HttpRequest(
            url = clientConfiguration.itemsUrl,
            httpMethod = HttpMethod.GET,
            requestPayload = RequestPayload.EmptyRequestPayload
        )

        // execute GET request
        okHttpRequestExecutor.execute(
            httpRequest = httpRequest,
            callback = getHttpResponseCallback(
                emptyResponse = GetItemsResponse.EMPTY,
                responseCallback = object : ResponseCallback<GetItemsResponse<Item>> {
                    override fun onItemSuccess(data: Response.ItemSuccess<GetItemsResponse<Item>>) {
                        continuation.resume(data.item)
                    }

                    override fun onListSuccess(data: Response.ListSuccess<GetItemsResponse<Item>>) {
                        // create a GetItemsResponse<Item> object using the deserialized items
                        val getItemsResponse = GetItemsResponse(data.items.filterIsInstance<Item>())
                        continuation.resume(getItemsResponse)
                    }

                    override fun onEmptySuccess(data: Response.EmptySuccess) {
                        continuation.resume(GetItemsResponse.EMPTY)
                    }

                    override fun onFailure(data: Response.Failure<GetItemsResponse<Item>>) {
                        continuation.resumeWithException(data.errorItem.exception)
                    }
                }
            )
        )
    }

    /**
     * Returns [ClientConfiguration] information used by the [FetchApiClient].
     *
     * @return [ClientConfiguration]
     */
    override fun getClientConfiguration(): ClientConfiguration {
        return this.clientConfiguration
    }

    /**
     * Update client configuration.
     *
     * <p>The updated changes only apply if [FetchApiClient] is initialized.</p>
     *
     * @param clientConfiguration REQUIRED: Configuration that collects information necessary to
     * perform request operations.
     */
    override fun updateClientConfiguration(
        clientConfiguration: ClientConfiguration
    ) {
        // update client configuration
        this.clientConfiguration = clientConfiguration
        super.configuration = clientConfiguration
    }
}
