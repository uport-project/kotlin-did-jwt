package me.uport.sdk.jwt

/**
 * Thrown when a JWT is invalid either because it is expired, not valid yet or the signature doesn't match
 */
open class InvalidJWTException(message: String, override val cause: Throwable? = null) :
    IllegalArgumentException(message, cause)

/**
 * Thrown when the a JWT does not seem to have the proper format
 */
open class JWTEncodingException(message: String, override val cause: Throwable? = null) :
    InvalidJWTException(message, cause)
