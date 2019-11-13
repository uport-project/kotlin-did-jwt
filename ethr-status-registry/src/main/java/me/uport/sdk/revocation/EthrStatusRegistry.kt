package me.uport.sdk.revocation

import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.collections.List
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase

class EthrStatusRegistry {
    object Revocations {
        const val METHOD_ID: String = "2f9219f3"

        fun encode(arg1: Solidity.Bytes32, arg2: Solidity.Address): String {
            return "0x" + METHOD_ID + pm.gnosis.model.SolidityBase.encodeFunctionArguments(arg1,
                arg2)
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
            val arg0 = Solidity.Bytes32.DECODER.decode(source)
            val arg1 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1)
        }

        data class Return(val param0: Solidity.UInt256)

        data class Arguments(val param0: Solidity.Bytes32, val param1: Solidity.Address)
    }

    object Revoke {
        const val METHOD_ID: String = "b75c7dc6"

        fun encode(digest: Solidity.Bytes32): String {
            return "0x" + METHOD_ID + pm.gnosis.model.SolidityBase.encodeFunctionArguments(digest)
        }

        fun decode(data: String): Return {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Bool.DECODER.decode(source)

            return Return(arg0)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Bytes32.DECODER.decode(source)

            return Arguments(arg0)
        }

        data class Return(val param0: Solidity.Bool)

        data class Arguments(val digest: Solidity.Bytes32)
    }

    object Revoked {
        const val METHOD_ID: String = "e46e3846"

        fun encode(party: Solidity.Address, digest: Solidity.Bytes32): String {
            return "0x" + METHOD_ID + pm.gnosis.model.SolidityBase.encodeFunctionArguments(party,
                digest)
        }

        fun decode(data: String): Return {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Bool.DECODER.decode(source)

            return Return(arg0)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Bytes32.DECODER.decode(source)

            return Arguments(arg0, arg1)
        }

        data class Return(val param0: Solidity.Bool)

        data class Arguments(val party: Solidity.Address, val digest: Solidity.Bytes32)
    }

    object Events {
        object Revoked {
            const val EVENT_ID: String =
                "6e70be4be1a4aebd688b5523bd8b6278acac3963d71ebf2bd5ea50757047664b"

            fun decode(topics: List<String>, data: String): Arguments {
                // Decode topics
                if (topics.first() != EVENT_ID) throw IllegalArgumentException("topics[0] does not match event id")

                // Decode data
                val source = SolidityBase.PartitionData.of(data)
                val arg0 = Solidity.Address.DECODER.decode(source)
                val arg1 = Solidity.Bytes32.DECODER.decode(source)
                return Arguments(arg0, arg1)
            }

            data class Arguments(val revoker: Solidity.Address, val digest: Solidity.Bytes32)
        }
    }
}
