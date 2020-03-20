@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass", "MagicNumber")

package me.uport.sdk.ethrdid

import me.uport.sdk.core.hexToBigInteger
import me.uport.sdk.core.hexToByteArray
import me.uport.sdk.core.prepend0xPrefix
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.signer.Signer
import me.uport.sdk.signer.signRawTx
import me.uport.sdk.signer.utf8
import me.uport.sdk.universaldid.PublicKeyType
import org.kethereum.model.Address
import org.kethereum.model.createTransactionWithDefaults
import org.komputing.khex.extensions.toHexString
import pm.gnosis.model.Solidity
import java.math.BigInteger

/**
 * Enables interaction with the EthrDID registry contract.
 *
 * See also: https://github.com/uport-project/ethr-did-registry
 *
 * **Note: This is a partial implementation and not meant for public use yet**
 */
class EthrDID(
    /**
     * Ethereum hex address that an interaction is about
     */
    private val address: String,

    /**
     * RPC endpoint wrapper that can execute JsonRPC calls such as
     * `eth_call`, `eth_sendRawTransaction`, `eth_getTransactionCount`...
     */
    private val rpc: JsonRPC,

    /**
     * Address of the EIP 1056 registry contract.
     * See also https://github.com/uport-project/ethr-did-registry
     */
    private val registry: String,

    /**
     * A [Signer] implementation used to sign any changes to the registry concerning the [address]
     */
    private val signer: Signer
) {

    private val owner: String? = null


    class DelegateOptions(
        val delegateType: PublicKeyType = PublicKeyType.veriKey,
        val expiresIn: Long = 86400L
    )

    suspend fun lookupOwner(cache: Boolean = true): String {
        if (cache && this.owner != null) return this.owner
        val encodedCall =
            EthereumDIDRegistry.IdentityOwner.encode(Solidity.Address(address.hexToBigInteger()))
        val rawResult = rpc.ethCall(registry, encodedCall)
        return rawResult.substring(rawResult.length - 40).prepend0xPrefix()
    }

    suspend fun changeOwner(newOwner: String, txOptions: TransactionOptions? = null): String {
        val owner = lookupOwner()

        val encodedCall = EthereumDIDRegistry.ChangeOwner.encode(
            Solidity.Address(address.hexToBigInteger()),
            Solidity.Address(newOwner.hexToBigInteger())
        )

        return signAndSendContractCall(owner, encodedCall, txOptions)
    }


    suspend fun addDelegate(
        delegate: String,
        options: DelegateOptions = DelegateOptions(),
        txOptions: TransactionOptions? = null
    ): String {
        val owner = lookupOwner()

        val encodedCall = EthereumDIDRegistry.AddDelegate.encode(
            Solidity.Address(this.address.hexToBigInteger()),
            Solidity.Bytes32(options.delegateType.name.toByteArray(utf8)),
            Solidity.Address(delegate.hexToBigInteger()),
            Solidity.UInt256(BigInteger.valueOf(options.expiresIn))
        )

        return signAndSendContractCall(owner, encodedCall, txOptions)
    }

    suspend fun revokeDelegate(
        delegate: String,
        delegateType: PublicKeyType = PublicKeyType.veriKey,
        txOptions: TransactionOptions? = null
    ): String {
        val owner = this.lookupOwner()
        val encodedCall = EthereumDIDRegistry.RevokeDelegate.encode(
            Solidity.Address(this.address.hexToBigInteger()),
            Solidity.Bytes32(delegateType.name.toByteArray(utf8)),
            Solidity.Address(delegate.hexToBigInteger())
        )

        return signAndSendContractCall(owner, encodedCall, txOptions)
    }

    suspend fun setAttribute(
        key: String,
        value: String,
        expiresIn: Long = 86400L,
        txOptions: TransactionOptions? = null
    ): String {
        val owner = this.lookupOwner()
        val encodedCall = EthereumDIDRegistry.SetAttribute.encode(
            Solidity.Address(this.address.hexToBigInteger()),
            Solidity.Bytes32(key.toByteArray(utf8)),
            Solidity.Bytes(value.toByteArray(utf8)),
            Solidity.UInt256(BigInteger.valueOf(expiresIn))
        )
        return signAndSendContractCall(owner, encodedCall, txOptions)
    }

    /**
     * Encapsulates some overrides
     */
    data class TransactionOptions(
        /**
         * overrides the gasLimit used in the transaction
         */
        val gasLimit: BigInteger? = null,

        /**
         * overrides the gasPrice (measured in wei) set for the transaction
         */
        val gasPrice: BigInteger? = null,

        /**
         * overrides the nonce used in the transaction
         * (for rebroadcasting with different params while the current one is not yet mined)
         */
        val nonce: BigInteger? = null,

        /**
         * overrides the ETH value (measured in wei).
         */
        val value: BigInteger? = null
    )

    private suspend fun signAndSendContractCall(
        owner: String,
        encodedCall: String,
        txOption: TransactionOptions? = null
    ): String {
        //these requests can be done in parallel
        val nonce = txOption?.nonce ?: rpc.getTransactionCount(owner)
        val networkPrice = txOption?.gasPrice ?: rpc.getGasPrice()

        val unsignedTx = createTransactionWithDefaults(
            from = Address(owner),
            to = Address(registry),
            gasLimit = txOption?.gasLimit ?: BigInteger.valueOf(80_000),
            gasPrice = networkPrice,
            nonce = nonce,
            input = encodedCall.hexToByteArray(),
            value = txOption?.value ?: BigInteger.ZERO
        )

        val signedEncodedTx = signer.signRawTx(unsignedTx)

        return rpc.sendRawTransaction(signedEncodedTx.toHexString())
    }
}
