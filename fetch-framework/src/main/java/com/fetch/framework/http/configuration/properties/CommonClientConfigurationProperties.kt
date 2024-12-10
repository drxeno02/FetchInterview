package com.fetch.framework.http.configuration.properties

import com.fetch.framework.http.client.FetchApiClient

/**
 * Common configuration properties. This class prevents redundancy of common configuration
 * attributes. This is an extensible class for setting API client related properties and
 * configuration. A configuration collects information necessary to perform operations and
 * instantiate the [FetchApiClient].
 */
class CommonClientConfigurationProperties {

    /**
     * Base url is required for performing requests.
     *
     * <p>The base url is sanitized, so if a base url is set that does not include a protocol,
     * a protocol (https) will automatically be added. If a protocol already exists, no modifications
     * will occur. If you do not include the ending backslash, a backslash will be appended
     * to the end, otherwise no modifications will occur</p>
     *
     * @property baseUrl REQUIRED: Base url will be used to perform requests.
     */
    var baseUrl: String? = null
        set(value) {
            field = value.let {
                if (!it?.endsWith("/")!!) {
                    "$it/"
                } else {
                    it
                }
            }.let {
                if (it.startsWith("http://") || it.startsWith("https://")) {
                    it
                } else {
                    "https://$it"
                }
            }
        }
}
