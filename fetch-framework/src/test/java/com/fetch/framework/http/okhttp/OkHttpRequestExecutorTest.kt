package com.fetch.framework.http.okhttp

import com.fetch.framework.http.capture
import com.fetch.framework.http.interfaces.HttpResponseCallback
import com.fetch.framework.http.okhttp.internal.RequestCleanupSpec
import com.fetch.framework.http.okhttp.internal.RequestCleanupStrategy
import com.fetch.framework.http.okhttp.internal.RequestState
import com.fetch.framework.http.requests.internal.HttpMethod
import com.fetch.framework.http.requests.internal.HttpRequest
import com.fetch.framework.http.requests.internal.RequestPayload
import com.fetch.framework.http.responses.internal.ErrorItem
import com.fetch.framework.http.responses.internal.HttpStatusCode
import com.fetch.framework.http.responses.internal.ResponseItem
import com.nhaarman.mockitokotlin2.lastValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val SINGLE_LATCH = 1
private const val EMPTY_JSON_OBJECT = "{}"

// test URLs and status codes
private const val DEFAULT_URL = "http://test.com/"
private const val DEFAULT_ERROR_HTTP_STATUS_CODE = 400
private const val DEFAULT_SUCCESS_HTTP_STATUS_CODE = 200
private const val DEFAULT_MOCK_RESPONSE_DELAY_IN_SECONDS = 5L
private const val DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS = 3L

// mock responses; both successful and failed response
private const val MOCK_RESPONSE_PATH_SUCCESS_EMPTY_JSON_OBJECT = "/success/empty_json"
private const val MOCK_RESPONSE_PATH_SUCCESS_EMPTY_BODY = "/success/empty_body"
private const val MOCK_RESPONSE_PATH_SUCCESS_EMPTY_BODY_WITH_DELAY =
    "/success/empty_body?delayed=true"
private const val MOCK_RESPONSE_PATH_FAILURE_HTTP_ERROR = "/failure?error=http"
private const val MOCK_RESPONSE_PATH_FAILURE_GENERIC_ERROR = "/failure?error=generic"

// media type representing JSON
private val MEDIA_TYPE_JSON = RequestPayload.CONTENT_TYPE_APPLICATION_JSON.toMediaTypeOrNull()

// payload examples
private val REQUEST_BODY_EMPTY_JSON_OBJECT = EMPTY_JSON_OBJECT.toRequestBody(MEDIA_TYPE_JSON)
private val REQUEST_PAYLOAD_STRING_JSON = RequestPayload.StringRequestPayload(
    RequestPayload.CONTENT_TYPE_APPLICATION_JSON,
    EMPTY_JSON_OBJECT
)

/**
 * Tests to verify validity of [OkHttpRequestExecutor]. Verification can be broken down into:
 * - Building the request payload/body: [OkHttpRequestExecutor.buildRequestBody]
 * - Building the HTTP request: [OkHttpRequestExecutor.buildRequest]
 * - Verifying callback behavior: [HttpResponseCallback]
 */
class OkHttpRequestExecutorTest {

    private val okHttpClient = OkHttpClient.Builder().build()
    private val okHttpRequestExecutor =
        OkHttpRequestExecutor(okHttpClient, RequestCleanupStrategy())

    companion object {
        val mockServer = MockWebServer()

        // custom dispatcher for mock web server
        // the advantage of a dispatcher is that we get to map url paths to specific responses
        private val mockRequestDispatcher = TestDispatcher()

        @JvmStatic
        @BeforeClass
        fun init() {
            mockServer.start()
            mockServer.dispatcher = mockRequestDispatcher
        }

        @JvmStatic
        @AfterClass
        fun tearDown() {
            mockServer.shutdown()
        }
    }

    @Test
    fun verify_Request_Executor_Notifies_Http_Response_Callback_Of_OnSuccess_Event_With_Valid_HttpStatus_And_String_Body() {
        // given
        val httpResponseCallbackLatch = CountDownLatch(SINGLE_LATCH)
        val requestStateLatch = CountDownLatch(SINGLE_LATCH)

        // cleanup strategy
        val requestCleanupStrategy = CountdownLatchedRequestCleanup(requestStateLatch)

        // countdown latched request cleanup
        val spiedCleanupStrategy = Mockito.spy(requestCleanupStrategy)
        val okHttpRequestExecutor = OkHttpRequestExecutor(
            client = okHttpClient,
            requestCleanupStrategy = spiedCleanupStrategy
        )
        // mockserver with response
        val emptyJsonObjectResponseUrl = mockServer.url(
            path = MOCK_RESPONSE_PATH_SUCCESS_EMPTY_JSON_OBJECT
        ).toString()

        // compose HTTP request
        val httRequest = HttpRequest(
            url = emptyJsonObjectResponseUrl,
            httpMethod = HttpMethod.GET
        )
        val expectedHttpStatusCode = HttpStatusCode.fromStatusCode(DEFAULT_SUCCESS_HTTP_STATUS_CODE)
        val spiedExecutor = Mockito.spy(okHttpRequestExecutor)
        val spiedCallback = Mockito.spy(
            CountdownLatchedHttpResponseCallback(
                httpResponseCallbackLatch
            )
        )
        val responseCaptor = ArgumentCaptor.forClass(ResponseItem::class.java)
        val stateCaptor = ArgumentCaptor.forClass(RequestState::class.java)

        // when
        spiedExecutor.execute(httRequest, spiedCallback)
        // initialize delay as the mock server will take sometime to reply back to the request
        httpResponseCallbackLatch.await(
            DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS,
            TimeUnit.SECONDS
        )
        requestStateLatch.await(DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS, TimeUnit.SECONDS)

        // then
        Mockito.verify(spiedCallback, Mockito.times(1))?.onSuccess(capture(responseCaptor))
        MatcherAssert.assertThat(
            responseCaptor.value,
            CoreMatchers.instanceOf(ResponseItem.StringResponseItem::class.java)
        )
        assertEquals(expectedHttpStatusCode, responseCaptor.value.statusCode)
        assertEquals(
            EMPTY_JSON_OBJECT,
            (responseCaptor.value as ResponseItem.StringResponseItem).response
        )
        Mockito.verify(
            spiedCleanupStrategy,
            Mockito.atLeastOnce()
        )?.onStateChanged(
            capture(stateCaptor)
        )
    }

    @Test
    fun verify_Request_Executor_Notifies_Http_Response_Callback_Of_OnSuccess_Event_With_Valid_HttpStatus_And_Empty_Body() {
        // given
        val expectedHttpStatusCode = HttpStatusCode.fromStatusCode(DEFAULT_SUCCESS_HTTP_STATUS_CODE)

        val httpResponseCallbackLatch = CountDownLatch(SINGLE_LATCH)
        val requestStateLatch = CountDownLatch(SINGLE_LATCH)

        // cleanup strategy
        val requestCleanupStrategy = CountdownLatchedRequestCleanup(requestStateLatch)

        // countdown latched request cleanup
        val spiedCleanupStrategy = Mockito.spy(requestCleanupStrategy)
        val okHttpRequestExecutor = OkHttpRequestExecutor(
            client = okHttpClient,
            requestCleanupStrategy = spiedCleanupStrategy
        )
        // mockserver with response
        val emptyBodyResponseUrl = mockServer.url(
            MOCK_RESPONSE_PATH_SUCCESS_EMPTY_BODY
        ).toString()

        // compose HTTP request
        val httRequest = HttpRequest(
            url = emptyBodyResponseUrl,
            httpMethod = HttpMethod.GET
        )
        val spiedExecutor = Mockito.spy(okHttpRequestExecutor)
        val spiedCallback = Mockito.spy(
            CountdownLatchedHttpResponseCallback(
                httpResponseCallbackLatch
            )
        )
        val responseCaptor = ArgumentCaptor.forClass(ResponseItem::class.java)
        val stateCaptor = ArgumentCaptor.forClass(RequestState::class.java)

        // when
        spiedExecutor.execute(httRequest, spiedCallback)
        // initialize delay as the mock server will take sometime to reply back to the request
        httpResponseCallbackLatch.await(
            DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS,
            TimeUnit.SECONDS
        )
        requestStateLatch.await(DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS, TimeUnit.SECONDS)

        // then
        Mockito.verify(spiedCallback, Mockito.times(1))?.onSuccess(capture(responseCaptor))
        MatcherAssert.assertThat(
            responseCaptor.value,
            CoreMatchers.instanceOf(ResponseItem.EmptyResponseItem::class.java)
        )
        assertEquals(expectedHttpStatusCode, responseCaptor.value.statusCode)
        Mockito.verify(
            spiedCleanupStrategy,
            Mockito.atLeastOnce()
        )?.onStateChanged(
            capture(stateCaptor)
        )
        assertEquals(stateCaptor.lastValue, RequestState.Successful)
    }

    @Test
    fun verify_Request_Executor_Notifies_Http_Response_Callback_Of_OnFailed_Event_With_Valid_HttpStatus_And_Error_Body() {
        // given
        val httpResponseCallbackLatch = CountDownLatch(SINGLE_LATCH)
        val requestStateLatch = CountDownLatch(SINGLE_LATCH)

        // cleanup strategy
        val requestCleanupStrategy = CountdownLatchedRequestCleanup(requestStateLatch)

        // countdown latched request cleanup
        val spiedCleanupStrategy = Mockito.spy(requestCleanupStrategy)
        val okHttpRequestExecutor = OkHttpRequestExecutor(
            okHttpClient,
            spiedCleanupStrategy
        )
        // mockserver with response
        val httpErrorUrl = mockServer.url(
            MOCK_RESPONSE_PATH_FAILURE_HTTP_ERROR
        ).toString()

        // compose HTTP request
        val httRequest = HttpRequest(
            url = httpErrorUrl,
            httpMethod = HttpMethod.GET
        )
        val spiedExecutor = Mockito.spy(okHttpRequestExecutor)
        val spiedCallback = Mockito.spy(
            CountdownLatchedHttpResponseCallback(
                httpResponseCallbackLatch
            )
        )
        val errorCaptor = ArgumentCaptor.forClass(ErrorItem::class.java)
        val stateCaptor = ArgumentCaptor.forClass(RequestState::class.java)

        // when
        spiedExecutor.execute(httRequest, spiedCallback)
        // initialize delay as the mock server will take sometime to reply back to the request
        httpResponseCallbackLatch.await(
            DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS,
            TimeUnit.SECONDS
        )
        requestStateLatch.await(DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS, TimeUnit.SECONDS)

        // then
        Mockito.verify(spiedCallback, Mockito.times(1))?.onFailure(capture(errorCaptor))
        val actualErrorItem = (errorCaptor.value as ErrorItem.HttpErrorItem)
        MatcherAssert.assertThat(
            actualErrorItem,
            CoreMatchers.instanceOf(ErrorItem.HttpErrorItem::class.java)
        )
        assertEquals(
            HttpStatusCode.fromStatusCode(DEFAULT_ERROR_HTTP_STATUS_CODE),
            actualErrorItem.statusCode
        )
        Mockito.verify(
            spiedCleanupStrategy,
            Mockito.atLeastOnce()
        )?.onStateChanged(
            capture(stateCaptor)
        )
    }

    @Test
    fun verify_Request_Executor_Notifies_Http_Response_Callback_Of_OnFailed_Event_With_Valid_Error() {
        // given
        val httpResponseCallbackLatch = CountDownLatch(SINGLE_LATCH)
        val requestStateLatch = CountDownLatch(SINGLE_LATCH)

        // cleanup strategy
        val requestCleanupStrategy = CountdownLatchedRequestCleanup(requestStateLatch)

        // countdown latched request cleanup
        val spiedCleanupStrategy = Mockito.spy(requestCleanupStrategy)
        val okHttpRequestExecutor = OkHttpRequestExecutor(
            client = okHttpClient,
            requestCleanupStrategy = spiedCleanupStrategy
        )
        val spiedExecutor = Mockito.spy(okHttpRequestExecutor)
        val spiedCallback = Mockito.spy(
            CountdownLatchedHttpResponseCallback(
                httpResponseCallbackLatch
            )
        )
        // mockserver with response
        val genericErrorUrl = mockServer.url(
            MOCK_RESPONSE_PATH_FAILURE_GENERIC_ERROR
        ).toString()

        // compose HTTP request
        val httRequest = HttpRequest(
            url = genericErrorUrl,
            httpMethod = HttpMethod.GET
        )
        val errorCaptor = ArgumentCaptor.forClass(ErrorItem::class.java)

        // when
        spiedExecutor.execute(httRequest, spiedCallback)
        // initialize delay as the mock server will take sometime to reply back to the request
        httpResponseCallbackLatch.await(
            DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS,
            TimeUnit.SECONDS
        )
        requestStateLatch.await(DEFAULT_MOCK_RESPONSE_TEST_DELAY_IN_SECONDS, TimeUnit.SECONDS)

        // then
        Mockito.verify(
            spiedCallback,
            Mockito.times(1)
        )?.onFailure(
            capture(errorCaptor)
        )
        val actualErrorItem = (errorCaptor.value as ErrorItem.GenericErrorItem)
        MatcherAssert.assertThat(
            actualErrorItem,
            CoreMatchers.instanceOf(ErrorItem.GenericErrorItem::class.java)
        )
    }

    @Test
    fun verify_Valid_Request_Object_Is_Built_For_Http_GET_Request() {
        // give + when
        val request = okHttpRequestExecutor.buildRequest(
            url = DEFAULT_URL,
            httpMethod = HttpMethod.GET,
            requestPayload = RequestPayload.EmptyRequestPayload
        )

        // then
        assertEquals(DEFAULT_URL, request.url.toString())
        assertEquals(HttpMethod.GET.name, request.method)
        assertTrue(request.headers.size == 0)
        assertEquals(EMPTY_REQUEST.contentType(), request.body?.contentType())
    }

    @Test
    fun verify_Valid_Request_Object_Is_Built_For_Http_POST_Request_With_An_Empty_JSON_String_Payload() {
        // given + when
        val request = okHttpRequestExecutor.buildRequest(
            url = DEFAULT_URL,
            httpMethod = HttpMethod.POST,
            requestPayload = REQUEST_PAYLOAD_STRING_JSON
        )

        // then
        assertEquals(DEFAULT_URL, request.url.toString())
        assertEquals(HttpMethod.POST.name, request.method)
        assertTrue(request.headers.size == 0)
        assertEquals(
            REQUEST_BODY_EMPTY_JSON_OBJECT.contentType(),
            request.body?.contentType()
        )
    }

    @Test
    fun verify_Valid_Response_Body_Is_Returned_When_Request_Payload_Is_Valid() {
        // given + when
        val actualRequestBody = okHttpRequestExecutor.buildRequestBody(REQUEST_PAYLOAD_STRING_JSON)

        // then
        assertNotNull(actualRequestBody)
        assertEquals(
            REQUEST_BODY_EMPTY_JSON_OBJECT.contentType(),
            actualRequestBody?.contentType()
        )
        assertEquals(
            REQUEST_BODY_EMPTY_JSON_OBJECT.contentLength(),
            actualRequestBody?.contentLength()
        )
    }

    @Test
    fun verify_Null_Body_Is_Returned_When_Request_Payload_Type_Is_Null() {
        // given + when
        val requestBody = okHttpRequestExecutor.buildRequestBody(null)

        // then
        assertNull(requestBody)
    }

    @Test
    fun verify_Empty_Request_Body_Is_Returned_When_Request_Payload_Type_Is_Empty() {
        // given + when
        val requestBody = okHttpRequestExecutor.buildRequestBody(RequestPayload.EmptyRequestPayload)

        // then
        assertNotNull(requestBody)
        assertEquals(EMPTY_REQUEST, requestBody)
    }

    @Test
    fun verify_OkHttpRequestExecutor_Supports_Multiple_Api_Calls() {
        runBlocking {
            val deferredCalls = mutableListOf<Deferred<ResponseItem>>()
            coroutineScope {
                repeat(10) { deferredCalls.add(async(Dispatchers.Default) { getDelayedResponseItem() }) }
                deferredCalls.awaitAll().forEach { responseItem ->
                    assertNotNull(responseItem)
                    MatcherAssert.assertThat(
                        responseItem,
                        CoreMatchers.instanceOf(ResponseItem.StringResponseItem::class.java)
                    )
                }
            }
        }
    }

    private suspend fun getDelayedResponseItem() =
        suspendCancellableCoroutine { continuation: CancellableContinuation<ResponseItem> ->
            val emptyBodyResponseUrl = mockServer.url(
                MOCK_RESPONSE_PATH_SUCCESS_EMPTY_BODY_WITH_DELAY
            ).toString()
            // compose HTTP request
            val httRequest = HttpRequest(
                url = emptyBodyResponseUrl,
                httpMethod = HttpMethod.GET
            )
            // perform GET operation
            okHttpRequestExecutor.execute(
                httRequest,
                ContinuationHttpResponseItem(continuation)
            )
        }
}

/**
 * For testing purposes we need to await till our callback(s) receive responses from the
 * [MockWebServer]. This implementation uses the [CountDownLatch] to [CountDownLatch.countDown]
 * and resume execution for the test.
 *
 * @property latch [CountDownLatch] that notifies call-site to resume execution.
 */
open class CountdownLatchedHttpResponseCallback(private val latch: CountDownLatch) :
    HttpResponseCallback {

    override fun onSuccess(responseItem: ResponseItem) {
        latch.countDown()
    }

    override fun onFailure(errorItem: ErrorItem) {
        latch.countDown()
    }
}

/**
 * For testing purposes we need to await till our callback(s) receive responses from the
 * [MockWebServer]. This implementation uses the [CountDownLatch] to [CountDownLatch.countDown]
 * and resume execution for the test.
 *
 * @property latch [CountDownLatch] that notifies call-site to resume execution.
 */
open class CountdownLatchedRequestCleanup(private val latch: CountDownLatch) :
    RequestCleanupSpec() {

    override fun onStateChanged(state: RequestState) {
        super.onStateChanged(state)
        latch.countDown()
    }
}

/**
 * Convert [HttpResponseCallback] to a [CancellableContinuation] compatible call to test
 * multiple API calls.
 */
class ContinuationHttpResponseItem(private val continuation: CancellableContinuation<ResponseItem>) :
    HttpResponseCallback {

    override fun onSuccess(responseItem: ResponseItem) {
        continuation.resume(responseItem)
    }

    override fun onFailure(errorItem: ErrorItem) {
        continuation.resumeWithException(errorItem.exception)
    }
}

/**
 * Custom handler for mock server requests. The test will map a [RecordedRequest.path]
 * to an expected [MockResponse].
 */
class TestDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.path) {
            MOCK_RESPONSE_PATH_SUCCESS_EMPTY_BODY_WITH_DELAY -> MockResponse().apply {
                setBody(EMPTY_JSON_OBJECT)
                setBodyDelay(DEFAULT_MOCK_RESPONSE_DELAY_IN_SECONDS, TimeUnit.SECONDS)
            }

            MOCK_RESPONSE_PATH_SUCCESS_EMPTY_JSON_OBJECT -> MockResponse().apply {
                setBody(
                    EMPTY_JSON_OBJECT
                )
            }

            MOCK_RESPONSE_PATH_FAILURE_HTTP_ERROR -> MockResponse().apply {
                setResponseCode(
                    DEFAULT_ERROR_HTTP_STATUS_CODE
                )
            }

            MOCK_RESPONSE_PATH_FAILURE_GENERIC_ERROR -> MockResponse().apply {
                val invalidStatusLine = "$DEFAULT_ERROR_HTTP_STATUS_CODE"
                status = invalidStatusLine
            }

            else -> {
                // default 200 response
                return MockResponse()
            }
        }
    }
}
