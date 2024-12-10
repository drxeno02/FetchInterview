package com.fetch.framework.http.responses.internal

/**
 * Intended to act as a layer of abstraction over network responses, [ResponseItem] defines
 * different types HTTP responses.
 *
 * @property statusCode HTTP response status code
 */
sealed class ResponseItem(
    val statusCode: HttpStatusCode
) {

    /**
     * Represents a HTTP string response body.
     *
     * @property statusCode Represents an HTTP status with code and message.
     * @property response The HTTP response body.
     */
    class StringResponseItem(
        statusCode: HttpStatusCode,
        val response: String?,
    ) : ResponseItem(statusCode)

    /**
     * Represents an empty HTTP response.
     *
     * @property statusCode Represents an HTTP status with code and message.
     */
    class EmptyResponseItem(
        statusCode: HttpStatusCode,
    ) : ResponseItem(statusCode)
}
