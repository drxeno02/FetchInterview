package com.fetch.framework.http.responses.internal

/**
 * Provide [isEmpty] DSL for responses.
 */
interface EmptyStateInfo {

    /**
     * Returns `true` if response is empty.
     */
    fun isEmpty(): Boolean
}
