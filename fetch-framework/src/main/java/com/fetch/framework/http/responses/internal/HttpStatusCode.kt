package com.fetch.framework.http.responses.internal

private val RANGE_SUCCESSFUL_CODE = 200..299

private const val MESSAGE_UNKNOWN_STATUS_CODE = "Unknown Status Code"

/**
 * Represents an HTTP status with code and message.
 *
 * @property code numeric value of the HTTP response status
 * @property message description of the status
 */
class HttpStatusCode private constructor(
    val code: Int,
    val message: String
) {

    /**
     * Returns `true` if the status code is considered successful according to HTTP standards.
     *
     * Codes from 200 to 299 are considered to be successful.
     */
    val isSuccessful: Boolean
        get() = code in RANGE_SUCCESSFUL_CODE

    override fun toString(): String = "$code $message"

    override fun equals(other: Any?) = other is HttpStatusCode && other.code == this.code

    override fun hashCode() = code.hashCode()

    companion object {

        /**
         * 2XX: Success. Indicates that the client's request was successfully received,
         * understood, and accepted.
         */
        val OK = HttpStatusCode(200, "OK")
        val Created = HttpStatusCode(201, "Created")
        val Accepted = HttpStatusCode(202, "Accepted")
        val NonAuthoritativeInformation = HttpStatusCode(203, "Non-Authoritative Information")
        val NoContent = HttpStatusCode(204, "No Content")
        val ResetContent = HttpStatusCode(205, "Reset Content")
        val PartialContent = HttpStatusCode(206, "Partial Content")
        val MultiStatus = HttpStatusCode(207, "Multi-Status")

        /**
         * 5XX: Server Error. Indicates that the server is aware that it has erred or is
         * incapable of performing the request.
         */
        val InternalServerError = HttpStatusCode(500, "Internal Server Error")
        val NotImplemented = HttpStatusCode(501, "Not Implemented")
        val BadGateway = HttpStatusCode(502, "Bad Gateway")
        val ServiceUnavailable = HttpStatusCode(503, "Service Unavailable")
        val GatewayTimeout = HttpStatusCode(504, "Gateway Timeout")
        val VersionNotSupported = HttpStatusCode(505, "HTTP Version Not Supported")
        val VariantAlsoNegotiates = HttpStatusCode(506, "Variant Also Negotiates")
        val InsufficientStorage = HttpStatusCode(507, "Insufficient Storage")

        private val allStatusCodes = getAllHttpStatuses()

        /**
         * Creates an instance of [HttpStatusCode] with the given numeric value. Returns
         * "Unknown Status Code" if [code] is not identified.
         *
         * @param code HTTP status code
         *
         * @return [HttpStatusCode] for corresponding [code]
         */
        fun fromStatusCode(
            code: Int,
            message: String = MESSAGE_UNKNOWN_STATUS_CODE
        ): HttpStatusCode {
            val knownStatus = allStatusCodes.firstOrNull { it.code == code }
            return knownStatus ?: HttpStatusCode(code, message)
        }
    }
}

internal fun getAllHttpStatuses() = listOf(
    HttpStatusCode.OK,
    HttpStatusCode.Created,
    HttpStatusCode.Accepted,
    HttpStatusCode.NonAuthoritativeInformation,
    HttpStatusCode.NoContent,
    HttpStatusCode.ResetContent,
    HttpStatusCode.PartialContent,
    HttpStatusCode.MultiStatus,
    HttpStatusCode.InternalServerError,
    HttpStatusCode.NotImplemented,
    HttpStatusCode.BadGateway,
    HttpStatusCode.ServiceUnavailable,
    HttpStatusCode.GatewayTimeout,
    HttpStatusCode.VersionNotSupported,
    HttpStatusCode.VariantAlsoNegotiates,
    HttpStatusCode.InsufficientStorage
)
