package com.fetch.framework.http.utils

object Utils {

    /**
     * Helper function to check if the response is an array
     *
     * @param response String The response body.
     * @return Boolean True if the response is an array, otherwise false.
     */
    fun isArrayResponse(response: String): Boolean {
        return response.trim().startsWith("[")
    }
}
