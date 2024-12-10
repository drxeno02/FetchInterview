package com.fetch.framework.http.provider

import com.fetch.framework.http.client.FetchApiClient
import com.fetch.framework.http.client.interfaces.FetchApiClientInterfaces
import com.fetch.framework.http.configuration.ClientConfiguration

private const val DESTROY_METHOD_JVM_NAME = "destroy"
internal const val ERROR_INSTANCE_ALREADY_INITIALIZED =
    "Start using the #getInstance() method since FetchApiClient has already been initialized."
internal const val ERROR_INSTANCE_NOT_INITIALIZED =
    "Initialize the [FetchApiClientProvider] first."

/**
 * A `singleton` instance provider for [FetchApiClientInterfaces]. The client should call
 * {@linkplain #initialize()} to initialize the provider first before using {@linkplain #getInstance()}.
 *
 * @see [FetchApiClientInterfaces]
 * @see [FetchApiClient]
 */
object FetchApiClientProvider {

    @Volatile
    private var INSTANCE: FetchApiClientInterfaces? = null

    /**
     * Initialize [FetchApiClient].
     *
     * @property clientConfiguration Contains necessary configuration data for making requests.
     */
    @JvmStatic
    @Throws(IllegalStateException::class)
    fun initialize(
        clientConfiguration: ClientConfiguration
    ): FetchApiClientInterfaces {
        if (INSTANCE == null) {
            synchronized(this) {
                // Assign the instance to local variable to check if it was initialized
                // by some other thread.
                // While current thread was blocked to enter the locked zone. If it was
                // initialized then we can return.
                val localInstance = INSTANCE
                if (localInstance == null) {
                    INSTANCE = FetchApiClient(
                        clientConfiguration = clientConfiguration
                    )
                }
            }
            return INSTANCE ?: throw IllegalStateException(ERROR_INSTANCE_NOT_INITIALIZED)
        } else {
            throw IllegalStateException(ERROR_INSTANCE_ALREADY_INITIALIZED)
        }
    }

    /**
     * Return a `singleton` instance of [FetchApiClientInterfaces].
     */
    @JvmStatic
    fun getInstance(): FetchApiClientInterfaces {
        return INSTANCE ?: throw IllegalStateException(ERROR_INSTANCE_NOT_INITIALIZED)
    }

    /**
     * ONLY FOR TESTING PURPOSES. Reset [INSTANCE] as null to help with unit tests.
     */
    @Synchronized
    @JvmStatic
    @JvmName(DESTROY_METHOD_JVM_NAME)
    internal fun destroy() {
        INSTANCE = null
    }
}
