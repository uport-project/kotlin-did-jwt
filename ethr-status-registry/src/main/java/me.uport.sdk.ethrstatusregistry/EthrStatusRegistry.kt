package me.uport.sdk.ethrstatusregistry

import kotlinx.coroutines.runBlocking
import me.uport.credential_status.CredentialStatus
import me.uport.credential_status.StatusEntry
import me.uport.credential_status.StatusResolver
import me.uport.sdk.core.Networks
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jwt.JWTTools
import org.kethereum.keccakshortcut.keccak
import pm.gnosis.model.Solidity
import java.math.BigInteger

class EthrStatusRegistry : StatusResolver {

    override val method = "EthrStatusRegistry2019"

    override fun checkStatus(credential: String): CredentialStatus {
        val (_, payloadRaw) = JWTTools().decodeRaw(credential)
        val statusEntry = payloadRaw["status"] as StatusEntry?

        if (statusEntry?.type == method) {
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
        val registryAddress = "0x1E4651dca5Ef38636e2E4D7A6Ff4d2413fC56450"
        val rpc = JsonRPC(ethNetwork.rpcUrl)
        val credentialHash = credential.keccak()

        val encodedMethodCall = Revocation.Revoked.encode(
            Solidity.Address(address.toBigInteger()),
            Solidity.Bytes32(credentialHash)
        )

        val result = runBlocking {
            rpc.ethCall(registryAddress, encodedMethodCall)
        }

        if (result.toBigInteger() > BigInteger.ZERO) {
            return CredentialStatus(true)
        } else {
            return CredentialStatus(false)
        }
    }

    private fun parseRegistryId(id: String): Pair<String, String> {

        //language=RegExp
        val didParsePattern =
            "^((.*):)?(0x[0-9a-fA-F]{40}$)".toRegex()

        if (!didParsePattern.matches(id)) {
            throw IllegalStateException("The id '$id' is not a valid status registry ID.")
        }

        val result = didParsePattern.find(id)?.destructured

        return Pair(result?.component2() ?: "", result?.component1() ?: "")
    }
}