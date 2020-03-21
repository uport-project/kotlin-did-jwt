package me.uport.sdk.ethrstatus

import me.uport.credentialstatus.CredentialStatus
import me.uport.credentialstatus.StatusEntry
import me.uport.credentialstatus.StatusResolver
import me.uport.credentialstatus.getStatusEntry
import me.uport.sdk.core.Networks
import me.uport.sdk.core.hexToBigInteger
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.universaldid.DIDDocument
import org.kethereum.keccakshortcut.keccak
import pm.gnosis.model.Solidity
import java.math.BigInteger

/**
 * Ethr Implementation of the [StatusResolver]
 * This class enables users check the revocation status of a credential
 */
class EthrStatusResolver : StatusResolver {

    override val method = "EthrStatusRegistry2019"

    override suspend fun checkStatus(credential: String, didDoc: DIDDocument): CredentialStatus {
        val statusEntry = getStatusEntry(credential)

        if (statusEntry.type == method) {
            return runCredentialCheck(
                credential,
                statusEntry,
                didDoc
            )
        } else {
            throw IllegalStateException("The method '$method' is not a supported credential status method.")
        }
    }

    /**
     * Checks the revocation status of a given credential by making a call to the smart contract
     */
    private suspend fun runCredentialCheck(
        credential: String,
        status: StatusEntry,
        didDoc: DIDDocument
    ): EthrStatus {
        val (registryAddress, network) = parseRegistryId(status.id)

        val ethNetwork = Networks.get(network)
        val rpc = JsonRPC(ethNetwork.rpcUrl)
        val credentialHash = credential.toByteArray().keccak()

        val revokers = getValidRevokers(didDoc)

        val minRevocationBlock: BigInteger = revokers.map { revoker ->
                val encodedMethodCall = RevocationContract.Revoked.encode(
                    Solidity.Address(revoker.hexToBigInteger()),
                    Solidity.Bytes32(credentialHash)
                )

                val result = rpc.ethCall(registryAddress, encodedMethodCall)
                result.hexToBigInteger()
            }
            .filter { it != BigInteger.ZERO }
            .min()
            ?: BigInteger.ZERO

        return EthrStatus(minRevocationBlock)
    }

    /**
     * Generates a list of valid revoker addresses using the
     * list of public key entries in the [DIDDocument]
     * @returns a list of ethereum addresses considered to be valid revokers
     */
    internal fun getValidRevokers(didDoc: DIDDocument): List<String> =
        didDoc.publicKey.mapNotNull { it.ethereumAddress }.distinct()

    /**
     * Parses a given registry ID
     * @returns the network and the registry Address
     */
    internal fun parseRegistryId(id: String): Pair<String, String> {

        //language=RegExp
        val didParsePattern = "^(\\w+)?(?::)?(0x[0-9a-fA-F]{40})".toRegex()

        if (!didParsePattern.matches(id)) {
            throw IllegalArgumentException("The id '$id' is not a valid status registry ID.")
        }

        val matchResult = didParsePattern.find(id)
            ?: throw IllegalStateException("The format for '$id' is not a supported")

        val (network, registryAddress) = matchResult.destructured

        val nameOrId = if (network.isBlank()) {
            "mainnet"
        } else {
            network
        }

        return Pair(registryAddress, nameOrId)
    }
}

/**
 * Represents the status of a credential that should be checked using `EthrStatusRegistry2019`
 **/
data class EthrStatus(
    /**
     * The block number when it was first revoked by the issuer, or [BigInteger.ZERO] if it was never revoked
     **/
    val blockNumber: BigInteger
) : CredentialStatus
