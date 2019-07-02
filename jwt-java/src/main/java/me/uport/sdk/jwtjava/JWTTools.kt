package me.uport.sdk.jwtjava

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import me.uport.sdk.core.EthNetwork
import me.uport.sdk.core.ITimeProvider
import me.uport.sdk.core.SystemTimeProvider
import me.uport.sdk.jwt.InvalidJWTException
import me.uport.sdk.jwt.JWTTools as KotlinJWTTools
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.jwt.model.JwtPayload
import me.uport.sdk.signer.Signer
import me.uport.sdk.universaldid.DIDResolver
import me.uport.sdk.universaldid.UniversalDID
import java.util.concurrent.CompletableFuture

/**
 * Tools for Verifying, Creating, and Decoding uport JWTs
 *
 * @param timeProvider an [ITimeProvider] that can be used for "was valid at ..." verifications
 * or to emit short lived tokens for future use.
 * It defaults to a [SystemTimeProvider]
 *
 * @param preferredNetwork an [EthNetwork] that can be used to initialize [DIDResolver]s that are
 * potentially missing from the [UniversalDID] resolver.
 * **This does not take effect if resolvers are already registered.**
 * If this param is `null`, then default networks will be used
 * (`mainnet` for Ethr DID and `rinkeby` for uPort DID ).
 */
class JWTTools(
    private val timeProvider: ITimeProvider = SystemTimeProvider,
    private val preferredNetwork: EthNetwork? = null
) {

    private val wrappedJWTTools = KotlinJWTTools(timeProvider, preferredNetwork)

    /**
     * This method creates a signed JWT from a [payload] Map and an abstracted [Signer]
     * You're also supposed to pass the [issuerDID] and can configure the algorithm used and expiry time
     *
     * @param payload a map containing the fields forming the payload of this JWT
     * @param issuerDID a DID string that will be set as the `iss` field in the JWT payload.
     *                  The signature produced by the signer should correspond to this DID.
     *                  If the `iss` field is already part of the [payload], that will get overwritten.
     *                  **The [issuerDID] is NOT checked for format, nor for a match with the signer.**
     * @param signer a [Signer] that will produce the signature section of this JWT.
     *                  The signature should correspond to the [issuerDID].
     * @param expiresInSeconds number of seconds of validity of this JWT. You may omit this param if
     *                  an `exp` timestamp is already part of the [payload].
     *                  If there is no `exp` field in the payload and the param is not specified,
     *                  it defaults to [DEFAULT_JWT_VALIDITY_SECONDS]
     *                  If this param is negative or if `payload["exp"]` is explicitly set to `null`,
     *                  the resulting JWT will not have an `exp` field
     * @param algorithm defaults to `ES256K-R`. The signing algorithm for this JWT.
     *                  Supported types are `ES256K` for uport DID and `ES256K-R` for ethr-did and the rest
     *
     */
    fun createJWT(
        payload: Map<String, Any?>,
        issuerDID: String,
        signer: Signer,
        expiresInSeconds: Long = KotlinJWTTools.DEFAULT_JWT_VALIDITY_SECONDS,
        algorithm: String = JwtHeader.ES256K_R
    ): CompletableFuture<String> =
        GlobalScope.future {
            wrappedJWTTools.createJWT(
                payload,
                issuerDID,
                signer,
                expiresInSeconds,
                algorithm
            )
        }

    /**
     * Verifies a jwt [token]
     * @param token the jwt token to be verified
     * @param auth if this param is `true` the public key list that this token is checked against is filtered to the
     *          entries in the `authentication` entries in the DID document of the issuer of the token.
     * @param audience the audience that should be able to verify this token. This is usually a DID but can also be a
     *          callback URL for situations where the token represents a response or a bearer token.
     * @throws InvalidJWTException when the current time is not within the time range of payload `iat` and `exp`
     *          , when no public key matches are found in the DID document
     *          , when the `audience` does not match the intended audience (`aud` field)
     * @return a [JwtPayload] if the verification is successful and `null` if it fails
     */
    fun verify(
        token: String,
        auth: Boolean = false,
        audience: String? = null
    ): CompletableFuture<JwtPayload> =
        GlobalScope.future {
            wrappedJWTTools.verify(token, auth, audience)
        }

    /**
     * Decodes a jwt [token] into a Triple (header, payload, signature), where the payload is converted to a
     * Map object wth all the corresponding JSON fields.
     *
     * @param token is a string of 3 parts separated by .
     * @throws InvalidJWTException when the header or payload are empty or when they don't start with { (invalid json)
     * @return the JWT Header,Payload and signature as parsed objects
     */
    fun decodeRaw(token: String): Triple<JwtHeader, Map<String, Any?>, ByteArray> =
        wrappedJWTTools.decodeRaw(token)

    /**
     * Decodes a jwt [token] into a Triple (header, payload, signature), where the payload is converted to a
     * [JwtPayload] object with the known JWT fields.
     *
     * @param token is a string of 3 parts separated by .
     * @throws InvalidJWTException when the header or payload are empty or when they don't start with { (invalid json)
     * @return the JWT Header,Payload and signature as parsed objects
     */
    fun decode(token: String): Triple<JwtHeader, JwtPayload, ByteArray> =
        wrappedJWTTools.decode(token)
}
