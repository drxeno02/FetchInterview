package com.fetch.framework.http.okhttp.internal

import com.fetch.framework.http.interfaces.HttpResponseCallback
import okhttp3.Call

/**
 * Potential states for a HTTP request.
 */
enum class RequestState {
    Ongoing,
    Cancelled,
    Failed,
    Successful
}

/**
 * Outlines a strategy centered around changes in [RequestState].
 */
abstract class RequestCleanupSpec {

    internal var ongoingCall: Call? = null
    internal var callback: HttpResponseCallback? = null
    internal var currentRequestState: RequestState? = null

    /**
     * Called when the HTTP request is either [RequestState.Successful], [RequestState.Failed] or
     * [RequestState.Cancelled].
     *
     * @param state [RequestState] of a HTTP request.
     */
    open fun onStateChanged(state: RequestState) {
        currentRequestState = state
    }

    fun releaseResources() {
        ongoingCall = null
        callback = null
    }
}

/**
 * A resource cleanup/release strategy for HTTP requests. Prevents memory leaks
 * by [releaseResources] as results are communicated via [HttpResponseCallback] which will
 * hold references to an activity or fragment.
 */
internal class RequestCleanupStrategy : RequestCleanupSpec() {

    override fun onStateChanged(state: RequestState) {
        super.onStateChanged(state)
        when (currentRequestState) {
            RequestState.Cancelled, RequestState.Failed, RequestState.Successful -> {
                releaseResources()
            }

            else -> {
                // no op
            }
        }
    }
}
