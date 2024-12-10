package com.fetch.framework.http.requests.internal

import com.fetch.framework.http.okhttp.OkHttpRequestExecutor

/**
 * Class to map [HttpException] in [OkHttpRequestExecutor].
 *
 * @property responseBody error string of [HttpException]
 */
data class HttpException(val responseBody: String?) : Exception(responseBody)
