package me.uport.sdk.ethrstatusregistry

import me.uport.credential_status.CredentialStatus
import me.uport.credential_status.StatusEntry
import me.uport.credential_status.StatusResolver
import me.uport.sdk.core.Networks
import me.uport.sdk.jwt.JWTTools

class EthrStatusRegistry : StatusResolver {

    override val method = "EthrStatusRegistry2019"

    override fun checkStatus(credential: String): CredentialStatus {
        val (_, payloadRaw) = JWTTools().decodeRaw(credential)
        val statusEntry = payloadRaw["status"] as StatusEntry?
        val issuer: String = payloadRaw["iss"] as String? ?: throw IllegalStateException("The method '$method' is not a supported credential status method.")

        if (statusEntry?.type == method) {
            return runCredentialCheck(
                credential,
                issuer,
                statusEntry
            )
        } else {
            throw IllegalStateException("The method '$method' is not a supported credential status method.")
        }
    }

    private fun runCredentialCheck(
        credential: String,
        issuer: String,
        status: StatusEntry
    ): CredentialStatus {
        val (address, network) = parseRegistryId(status.id)

        val ethNetwork = Networks.get(network)
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