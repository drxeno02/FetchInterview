package com.fetch.framework.http.configuration

import com.fetch.framework.http.client.FetchApiClient
import com.fetch.framework.http.configuration.base.BaseClientConfiguration
import com.fetch.framework.http.configuration.properties.CommonClientConfigurationProperties

// exceptions
internal const val ILLEGAL_ARGUMENT_EXCEPTION_BASE_URL =
    "Failed to read `baseUrl`. This field can not be empty."

/**
 * Client configuration properties used for [FetchApiClient]. This is an extensible class for
 * setting API client related properties and configuration. A configuration collects information
 * necessary to perform operations and instantiate the [FetchApiClient].
 *
 * @property commonConfiguration Common configuration properties.
 * @constructor
 */
data class ClientConfiguration(
    private val commonConfiguration: CommonClientConfigurationProperties?
) : BaseClientConfiguration() {

    /**
     * Base url is required for performing requests.
     *
     * <p>The base url is sanitized, so if a base url is set that does not include a protocol,
     * a protocol (https) will automatically be added. If a protocol already exists, no modifications
     * will occur. If you do not include the ending backslash, a backslash will be appended
     * to the end, otherwise no modifications will occur.</p>
     *
     * @property baseUrl REQUIRED: Base url will be used to perform requests.
     */
    override var baseUrl: String?
        get() = commonConfiguration?.baseUrl
        set(value) {
            commonConfiguration?.baseUrl = value
            // rebuild URLs
            updateURLs()
        }

    // build encapsulated urls
    internal var itemsUrl = "$baseUrl$PATH_ITEMS"

    /**
     * Update URLs after [ClientConfiguration] has been initialized and the [baseUrl]
     * has been changed. Need to reinitialize URL variables.
     */
    private fun updateURLs() {
        itemsUrl = "$baseUrl$PATH_ITEMS"
    }

    companion object {
        // encapsulated url paths
        private const val PATH_ITEMS = "hiring.json"
    }

    /**
     * Builder pattern is a creational design pattern. It means it solves problems
     * related to object creation.
     *
     * <p>Builder pattern is used to create instance of very complex object having telescoping
     * constructor in easiest way.</p>
     *
     * @property baseUrl Base url to perform necessary requests.
     *
     * @constructor
     */
    data class Builder(
        private var baseUrl: String? = null
    ) {

        /**
         * Setter for setting base url.
         *
         * NOTE: You do not need to provide the full url. Only the base url is necessary.
         *
         * @param baseUrl REQUIRED: Base url to perform necessary requests.
         * @return [ClientConfiguration.Builder]
         */
        fun setBaseUrl(baseUrl: String) = apply {
            this.baseUrl = baseUrl
        }

        /**
         * Create the [ClientConfiguration] object.
         * Will throw [IllegalArgumentException] if required attributes aren't set.
         * REQUIRED: [baseUrl]
         * @return [ClientConfiguration]
         * @throws [IllegalArgumentException]
         */
        @Throws(IllegalArgumentException::class)
        fun create(): ClientConfiguration {
            when {
                baseUrl.isNullOrEmpty() -> throw IllegalArgumentException(
                    ILLEGAL_ARGUMENT_EXCEPTION_BASE_URL
                )

                else -> {
                    val commonConfiguration = CommonClientConfigurationProperties()
                    commonConfiguration.baseUrl = baseUrl
                    return ClientConfiguration(
                        commonConfiguration = commonConfiguration
                    )
                }
            }
        }
    }
}
