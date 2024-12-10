package com.fetch.framework.http.client

import android.os.Handler
import android.os.Looper
import com.fetch.framework.http.configuration.base.BaseClientConfiguration
import com.fetch.framework.http.interfaces.HttpRequestExecutor
import com.fetch.framework.http.interfaces.HttpResponseCallback
import com.fetch.framework.http.okhttp.OkHttpRequestExecutor
import com.fetch.framework.http.responses.GetItemsResponse
import com.fetch.framework.http.responses.Item
import com.fetch.framework.http.responses.internal.EmptyStateInfo
import com.fetch.framework.http.responses.internal.ErrorItem
import com.fetch.framework.http.responses.internal.HttpStatusCode
import com.fetch.framework.http.responses.internal.Response
import com.fetch.framework.http.responses.internal.ResponseCallback
import com.fetch.framework.http.responses.internal.ResponseData
import com.fetch.framework.http.responses.internal.ResponseItem
import com.fetch.framework.http.utils.Utils.isArrayResponse
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

/**
 * This abstract class encapsulates HTTP logic.
 *
 * @param T : BaseClientConfiguration Used to abstract the attributes used in HTTP request in.
 * @property configuration This property contains necessary info to make the HTTP requests.
 * @property okHttpRequestExecutor This interface establishes a common contract for hiding HTTP
 * library dependencies.
 * @property handler Class used to run a message loop for a thread
 * @property gson This is the main class for using Gson.
 */
abstract class BaseApiClient<T : BaseClientConfiguration>(
    protected var configuration: T,
    protected val okHttpRequestExecutor: HttpRequestExecutor = OkHttpRequestExecutor(),
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private var gson: Gson = Gson()
) {
    /**
     * FOR TESTING ONLY
     * Causes the Runnable to be added to the message queue. The runnable will be run on
     * the thread to which a handler is attached.
     */
    protected var postRunnableHook: () -> Unit = {}

    /**
     * Listen for the [HttpResponseCallback] and update [ResponseCallback] accordingly.
     *
     * @param T Generic type parameter.
     * @param responseCallback Callback to notify call-site of `onSuccess` and `onFailure` events.
     */
    protected inline fun <reified T : EmptyStateInfo> getHttpResponseCallback(
        emptyResponse: T,
        responseCallback: ResponseCallback<T>?
    ) = object : HttpResponseCallback {
        override fun onSuccess(responseItem: ResponseItem) {
            handleValidHttpResponse(
                responseItem = responseItem,
                emptyResponse = emptyResponse,
                tClass = T::class.java,
                responseCallback = responseCallback
            )
        }

        override fun onFailure(errorItem: ErrorItem) {
            handleHttpResponseFailure(
                errorItem = errorItem,
                responseCallback = responseCallback
            )
        }
    }

    /**
     * Handle callbacks for [ResponseCallback] when a HTTP request concludes successfully.
     *
     * @param responseItem HTTP response item.
     * @param emptyResponse Empty object for [T].
     * @param responseCallback Callback to notify call-site of `onSuccess` and `onFailure` events.
     * @param tClass Generic request class [T::class].
     */
    protected fun <T : EmptyStateInfo> handleValidHttpResponse(
        responseItem: ResponseItem,
        emptyResponse: T,
        responseCallback: ResponseCallback<T>?,
        tClass: Class<T>
    ) {
        when (responseItem) {
            is ResponseItem.StringResponseItem -> {

                try {
                    val responseData: ResponseData<T> = when {
                        responseItem.response.isNullOrEmpty() -> ResponseData.EmptyResponse
                        isArrayResponse(responseItem.response) -> {
                            // deserialize as a list if it's an array response
                            val items: List<T> = gson.fromJson(
                                responseItem.response,
                                object : TypeToken<List<Item>>() {}.type
                            )
                            // wrap the list in ResponseData.ListResponse
                            ResponseData.ListResponse(items)
                        }

                        else -> {
                            // deserialize as a single object otherwise
                            val item: T = gson.fromJson(responseItem.response, tClass)
                            // wrap the item in ResponseData.ItemResponse
                            ResponseData.ItemResponse(item)
                        }
                    }

                    // handle valid HTTP response
                    handleResponseSuccess(
                        httpStatusCode = responseItem.statusCode,
                        responseData = responseData,
                        responseCallback = responseCallback
                    )
                } catch (e: JsonSyntaxException) {
                    e.printStackTrace()
                    // handle non-HTTP failure
                    handleNonHttpFailure(
                        exception = e,
                        responseCallback = responseCallback
                    )
                }
            }

            is ResponseItem.EmptyResponseItem -> {
                // handle valid empty HTTP response
                handleResponseSuccess(
                    httpStatusCode = responseItem.statusCode,
                    responseData = ResponseData.EmptyResponse,
                    responseCallback = responseCallback
                )
                handleResponseSuccess(
                    httpStatusCode = responseItem.statusCode,
                    responseData = if (emptyResponse == GetItemsResponse.EMPTY) {
                        ResponseData.EmptyResponse
                    } else {
                        ResponseData.ItemResponse(emptyResponse)
                    },
                    responseCallback = responseCallback
                )
            }
        }
    }

    /**
     * Handle callbacks for [ResponseCallback] when a response succeeds.
     *
     * @param httpStatusCode Represents an HTTP status with code and message.
     * @param responseData Response data.
     * @param responseCallback Callback to notify call-site of `onSuccess` and `onFailure` events.
     */
    private fun <T : EmptyStateInfo> handleResponseSuccess(
        httpStatusCode: HttpStatusCode,
        responseData: ResponseData<T>,
        responseCallback: ResponseCallback<T>?
    ) = notifyWithHandler {
        when (responseData) {
            is ResponseData.ItemResponse -> {
                responseCallback?.onItemSuccess(
                    Response.ItemSuccess(httpStatusCode, responseData.anyObject)
                )
            }

            is ResponseData.ListResponse -> {
                responseCallback?.onListSuccess(
                    Response.ListSuccess(httpStatusCode, responseData.anyList)
                )
            }

            is ResponseData.EmptyResponse -> {
                responseCallback?.onEmptySuccess(
                    Response.EmptySuccess(httpStatusCode)
                )
            }
        }
    }

    /**
     * Handle callbacks for [ResponseCallback] when a response fails.
     *
     * @param errorItem Distinguishes between a runtime error and a failed HTTP response.
     * @param responseCallback Callback to notify call-site of `onSuccess` and `onFailure` events.
     */
    private fun <T : EmptyStateInfo> handleResponseFailure(
        errorItem: ErrorItem,
        responseCallback: ResponseCallback<T>?
    ) = notifyWithHandler {
        responseCallback?.onFailure(
            Response.Failure(
                errorItem = errorItem
            )
        )
    }

    /**
     * Handle non-Http failures and callbacks for failures.
     *
     * @param exception An object that wraps an error event that occurred and contains information
     * about the error including its type.
     * @param responseCallback Callback to notify call-site of `onSuccess` and `onFailure` events.
     */
    private fun <T : EmptyStateInfo> handleNonHttpFailure(
        exception: Exception,
        responseCallback: ResponseCallback<T>?
    ) {
        handleResponseFailure(
            errorItem = ErrorItem.GenericErrorItem(exception),
            responseCallback = responseCallback
        )
    }

    /**
     * Handle HTTP failures and callbacks for failures.
     *
     * @param errorItem Distinguishes between a runtime error and a failed HTTP response.
     * @param responseCallback Callback to notify call-site of `onSuccess` and `onFailure` events.
     */
    protected fun <T : EmptyStateInfo> handleHttpResponseFailure(
        errorItem: ErrorItem,
        responseCallback: ResponseCallback<T>?
    ) {
        handleResponseFailure(
            errorItem = errorItem,
            responseCallback = responseCallback
        )
    }

    /**
     * Wrap [action] around [Handler]'s post call.
     */
    private fun notifyWithHandler(action: () -> Unit) = handler.post { action() }
        .also { postRunnableHook() }
}
