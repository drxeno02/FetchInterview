package com.fetch.framework.http.responses.internal

import com.fetch.framework.http.responses.internal.Response.EmptySuccess
import com.fetch.framework.http.responses.internal.Response.Failure
import com.fetch.framework.http.responses.internal.Response.ItemSuccess
import com.fetch.framework.http.responses.internal.Response.ListSuccess

/**
 * Response callback for requests. Each request could finish in the following state:
 * - [ItemSuccess]: Denotes that an API call finished successfully; for handling standard responses
 * that are objects.
 * - [ListSuccess]: Denotes that an API call finished successfully; for handling responses with
 * lists as the root.
 * - [EmptySuccess]: Denotes that an API call finished successfully; for handling responses that
 * are completely empty.
 * - [Failure]: An API call failed during execution; probable errors are categorized as a
 * part of [ErrorItem].
 */
interface ResponseCallback<T : EmptyStateInfo> {

    /**
     * Represents that a request concluded successfully with a single item.
     *
     * @param data A successful response with a single item.
     */
    fun onItemSuccess(data: ItemSuccess<T>)

    /**
     * Represents that a request concluded successfully with a list of items.
     *
     * @param data A successful response with a list of items.
     */
    fun onListSuccess(data: ListSuccess<T>)

    /**
     * Represents that a request concluded successfully with an empty response.
     *
     * @param data A successful response with no content.
     */
    fun onEmptySuccess(data: EmptySuccess)

    /**
     * Represents that a request failed.
     *
     *@param data A failed response with error information.
     */
    fun onFailure(data: Failure<T>)
}
