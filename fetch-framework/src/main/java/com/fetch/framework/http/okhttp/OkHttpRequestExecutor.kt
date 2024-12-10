package com.fetch.framework.http.okhttp

import androidx.annotation.VisibleForTesting
import com.fetch.framework.http.interfaces.HttpRequestExecutor
import com.fetch.framework.http.interfaces.HttpResponseCallback
import com.fetch.framework.http.okhttp.internal.RequestCleanupSpec
import com.fetch.framework.http.okhttp.internal.RequestCleanupStrategy
import com.fetch.framework.http.okhttp.internal.RequestState
import com.fetch.framework.http.requests.internal.HttpException
import com.fetch.framework.http.requests.internal.HttpMethod
import com.fetch.framework.http.requests.internal.HttpRequest
import com.fetch.framework.http.requests.internal.RequestPayload
import com.fetch.framework.http.responses.internal.ErrorItem
import com.fetch.framework.http.responses.internal.HttpStatusCode
import com.fetch.framework.http.responses.internal.ResponseItem
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.Closeable
import java.io.IOException

private const val ERROR_MESSAGE_NULL_EMPTY_URL = "Url cannot be null or empty"

// creates a new array of the specified size, with all elements initialized to zero
// referenced in unit tests
internal val EMPTY_BYTE_ARRAY = ByteArray(0)
internal val EMPTY_REQUEST = EMPTY_BYTE_ARRAY.toRequestBody(null, 0, 0)

/**
 * HTTP request executor implementation for [OkHttpClient].
 *
 * @property client Factory for calls, which can be used to send HTTP requests and
 * read their responses.
 * @property requestCleanupStrategy A resource cleanup/release strategy for HTTP requests.
 * @constructor
 */
open class OkHttpRequestExecutor(
    private var client: OkHttpClient = OkHttpClient(),
    private val requestCleanupStrategy: RequestCleanupSpec = RequestCleanupStrategy()
) : HttpRequestExecutor {

    /**
     * Execute a HTTP request.
     *
     * @param httpRequest Collection of HTTP request models - method, headers and body.
     * @param callback HTTP callback for the call-site to receive the HTTP response.
     */
    override fun execute(
        httpRequest: HttpRequest,
        callback: HttpResponseCallback?
    ) {
        // called when the HTTP request is either RequestState.Successful,
        // RequestState.Failed, or RequestState.Cancelled
        requestCleanupStrategy.onStateChanged(RequestState.Ongoing)
        requestCleanupStrategy.callback = callback

        // establish pre-checks
        if (httpRequest.url.isNullOrEmpty()) {
            callback?.onFailure(
                ErrorItem.GenericErrorItem(
                    NullPointerException(
                        ERROR_MESSAGE_NULL_EMPTY_URL
                    )
                )
            )
            requestCleanupStrategy.onStateChanged(RequestState.Failed)
            return
        }

        client = OkHttpClient().newBuilder().build()
        // initialize request
        val okHttpRequest = buildRequest(
            httpRequest.url,
            httpRequest.httpMethod,
            httpRequest.requestPayload
        )
        val apiCall = client.newCall(okHttpRequest)
        requestCleanupStrategy.ongoingCall = apiCall

        // execute request
        apiCall.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                handleHttpRequestFailure(e, callback)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    handleSuccessfulResponse(response, callback)
                } else {
                    handleErrorResponse(response, callback)
                }
                closeQuietly(response)
            }
        })
    }

    /**
     * Cancel ongoing/last know request. Although request cancellation is not guaranteed,
     * this call will ensure if cancelled, the response callback reflects the correct state
     * along with internal resource cleanup.
     */
    override fun cancel() {
        if (requestCleanupStrategy.ongoingCall == null || requestCleanupStrategy.ongoingCall?.isCanceled() == true) {
            return
        }
        requestCleanupStrategy.ongoingCall?.cancel()
        requestCleanupStrategy.onStateChanged(RequestState.Cancelled)
    }

    /**
     * Build an appropriate [Request] object for the HTTP request.
     *
     * @param url The request url.
     * @param httpMethod The HTTP method.
     * @param requestPayload The request payload.
     *
     * @return [Request] object for the HTTP request.
     */
    @VisibleForTesting
    fun buildRequest(
        url: String,
        httpMethod: HttpMethod,
        requestPayload: RequestPayload?
    ): Request {
        val httpUrl = url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $url")

        return Request.Builder().apply {
            // initialize URL
            url(httpUrl)
            val requestBody = buildRequestBody(requestPayload)
            when (httpMethod) {
                HttpMethod.GET -> get()
                HttpMethod.POST -> post(requestBody.orEmpty())
                HttpMethod.PUT -> put(requestBody.orEmpty())
                HttpMethod.PATCH -> patch(requestBody.orEmpty())
                HttpMethod.DELETE -> requestBody?.let { delete(it) } ?: delete()
            }
        }.build()
    }

    /**
     * Build the HTTP request body.
     *
     * @param requestPayload The request payload.
     *
     * @return A nullable [RequestBody] for the HTTP request.
     */
    @VisibleForTesting
    fun buildRequestBody(
        requestPayload: RequestPayload?
    ): RequestBody? {
        return when (requestPayload) {
            is RequestPayload.StringRequestPayload -> {
                requestPayload.value.orEmpty().toRequestBody(
                    requestPayload.contentType.orEmpty().toMediaTypeOrNull()
                )
            }

            is RequestPayload.EmptyRequestPayload -> {
                EMPTY_REQUEST
            }

            else -> {
                null
            }
        }
    }

    /**
     * Handle [okhttp3.Callback.onResponse] events for an HTTP request.
     *
     * This method specifically handles cases when a response successfully concludes
     * (HTTP response code is between 200 and 300).
     *
     * @param response An HTTP response. Instances of this class are not immutable: the response
     * body is a one-shot value that may be consumed only once and then closed. All other
     * properties are immutable.
     * @param callback A success/failure driven callback for HTTP response(s).
     */
    private fun handleSuccessfulResponse(
        response: Response,
        callback: HttpResponseCallback?
    ) {
        // guard clause
        if (requestCleanupStrategy.currentRequestState == RequestState.Cancelled) {
            return
        }
        val stringBody = response.body.string()
        val statusCode = HttpStatusCode.fromStatusCode(response.code)

        // return response in callback
        callback?.onSuccess(
            if (stringBody.isEmpty()) {
                ResponseItem.EmptyResponseItem(
                    statusCode = statusCode
                )
            } else {
                ResponseItem.StringResponseItem(
                    statusCode = statusCode,
                    response = stringBody
                )
            }
        )
        requestCleanupStrategy.onStateChanged(RequestState.Successful)
    }

    /**
     * Handle [okhttp3.Callback.onFailure] events for an HTTP request.
     *
     * @param exception Signals that an I/O exception of some sort has occurred.
     * @param callback A success/failure driven callback for HTTP response(s).
     */
    private fun handleHttpRequestFailure(
        exception: IOException,
        callback: HttpResponseCallback?
    ) {
        // guard clause
        if (requestCleanupStrategy.currentRequestState == RequestState.Cancelled) {
            return
        }
        callback?.onFailure(ErrorItem.GenericErrorItem(exception))
        requestCleanupStrategy.onStateChanged(RequestState.Failed)
    }

    /**
     * Handle [okhttp3.Callback.onResponse] events for an HTTP request.
     *
     * This method specifically handles cases when a response is unsuccessful
     * (HTTP response code is higher than 300).
     *
     * @param response An HTTP response. Instances of this class are not immutable: the response
     * body is a one-shot value that may be consumed only once and then closed. All other
     * properties are immutable.
     * @param callback A success/failure driven callback for HTTP response(s).
     */
    private fun handleErrorResponse(
        response: Response,
        callback: HttpResponseCallback?
    ) {
        // guard clause
        if (requestCleanupStrategy.currentRequestState == RequestState.Cancelled) {
            return
        }
        // return failed response in callback
        callback?.onFailure(
            ErrorItem.HttpErrorItem(
                statusCode = HttpStatusCode.fromStatusCode(response.code),
                exception = HttpException(response.body.string())
            )
        )
        requestCleanupStrategy.onStateChanged(RequestState.Failed)
    }

    /**
     * An extension function on [RequestBody] to return [EMPTY_REQUEST] if null.
     */
    private fun RequestBody?.orEmpty() = this ?: EMPTY_REQUEST

    /**
     * Method which closes a [Closeable] and absorbs [IOException] if it is thrown.
     *
     * @param closeable A Closeable is a source or destination of data that can be closed.
     * The close method is invoked to release resources that the object is holding
     * (such as open files).
     */
    internal fun closeQuietly(
        closeable: Closeable?
    ) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
