package com.fetch.framework.http.responses.internal

/**
 * Intended to act as a layer of abstraction over the different types of responses..
 *
 * <p>The /hiring.json request returns a List of objects. Using Generics, we can use
 * ResponseData to represent different types of responses that include Lists of any type
 * and even objects.</p>
 */
sealed class ResponseData<out T : EmptyStateInfo> {
    data class ItemResponse<out T : EmptyStateInfo>(val anyObject: T) : ResponseData<T>()
    data class ListResponse<out T : EmptyStateInfo>(val anyList: List<T>) : ResponseData<T>()
    data object EmptyResponse : ResponseData<Nothing>()
}
