package me.uport.sdk.ethrstatusregistry

import kotlinx.coroutines.runBlocking
import me.uport.credential_status.CredentialStatus
import me.uport.credential_status.StatusEntry
import me.uport.credential_status.StatusResolver
import me.uport.sdk.core.Networks
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.JWTTools
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.keccakshortcut.keccak
import pm.gnosis.model.Solidity

class EthrStatusRegistry : StatusResolver {

    override val method = "EthrStatusRegistry2019"

    override fun checkStatus(credential: String): CredentialStatus {
        val (_, payloadRaw) = JWTTools().decodeRaw(credential)
        val status = payloadRaw["status"] as Map<String, String>
        val type = status["type"] ?: ""
        val id = status["id"] ?: ""
        val statusEntry = StatusEntry(type, id)

        if (statusEntry.type == method) {
            return runCredentialCheck(
                credential,
                statusEntry
            )
        } else {
            throw IllegalStateException("The method '$method' is not a supported credential status method.")
        }
    }

    private fun runCredentialCheck(
        credential: String,
        status: StatusEntry
    ): CredentialStatus {
        val (address, network) = parseRegistryId(status.id)

        val ethNetwork = Networks.get(network)
        val rpc = JsonRPC(ethNetwork.rpcUrl)
        val credentialHash = credential.toByteArray().keccak()

        val encodedMethodCall = Revocation.Revoked.encode(
            Solidity.Address(address.hexToBigInteger()),
            Solidity.Bytes32(credentialHash)
        )

        val result = runBlocking {
            rpc.ethCall(address, encodedMethodCall)
        }

        if (result.toBigIntegerOrNull() != null) {
            return CredentialStatus(true)
        } else {
            return CredentialStatus(false)
        }
    }

    private fun parseRegistryId(did: String): Pair<String, String> {

        //language=RegExp
        val didParsePattern = "^(did:)?((\\w+):)?((\\w+):)?((0x)([0-9a-fA-F]{40}))".toRegex()

        if (!didParsePattern.matches(did)) {
            throw IllegalStateException("The id '$did' is not a valid status registry ID.")
        }

        val matchResult = didParsePattern.find(did)
            ?: throw IllegalStateException("The format for '$did' is not a supported")

        val (_, _, _, _, network, registryAddress, _, _) = matchResult.destructured

        val nameOrId = if (network.isBlank() || network in
            listOf(
                "0x1",
                "0x01"
            )
        ) {
            "mainnet"
        } else {
            network
        }

        return Pair(registryAddress, nameOrId)
    }
}