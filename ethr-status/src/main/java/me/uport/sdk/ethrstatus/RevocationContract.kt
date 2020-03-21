@file:Suppress("unused")

package me.uport.sdk.ethrstatus

import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase

/**
 * Encodes and decodes calls to the ethr-status-registry contract.
 *
 * This class was generated using the bivrost tools
 */
internal class RevocationContract {
    object Revoked {
        private const val METHOD_ID: String = "e46e3846"

        fun encode(issuer: Solidity.Address, digest: Solidity.Bytes32): String {
            return """0x$METHOD_ID${SolidityBase.encodeFunctionArguments(issuer, digest)}"""
        }

        fun decode(data: String): Return {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.UInt256.DECODER.decode(source)

            return Return(arg0)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Bytes32.DECODER.decode(source)

            return Arguments(arg0, arg1)
        }

        data class Return(val param0: Solidity.UInt256)

        data class Arguments(val issuer: Solidity.Address, val digest: Solidity.Bytes32)
    }
}
