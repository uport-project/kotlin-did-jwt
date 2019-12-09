package me.uport.sdk.ethrstatusregistry

import me.uport.credential_status.CredentialStatus
import me.uport.credential_status.StatusEntry
import me.uport.credential_status.StatusMethod
import me.uport.credential_status.StatusResolver
import me.uport.sdk.core.Networks
import me.uport.sdk.jwt.JWTTools
import me.uport.sdk.jwt.model.JwtPayload


interface MethodMapping {
    fun methodName(method: String): StatusMethod
}

interface JWTDecodedExtended {
    val status: EthrStatusEntry
    val payload: JwtPayload
}

interface EthrStatusEntry : StatusEntry {
    val type: String
    val id: String
}

class EthrStatusRegistry : StatusResolver {

    val method = "EthrStatusRegistry2019"
    //val methodMapping: MethodMapping = MethodMapping{}

    fun checkStatus(credential: String): CredentialStatus {
        val (_, payload) = JWTTools().decode(credential)
        val decodedJwt = payload as JWTDecodedExtended
        if (decodedJwt.status.type == method) {
            return runCredentialCheck(
                credential,
                decodedJwt.payload.iss,
                decodedJwt.status
            )
        } else {
            throw IllegalStateException("The method '$method' is not a supported credential status method.")
        }
    }

    private fun runCredentialCheck(
        credential: String,
        issuer: String,
        status: EthrStatusEntry
    ): CredentialStatus {
        val (address, network) = parseRegistryId(status.id)

        val ethNetwork = Networks.get(network)
    }

    private fun parseRegistryId(id: String): Pair<String, String> {

        //language=RegExp
        val didParsePattern =
            "^((.*):)?(0x[0-9a-fA-F]{40}$)".toRegex()

        if(!didParsePattern.matches(id)) {
            throw IllegalStateException("The id '$id' is not a valid status registry ID.")
        }

        val result = didParsePattern.find(id)?.destructured

        return Pair(result?.component2() ?: "", result?.component1() ?: "")
    }
}