package com.fetch.framework.http.requests.internal

/**
 * Represents all the required parameters for an HTTP request.
 *
 * @property url The request URL.
 * @property httpMethod Type of HTTP request method to be used. For e.g. [HttpMethod.GET],
 * [HttpMethod.POST], etc.
 * @property requestPayload The request parameter to be included as a part of the HTTP request.
 */
class HttpRequest(
    val url: String?,
    val httpMethod: HttpMethod,
    val requestPayload: RequestPayload? = null
)
