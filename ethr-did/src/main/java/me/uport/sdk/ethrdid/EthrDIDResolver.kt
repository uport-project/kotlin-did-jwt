@file:Suppress("LongMethod", "ComplexMethod")

package me.uport.sdk.ethrdid

import me.uport.sdk.core.EthNetwork
import me.uport.sdk.core.ITimeProvider
import me.uport.sdk.core.SystemTimeProvider
import me.uport.sdk.core.hexToBigInteger
import me.uport.sdk.core.hexToByteArray
import me.uport.sdk.core.prepend0xPrefix
import me.uport.sdk.core.toBase64
import me.uport.sdk.ethrdid.EthereumDIDRegistry.Events.DIDAttributeChanged
import me.uport.sdk.ethrdid.EthereumDIDRegistry.Events.DIDDelegateChanged
import me.uport.sdk.ethrdid.EthereumDIDRegistry.Events.DIDOwnerChanged
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jsonrpc.model.exceptions.JsonRpcException
import me.uport.sdk.signer.Signer
import me.uport.sdk.signer.bytes32ToString
import me.uport.sdk.signer.hexToBytes32
import me.uport.sdk.signer.utf8
import me.uport.sdk.universaldid.AuthenticationEntry
import me.uport.sdk.universaldid.DIDResolver
import me.uport.sdk.universaldid.DidResolverError
import me.uport.sdk.universaldid.PublicKeyEntry
import me.uport.sdk.universaldid.PublicKeyType
import me.uport.sdk.universaldid.PublicKeyType.Companion.Secp256k1SignatureAuthentication2018
import me.uport.sdk.universaldid.PublicKeyType.Companion.Secp256k1VerificationKey2018
import me.uport.sdk.universaldid.PublicKeyType.Companion.sigAuth
import me.uport.sdk.universaldid.PublicKeyType.Companion.veriKey
import me.uport.sdk.universaldid.ServiceEntry
import org.kethereum.extensions.toHexStringNoPrefix
import org.komputing.kbase58.encodeToBase58String
import org.komputing.khex.extensions.toHexString
import pm.gnosis.model.Solidity
import java.math.BigInteger
import java.util.*

/**
 * This is a DID resolver implementation that supports the "ethr" DID method.
 * It accepts ethr-dids or simple ethereum addresses and produces a document described at:
 * https://w3c-ccg.github.io/did-spec/#did-documents
 *
 * Example ethr did: "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a"
 */
open class EthrDIDResolver : DIDResolver {

    private val _registryMap: RegistryMap
    private val _timeProvider: ITimeProvider

    private constructor(registryMap: RegistryMap, clock: ITimeProvider) {
        _timeProvider = clock
        this._registryMap = registryMap
    }

    @Deprecated(
        "Constructing the resolver directly has been deprecated " +
                "in favor of the Builder pattern that can supply multi-network configurations." +
                "This will be removed in the next major release.",
        ReplaceWith(
            """EthrDIDResolver.Builder().addNetwork(EthrDIDNetwork("", registryAddress, rpc, "0x1")).build()""",
            "me.uport.sdk.ethrdid.EthrDIDResolver.Companion.DEFAULT_REGISTRY_ADDRESS"
        )
    )
    constructor(
        rpc: JsonRPC,
        registryAddress: String = DEFAULT_REGISTRY_ADDRESS,
        timeProvider: ITimeProvider = SystemTimeProvider
    ) {
        val net = EthrDIDNetwork(DEFAULT_NETWORK_NAME, registryAddress, rpc, "0x1")
        this._timeProvider = timeProvider
        this._registryMap = RegistryMap().registerNetwork(net)
    }

    override val method = "ethr"

    override fun canResolve(potentialDID: String): Boolean {
        //if it can be normalized, then it matches either an ethereum address or a full ethr-did
        val did = normalizeDid(potentialDID)
        val network = extractNetwork(did)
        return did.isNotBlank() && _registryMap.getOrNull(network) != null
    }

    /**
     * Resolves a given ethereum address or DID string into a corresponding [EthrDIDDocument]
     */
    override suspend fun resolve(did: String): EthrDIDDocument {
        val networkIdentifier = extractNetwork(did).run {
            if (this.isBlank()) DEFAULT_NETWORK_NAME else this
        }

        val ethNetworkConfig = try {
            _registryMap[networkIdentifier]
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Missing registry configuration for `$networkIdentifier`. To resolve " +
                        "did:ethr:$networkIdentifier:0x... you need to register an `EthrDIDNetwork` " +
                        "in the EthrDIDResolver.Builder"
            )
        }

        val rpc = ethNetworkConfig.rpc
        val registryAddress = ethNetworkConfig.registryAddress

        require(registryAddress.isNotBlank()) {
            "The registry address configured for network `$networkIdentifier` is blank."
        }

        val normalizedDid = normalizeDid(did)
        val identityAddress = extractAddress(normalizedDid)
        val ethrDidContract = EthrDID(identityAddress, rpc, registryAddress, Signer.blank)
        val owner = ethrDidContract.lookupOwner(false)
        val history = getHistory(identityAddress, rpc, registryAddress)
        return wrapDidDocument(normalizedDid, owner, history)
    }

    /**
     * Obtains the block number when the given identity was last changed, or [BigInteger.ZERO] if
     * no change was ever made
     *
     * @hide
     */
    internal suspend fun lastChanged(
        identity: String,
        rpc: JsonRPC,
        registryAddress: String
    ): String {
        val encodedCall =
            EthereumDIDRegistry.Changed.encode(Solidity.Address(identity.hexToBigInteger()))
        return try {
            rpc.ethCall(registryAddress, encodedCall)
        } catch (err: JsonRpcException) {
            throw DidResolverError(
                "Unable to evaluate when or if the $identity was lastChanged because RPC" +
                        " endpoint responded with an error",
                err
            )
        }
    }

    /**
     * Builds a simple_list of events associated with the [identity] in the ether-did-registry
     * contract that resides at [registryAddress]
     *
     * Since the Event classes are generated by bivrost-kotlin, they don't have a specific type
     * so the simple_list id of type [Any]
     *
     * @hide
     */
    @Suppress("TooGenericExceptionCaught")
    internal suspend fun getHistory(
        identity: String,
        rpc: JsonRPC,
        registryAddress: String
    ): List<Any> {
        val lastChangedQueue: Queue<BigInteger> = PriorityQueue()
        val events = emptyList<Any>().toMutableList()
        lastChangedQueue.add(lastChanged(identity, rpc, registryAddress).hexToBigInteger())
        do {
            val lastChange = lastChangedQueue.poll() ?: break
            val logs = rpc.getLogs(
                registryAddress,
                listOf(null, identity.hexToBytes32()),
                lastChange,
                lastChange
            )
            logs.forEach {
                val topics: List<String> = it.topics
                val data: String = it.data

                try {
                    val event = DIDOwnerChanged.decode(topics, data)
                    lastChangedQueue.add(event.previouschange.value)
                    events.add(event)
                } catch (err: Exception) { /*nop*/
                }

                try {
                    val event = DIDAttributeChanged.decode(topics, data)
                    lastChangedQueue.add(event.previouschange.value)
                    events.add(event)
                } catch (err: Exception) { /*nop*/
                }

                try {
                    val event = DIDDelegateChanged.decode(topics, data)
                    lastChangedQueue.add(event.previouschange.value)
                    events.add(event)
                } catch (err: Exception) { /*nop*/
                }

            }

        } while (lastChange != BigInteger.ZERO)

        return events
    }

    /**
     * Wraps previously gathered info into a [EthrDIDDocument]
     *
     * @hide
     */
    internal fun wrapDidDocument(
        ownerDID: String,
        ownerAddress: String,
        history: List<Any>
    ): EthrDIDDocument {

        val pkEntries = mapOf<String, PublicKeyEntry>().toMutableMap().apply {
            put(
                "owner", PublicKeyEntry(
                    id = "$ownerDID#owner",
                    type = Secp256k1VerificationKey2018,
                    owner = ownerDID,
                    ethereumAddress = ownerAddress
                )
            )

        }
        val authEntries = mapOf<String, AuthenticationEntry>().toMutableMap().apply {
            put(
                "owner", AuthenticationEntry(
                    type = Secp256k1SignatureAuthentication2018,
                    publicKey = "$ownerDID#owner"
                )
            )
        }
        val serviceEntries = mapOf<String, ServiceEntry>().toMutableMap()

        var delegateCount = 0

        history.forEach { event ->
            when (event) {
                is DIDDelegateChanged.Arguments -> {
                    val (pk, auth) = processDelegateChanged(event, delegateCount, ownerDID)
                    pkEntries.putAll(pk)
                    authEntries.putAll(auth)
                    delegateCount += pk.size
                }

                is DIDAttributeChanged.Arguments -> {
                    val (pk, services) = processAttributeChanged(event, delegateCount, ownerDID)
                    pkEntries.putAll(pk)
                    serviceEntries.putAll(services)
                    delegateCount += pk.size
                }
            }
        }

        return EthrDIDDocument(
            id = ownerDID,
            publicKey = pkEntries.values.toList(),
            authentication = authEntries.values.toList(),
            service = serviceEntries.values.toList()
        )
    }

    @Suppress("MagicNumber", "ReturnCount")
    internal fun processAttributeChanged(
        event: DIDAttributeChanged.Arguments,
        delegateCount: Int,
        normalizedDid: String
    ): Pair<Map<String, PublicKeyEntry>, Map<String, ServiceEntry>> {
        val pkEntries = mapOf<String, PublicKeyEntry>().toMutableMap()
        val serviceEntries = mapOf<String, ServiceEntry>().toMutableMap()

        var delegateIndex = delegateCount
        val validTo = event.validto.value.toLong()
        if (validTo < _timeProvider.nowMs() / 1000L) {
            return (pkEntries to serviceEntries)
        }
        val name = event.name.byteArray.bytes32ToString().replace("\u0000", "")
        val key = "DIDAttributeChanged-$name-${event.value.items.toHexString()}"

        //language=RegExp
        val regex = """^did/(pub|auth|svc)/(\w+)(/(\w+))?(/(\w+))?$""".toRegex()
        val matchResult = regex.matchEntire(name)
            ?: return (pkEntries to serviceEntries)
        val (section, algo, _, rawType, _, encoding)
                = matchResult.destructured
        val type = parseType(algo, rawType)

        when (section) {

            "pub" -> {
                delegateIndex++
                val pk = PublicKeyEntry(
                    id = "$normalizedDid#delegate-$delegateIndex",
                    type = type,
                    owner = normalizedDid
                )

                pkEntries[key] = when (encoding) {
                    "", "null", "hex" ->
                        pk.copy(publicKeyHex = event.value.items.toHexString())
                    "base64" ->
                        pk.copy(publicKeyBase64 = event.value.items.toBase64())
                    "base58" ->
                        pk.copy(
                            publicKeyBase58 = event.value.items.toString(utf8).hexToByteArray()
                                .encodeToBase58String()
                        )
                    else ->
                        pk.copy(value = event.value.items.toHexString())
                }

            }

            "svc" -> {
                serviceEntries[key] = ServiceEntry(
                    type = algo,
                    serviceEndpoint = event.value.items.toString(utf8)
                )
            }
        }
        return (pkEntries to serviceEntries)
    }

    @Suppress("StringLiteralDuplication")
    private fun processDelegateChanged(
        event: DIDDelegateChanged.Arguments,
        delegateCount: Int,
        ownerDID: String
    ):
            Pair<MutableMap<String, PublicKeyEntry>, MutableMap<String, AuthenticationEntry>> {

        val pkEntries = mapOf<String, PublicKeyEntry>().toMutableMap()
        val authEntries = mapOf<String, AuthenticationEntry>().toMutableMap()

        var delegateIndex = delegateCount
        val delegateType = event.delegatetype.bytes.toString(utf8).replace("\u0000", "")
        val delegate = event.delegate.value.toHexStringNoPrefix().prepend0xPrefix()
        val key = "DIDDelegateChanged-$delegateType-$delegate"
        val validTo = event.validto.value.toLong()

        @Suppress("MagicNumber")
        if (validTo >= _timeProvider.nowMs() / 1000L) {
            delegateIndex++

            when (delegateType) {
                Secp256k1SignatureAuthentication2018.name,
                sigAuth.name -> authEntries[key] = AuthenticationEntry(
                    type = Secp256k1SignatureAuthentication2018,
                    publicKey = "$ownerDID#delegate-$delegateIndex"
                )

                Secp256k1VerificationKey2018.name,
                veriKey.name -> pkEntries[key] = PublicKeyEntry(
                    id = "$ownerDID#delegate-$delegateIndex",
                    type = Secp256k1VerificationKey2018,
                    owner = ownerDID,
                    ethereumAddress = delegate
                )
            }
        }
        return (pkEntries to authEntries)
    }

    companion object {
        const val DEFAULT_REGISTRY_ADDRESS = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b"

        private val attrTypes = mapOf(
            sigAuth.name to "SignatureAuthentication2018",
            veriKey.name to "VerificationKey2018"
        )

        private fun parseType(algo: String, rawType: String): PublicKeyType {
            var type = if (rawType.isBlank()) veriKey.name else rawType
            type = attrTypes[type] ?: type
            return PublicKeyType("$algo$type") //will throw exception if none found
        }

        //language=RegExp
        private val identityExtractPattern = "^did:ethr:((\\w+):)?(0x[0-9a-fA-F]{40})".toRegex()

        internal fun extractAddress(normalizedDid: String): String = identityExtractPattern
            .find(normalizedDid)
            ?.destructured?.component3() ?: ""

        internal fun extractNetwork(normalizedDid: String): String = identityExtractPattern
            .find(normalizedDid)
            ?.destructured?.component2() ?: ""

        //language=RegExp
        private val didParsePattern =
            "^(did:)?((\\w+):)?((\\w+):)?((0x)([0-9a-fA-F]{40}))".toRegex()

        @Suppress("ReturnCount")
        internal fun normalizeDid(did: String): String {
            val matchResult = didParsePattern.find(did) ?: return ""
            val (didHeader, _, didType, _, network, _, _, hexDigits) = matchResult.destructured
            if (didType.isNotBlank() && didType != "ethr") {
                //should forward to another resolver
                return ""
            }
            if (didHeader.isBlank() && didType.isNotBlank()) {
                //doesn't really look like a did if it only specifies type and not "did:"
                return ""
            }
            return if (network.isBlank() || network in listOf("mainnet", "0x1", "0x01"))
                "did:ethr:0x$hexDigits"
            else
                "did:ethr:$network:0x$hexDigits"
        }

        private const val DEFAULT_NETWORK_NAME = "" //empty string

    }

    /**
     * Builds an [EthrDIDResolver]
     * This class allows configuration of multiple ethereum networks that this resolver can access.
     */
    class Builder {
        private var _clock: ITimeProvider? = null
        private val _networks = emptyList<EthrDIDNetwork>().toMutableList()

        /**
         * Allows the use of a different clock than the system time.
         * This is usable for "was valid at" type of queries, and for deterministic checks.
         */
        @Suppress("unused")
        fun setTimeProvider(timeProvider: ITimeProvider): Builder {
            _clock = timeProvider
            return this
        }

        /**
         * Adds a network configuration that can be used to resolve ethr-DIDs
         * into their corresponding DID documents based on the network specific
         * ERC 1056 registry.
         *
         * This is the preferred method of configuration because it supports
         * abstraction of the blockchain access method.
         */
        fun addNetwork(network: EthrDIDNetwork): Builder {
            _networks.add(network)
            return this
        }

        /**
         * Adds a network configuration that can be used to resolve ethr-DIDs
         * into their corresponding DID documents based on the network specific
         * ERC 1056 registry.
         *
         * This method is usable when only JSON RPC over http is available;
         * If blockchain access needs to be abstracted or mocked, please use
         * [EthrDIDNetwork]
         */
        fun addNetwork(network: EthNetwork): Builder {
            _networks.add(network.toEthrDIDNetwork())
            return this
        }

        /**
         * Constructs the final EthrDIDResolver instance based on the networks and clock provided.
         */
        fun build(): EthrDIDResolver {
            val clock = _clock ?: SystemTimeProvider
            return EthrDIDResolver(RegistryMap.fromNetworks(_networks), clock)
        }
    }
}
