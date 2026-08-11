package com.lifetrace.order.domain

enum class PlatformErrorCode {
    AUTH_REQUIRED,
    VERIFICATION_REQUIRED,
    RATE_LIMITED,
    ACCESS_DENIED,
    SOURCE_UNAVAILABLE,
    PARSE_FAILED,
    NORMALIZE_FAILED,
    CANCELLED,
    UNKNOWN,
}

class PlatformFailure(
    val code: PlatformErrorCode,
    override val message: String,
    val retryable: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause)
