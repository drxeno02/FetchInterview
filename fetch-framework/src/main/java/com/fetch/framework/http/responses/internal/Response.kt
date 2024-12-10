package com.fetch.framework.http.responses.internal

/**
 * HTTP calls will return the appropriate response object wrapped inside the [Response].
 *
 * [Response] categorizes response objects into various states.
 * - [ItemSuccess]: Denotes that an API call finished successfully; for handling standard responses
 * that are objects.
 * - [ListSuccess]: Denotes that an API call finished successfully; for handling responses with
 * lists as the root.
 * - [EmptySuccess]: Denotes that an API call finished successfully; for handling responses that
 * are completely empty.
 * - [Failure]: An API call failed during execution; probable errors are categorized as a
 * part of [ErrorItem].
 *
 * For best results consume the [Response] inside of `when`:
 * ```
 *  when(response) {
 *      ItemSuccess -> ...
 *      ListSuccess -> ...
 *      EmptySuccess -> ...
 *      Failure -> ...
 *  }
 * ```
 */
sealed class Response<out T : EmptyStateInfo> {

    /**
     * Represents a successful request with a single item.
     *
     * @property httpStatusCode Request status code for the API request.
     * @property item The single item returned in the response.*/
    data class ItemSuccess<out T : EmptyStateInfo>(
        val httpStatusCode: HttpStatusCode,
        val item: T
    ) : Response<T>()

    /**
     * Represents a successful request with a list of items.
     *
     * @property httpStatusCode Request status code for the API request.
     * @property items The list of items returned in the response.
     */
    data class ListSuccess<out T : EmptyStateInfo>(
        val httpStatusCode: HttpStatusCode,
        val items: List<T>
    ) : Response<T>()

    /**
     * Represents a successful request with an empty response.
     *
     * @property httpStatusCode Request status code for the API request.
     */
    data class EmptySuccess(
        val httpStatusCode: HttpStatusCode
    ) : Response<Nothing>()

    /**
     * Represents a failed request.
     *
     * @property errorItem A request error wrapped inside the [ErrorItem].
     */
    data class Failure<out T : EmptyStateInfo>(
        val errorItem: ErrorItem
    ) : Response<T>()
}
