@file:Suppress("KDocUnresolvedReference", "EXPERIMENTAL_API_USAGE")

package me.uport.sdk.jwt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.json.JsonException
import me.uport.sdk.core.*
import me.uport.sdk.ethrdid.EthrDIDResolver
import me.uport.sdk.httpsdid.WebDIDResolver
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.JWTUtils.Companion.normalizeKnownDID
import me.uport.sdk.jwt.JWTUtils.Companion.splitToken
import me.uport.sdk.jwt.model.ArbitraryMapSerializer
import me.uport.sdk.jwt.model.JwtHeader
import me.uport.sdk.jwt.model.JwtHeader.Companion.ES256K
import me.uport.sdk.jwt.model.JwtHeader.Companion.ES256K_R
import me.uport.sdk.jwt.model.JwtPayload
import me.uport.sdk.signer.*
import me.uport.sdk.universaldid.DIDDocument
import me.uport.sdk.universaldid.DIDResolver
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.PublicKeyType.Companion.EcdsaPublicKeySecp256k1
import me.uport.sdk.universaldid.PublicKeyType.Companion.Secp256k1SignatureVerificationKey2018
import me.uport.sdk.universaldid.PublicKeyType.Companion.Secp256k1VerificationKey2018
import me.uport.sdk.universaldid.UniversalDID
import me.uport.sdk.uportdid.UportDIDResolver
import org.kethereum.crypto.toAddress
import org.kethereum.encodings.decodeBase58
import org.kethereum.extensions.toBigInteger
import org.kethereum.hashes.sha256
import org.kethereum.model.PUBLIC_KEY_SIZE
import org.kethereum.model.PublicKey
import org.walleth.khex.clean0xPrefix
import org.walleth.khex.hexToByteArray
import java.math.BigInteger
import java.security.SignatureException
import kotlin.math.floor

/**
 * Method signature for the verification methods.
 *
 * @param publicKeys : List<PublicKeyEntry> list of public key entries to be verified against the signature
 * @param signatureBytes : ByteArray the algorithm-specific encoded signature
 * @param signedData : ByteArray the blob of data that was signed
 *
 */
typealias VerificationMethod = (
    publicKeys: List<PublicKeyEntry>,
    signatureBytes: ByteArray,
    signedData: ByteArray
) -> Boolean

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
 * It defaults to `null`
 */
class JWTTools(
    private val timeProvider: ITimeProvider = SystemTimeProvider,
    preferredNetwork: EthNetwork? = null
) {
    init {

        // blank did declarations
        val blankUportDID = "did:uport:2nQs23uc3UN6BBPqGHpbudDxBkeDRn553BB"
        val blankEthrDID = "did:ethr:0x0000000000000000000000000000000000000000"
        val blankHttpsDID = "did:https:example.com"

        // register default Ethr DID resolver if Universal DID is unable to resolve blank Ethr DID
        if (!UniversalDID.canResolve(blankEthrDID)) {
            val defaultRPC = JsonRPC(preferredNetwork?.rpcUrl ?: Networks.mainnet.rpcUrl)
            val defaultRegistry = preferredNetwork?.ethrDidRegistry
                ?: Networks.mainnet.ethrDidRegistry
            UniversalDID.registerResolver(EthrDIDResolver(defaultRPC, defaultRegistry))
        }

        // register default Uport DID resolver if Universal DID is unable to resolve blank Uport DID
        if (!UniversalDID.canResolve(blankUportDID)) {
            val defaultRPC = JsonRPC(preferredNetwork?.rpcUrl ?: Networks.rinkeby.rpcUrl)
            UniversalDID.registerResolver(UportDIDResolver(defaultRPC))
        }

        // register default https DID resolver if Universal DID is unable to resolve blank https DID
        if (!UniversalDID.canResolve(blankHttpsDID)) {
            UniversalDID.registerResolver(WebDIDResolver())
        }
    }

    /**
     * This coroutine method creates a signed JWT from a [payload] Map and an abstracted [Signer]
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
    suspend fun createJWT(
        payload: Map<String, Any?>,
        issuerDID: String,
        signer: Signer,
        expiresInSeconds: Long = DEFAULT_JWT_VALIDITY_SECONDS,
        algorithm: String = ES256K_R
    ): String {

        val mutablePayload = payload.toMutableMap()

        val header = JwtHeader(alg = algorithm)

        val iatSeconds = floor(timeProvider.nowMs() / 1000.0).toLong()
        mutablePayload["iat"] = payload["iat"] ?: iatSeconds

        val expSeconds = iatSeconds + expiresInSeconds
        if (expiresInSeconds >= 0) {
            mutablePayload["exp"] = payload["exp"] ?: expSeconds
        } else {
            mutablePayload.remove("exp")
        }
        if (payload.containsKey("exp") && payload["exp"] == null) {
            mutablePayload.remove("exp")
        }

        mutablePayload["iss"] = issuerDID

        val serializedPayload = Json(JsonConfiguration.Stable)
            .stringify(ArbitraryMapSerializer, mutablePayload)

        @Suppress("SimplifiableCallChain", "ConvertCallChainIntoSequence")
        val signingInput = listOf(header.toJson(), serializedPayload)
            .map { it.toBase64UrlSafe() }
            .joinToString(".")

        val jwtSigner = JWTSignerAlgorithm(header)
        val signature: String = jwtSigner.sign(signingInput, signer)
        return listOf(signingInput, signature).joinToString(".")
    }

    /**
     * Decodes a jwt [token]
     * @param token is a string of 3 parts separated by .
     * @throws InvalidJWTException when the header or payload are empty or when they don't start with { (invalid json)
     * @return the JWT Header,Payload and signature as parsed objects
     */
    fun decode(token: String): Triple<JwtHeader, JwtPayload, ByteArray> {
        //Split token by . from jwtUtils
        val (encodedHeader, encodedPayload, encodedSignature) = splitToken(token)
        if (encodedHeader.isEmpty())
            throw InvalidJWTException("Header cannot be empty")
        else if (encodedPayload.isEmpty())
            throw InvalidJWTException("Payload cannot be empty")
        //Decode the pieces
        val headerString = String(encodedHeader.decodeBase64())
        val payloadString = String(encodedPayload.decodeBase64())
        val signatureBytes = encodedSignature.decodeBase64()

        try {
            //Parse Json
            val header = JwtHeader.fromJson(headerString)
            val payload = JwtPayload.fromJson(payloadString)
            return Triple(header, payload, signatureBytes)
        } catch (ex: JsonException) {
            throw JWTEncodingException("cannot parse the JWT($token)", ex)
        }
    }

    /**
     * Decodes a JWT into it's 3 components, keeping the payload as a Map type
     *
     * This is useful for situations where the known [JwtPayload] fields are not enough.
     */
    fun decodeRaw(token: String): Triple<JwtHeader, Map<String, Any?>, ByteArray> {
        //Split token by . from jwtUtils
        val (encodedHeader, encodedPayload, encodedSignature) = splitToken(token)
        if (encodedHeader.isEmpty())
            throw InvalidJWTException("Header cannot be empty")
        else if (encodedPayload.isEmpty())
            throw InvalidJWTException("Payload cannot be empty")
        //Decode the pieces
        val headerString = String(encodedHeader.decodeBase64())
        val payloadString = String(encodedPayload.decodeBase64())
        val signatureBytes = encodedSignature.decodeBase64()

        try {
            //Parse Json
            val header = JwtHeader.fromJson(headerString)

            val payload = Json.nonstrict.parse(ArbitraryMapSerializer, payloadString)

            return Triple(header, payload, signatureBytes)
        } catch (ex: JsonException) {
            throw JWTEncodingException("cannot parse the JWT($token)", ex)
        }
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
    suspend fun verify(
        token: String,
        auth: Boolean = false,
        audience: String? = null
    ): JwtPayload {
        val (header, payload, signatureBytes) = decode(token)

        if (payload.iat != null && payload.iat > (timeProvider.nowMs() / 1000 + TIME_SKEW)) {
            throw InvalidJWTException("Jwt not valid yet (issued in the future) iat: ${payload.iat}")
        }

        if (payload.exp != null && payload.exp <= (timeProvider.nowMs() / 1000 - TIME_SKEW)) {
            throw InvalidJWTException("JWT has expired: exp: ${payload.exp}")
        }

        if (payload.aud != null) {

            val payloadAudience = normalizeKnownDID(payload.aud)
            if (UniversalDID.canResolve(payloadAudience)) {

                if (audience == null) {
                    throw InvalidJWTException(
                        "JWT audience is required but your app address has not been configured. " +
                                "You can provide the proper app address using the `audience` parameter when verifying."
                    )
                }

                if (audience != payloadAudience) {
                    throw InvalidJWTException(
                        "JWT audience does not match your DID. " +
                                "aud: $payloadAudience != yours: $audience"
                    )
                }
            }
        }

        val publicKeys = resolveAuthenticator(header.alg, payload.iss, auth)

        val signingInputBytes = token.substringBeforeLast('.').toByteArray(utf8)

        val signatureIsValid = verificationMethod[header.alg]
            ?.invoke(publicKeys, signatureBytes, signingInputBytes)
            ?: throw JWTEncodingException("JWT algorithm ${header.alg} not supported")

        if (signatureIsValid) {
            return payload
        } else {
            throw InvalidJWTException(
                "Signature invalid for JWT. DID document for ${payload.iss} does not have any " +
                        "matching public keys"
            )
        }
    }

    /**
     * maps known algorithms to the corresponding verification method
     */
    private val verificationMethod: Map<String, VerificationMethod> = mapOf(
        ES256K_R to ::verifyRecoverableES256K,
        ES256K to ::verifyES256K
    )

    private fun verifyES256K(
        publicKeys: List<PublicKeyEntry>,
        signatureBytes: ByteArray,
        signingInputBytes: ByteArray
    ): Boolean {

        val sigData = signatureBytes.decodeJose()

        val messageHash = signingInputBytes.sha256()

        val matches = publicKeys.map { pubKeyEntry ->

            val pkBytes = pubKeyEntry.publicKeyHex?.hexToByteArray()
                ?: pubKeyEntry.publicKeyBase64?.decodeBase64()
                ?: pubKeyEntry.publicKeyBase58?.decodeBase58()
                ?: ByteArray(PUBLIC_KEY_SIZE)
            PublicKey(pkBytes.toBigInteger()).normalize()

        }.filter { publicKey ->
            try {
                ecVerify(messageHash, sigData, publicKey)
            } catch (ex: IllegalArgumentException) {
                false
            }
        }

        fun hasEthereumAddressKeys(publicKeys: List<PublicKeyEntry>): Boolean {
            return publicKeys.any { it.ethereumAddress != null }
        }

        if (matches.isEmpty() && hasEthereumAddressKeys(publicKeys)) {
            return verifyRecoverableES256K(publicKeys, signatureBytes, signingInputBytes)
        } else {
            return matches.isNotEmpty()
        }

    }

    private fun verifyRecoverableES256K(
        publicKeys: List<PublicKeyEntry>,
        signatureBytes: ByteArray,
        signingInputBytes: ByteArray
    ): Boolean {

        val signatures = if (signatureBytes.size == SIG_RECOVERABLE_SIZE) {
            listOf(signatureBytes.decodeJose())
        } else {
            listOf(
                signatureBytes.decodeJose(0),
                signatureBytes.decodeJose(1)
            )
        }

        val recoveredAddresses = signatures.map { signature ->
            try {
                signedJwtToKey(signingInputBytes, signature)
            } catch (e: SignatureException) {
                BigInteger.ZERO
            }
        }.map { pubKey ->
            val pubKeyNoPrefix = PublicKey(pubKey).normalize()
            pubKeyNoPrefix.toAddress().cleanHex.toLowerCase()
        }

        val matches = publicKeys.map { pubKeyEntry ->

            val pkBytes = pubKeyEntry.publicKeyHex?.hexToByteArray()
                ?: pubKeyEntry.publicKeyBase64?.decodeBase64()
                ?: pubKeyEntry.publicKeyBase58?.decodeBase58()
                ?: ByteArray(PUBLIC_KEY_SIZE)
            val pubKey = PublicKey(pkBytes.toBigInteger()).normalize()

            (pubKeyEntry.ethereumAddress?.clean0xPrefix() ?: pubKey.toAddress().cleanHex)

        }.filter { ethereumAddress ->
            ethereumAddress.toLowerCase() in recoveredAddresses
        }

        return matches.isNotEmpty()
    }

    /**
     * This method obtains a [DIDDocument] corresponding to the [issuer] and returns a list of [PublicKeyEntry]
     * that can be used to check JWT signatures
     *
     * @param [auth] decide if the returned list should also be filtered against the `authentication`
     * entries in the DIDDocument
     *
     */
    internal suspend fun resolveAuthenticator(alg: String, issuer: String, auth: Boolean): List<PublicKeyEntry> {

        if (alg !in verificationMethod.keys) {
            throw JWTEncodingException("JWT algorithm '$alg' not supported")
        }

        val doc: DIDDocument = UniversalDID.resolve(issuer)

        val authenticationKeys: List<String> = if (auth) {
            doc.authentication.map { it.publicKey }
        } else {
            emptyList() // return an empty list
        }

        val authenticators = doc.publicKey.filter {

            // filter public keys which belong to the list of supported key types
            supportedKeyTypes.contains(it.type) && (!auth || (authenticationKeys.contains(it.id)))
        }

        if (auth && (authenticators.isEmpty())) throw InvalidJWTException("DID document for $issuer does not have public keys suitable for authenticating user")
        if (authenticators.isEmpty()) throw InvalidJWTException("DID document for $issuer does not have public keys for $alg")

        return authenticators
    }

    companion object {

        /**
         * 5 minutes. The default number of seconds of validity of a JWT, in case no other interval is specified.
         */
        const val DEFAULT_JWT_VALIDITY_SECONDS = 300L

        private const val TIME_SKEW = 300L

        /**
         * List of supported key types for verifying DID JWT signatures
         */
        val supportedKeyTypes = listOf(
            Secp256k1VerificationKey2018,
            Secp256k1SignatureVerificationKey2018,
            EcdsaPublicKeySecp256k1
        )
    }

}

