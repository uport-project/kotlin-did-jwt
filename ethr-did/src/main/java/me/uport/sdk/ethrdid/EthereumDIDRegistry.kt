//@file:Suppress("unused")

package me.uport.sdk.ethrdid

import java.lang.IllegalArgumentException
import java.math.BigInteger
import kotlin.String
import kotlin.collections.List
import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.utils.BigIntegerUtils

class EthereumDIDRegistry {
    object Owners {
        private const val METHOD_ID: String = "022914a7"

        fun encode(arg1: Solidity.Address): String {
            return "0x" + METHOD_ID + SolidityBase.encodeFunctionArguments(arg1)
        }

        fun decode(data: String): Return {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)

            return Return(arg0)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0)
        }

        data class Return(val param0: Solidity.Address)

        data class Arguments(val param0: Solidity.Address)
    }

    object Delegates {
        private const val METHOD_ID: String = "0d44625b"

        fun encode(
            arg1: Solidity.Address,
            arg2: Solidity.Bytes32,
            arg3: Solidity.Address
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(arg1,
                    arg2, arg3)
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
            val arg2 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2)
        }

        data class Return(val param0: Solidity.UInt256)

        data class Arguments(
            val param0: Solidity.Address,
            val param1: Solidity.Bytes32,
            val param2: Solidity.Address
        )
    }

    object Nonce {
        private const val METHOD_ID: String = "70ae92d2"

        fun encode(arg1: Solidity.Address): String {
            return "0x" + METHOD_ID + SolidityBase.encodeFunctionArguments(arg1)
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

            return Arguments(arg0)
        }

        data class Return(val param0: Solidity.UInt256)

        data class Arguments(val param0: Solidity.Address)
    }

    object Changed {
        private const val METHOD_ID: String = "f96d0f9f"

        fun encode(arg1: Solidity.Address): String {
            return "0x" + METHOD_ID + SolidityBase.encodeFunctionArguments(arg1)
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

            return Arguments(arg0)
        }

        data class Return(val param0: Solidity.UInt256)

        data class Arguments(val param0: Solidity.Address)
    }

    object IdentityOwner {
        private const val METHOD_ID: String = "8733d4e8"

        fun encode(identity: Solidity.Address): String {
            return "0x" + METHOD_ID + SolidityBase.encodeFunctionArguments(identity)
        }

        fun decode(data: String): Return {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)

            return Return(arg0)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0)
        }

        data class Return(val param0: Solidity.Address)

        data class Arguments(val identity: Solidity.Address)
    }

    object ValidDelegate {
        private const val METHOD_ID: String = "622b2a3c"

        fun encode(
            identity: Solidity.Address,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    delegateType, delegate)
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
            val arg2 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2)
        }

        data class Return(val param0: Solidity.Bool)

        data class Arguments(
            val identity: Solidity.Address,
            val delegatetype: Solidity.Bytes32,
            val delegate: Solidity.Address
        )
    }

    object ChangeOwner {
        private const val METHOD_ID: String = "f00d4b5d"

        fun encode(identity: Solidity.Address, newOwner: Solidity.Address): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    newOwner)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1)
        }

        data class Arguments(val identity: Solidity.Address, val newowner: Solidity.Address)
    }

    object ChangeOwnerSigned {
        private const val METHOD_ID: String = "240cf1fa"

        fun encode(
            identity: Solidity.Address,
            sigV: Solidity.UInt8,
            sigR: Solidity.Bytes32,
            sigS: Solidity.Bytes32,
            newOwner: Solidity.Address
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    sigV, sigR, sigS, newOwner)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.UInt8.DECODER.decode(source)
            val arg2 = Solidity.Bytes32.DECODER.decode(source)
            val arg3 = Solidity.Bytes32.DECODER.decode(source)
            val arg4 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2, arg3, arg4)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val sigv: Solidity.UInt8,
            val sigr: Solidity.Bytes32,
            val sigs: Solidity.Bytes32,
            val newowner: Solidity.Address
        )
    }

    object AddDelegate {
        private const val METHOD_ID: String = "a7068d66"

        fun encode(
            identity: Solidity.Address,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address,
            validity: Solidity.UInt256
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    delegateType, delegate, validity)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Bytes32.DECODER.decode(source)
            val arg2 = Solidity.Address.DECODER.decode(source)
            val arg3 = Solidity.UInt256.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2, arg3)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val delegatetype: Solidity.Bytes32,
            val delegate: Solidity.Address,
            val validity: Solidity.UInt256
        )
    }

    object AddDelegateSigned {
        private const val METHOD_ID: String = "9c2c1b2b"

        fun encode(
            identity: Solidity.Address,
            sigV: Solidity.UInt8,
            sigR: Solidity.Bytes32,
            sigS: Solidity.Bytes32,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address,
            validity: Solidity.UInt256
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    sigV, sigR, sigS, delegateType, delegate, validity)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.UInt8.DECODER.decode(source)
            val arg2 = Solidity.Bytes32.DECODER.decode(source)
            val arg3 = Solidity.Bytes32.DECODER.decode(source)
            val arg4 = Solidity.Bytes32.DECODER.decode(source)
            val arg5 = Solidity.Address.DECODER.decode(source)
            val arg6 = Solidity.UInt256.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2, arg3, arg4, arg5, arg6)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val sigv: Solidity.UInt8,
            val sigr: Solidity.Bytes32,
            val sigs: Solidity.Bytes32,
            val delegatetype: Solidity.Bytes32,
            val delegate: Solidity.Address,
            val validity: Solidity.UInt256
        )
    }

    object RevokeDelegate {
        private const val METHOD_ID: String = "80b29f7c"

        fun encode(
            identity: Solidity.Address,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    delegateType, delegate)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Bytes32.DECODER.decode(source)
            val arg2 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val delegatetype: Solidity.Bytes32,
            val delegate: Solidity.Address
        )
    }

    object RevokeDelegateSigned {
        private const val METHOD_ID: String = "93072684"

        fun encode(
            identity: Solidity.Address,
            sigV: Solidity.UInt8,
            sigR: Solidity.Bytes32,
            sigS: Solidity.Bytes32,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    sigV, sigR, sigS, delegateType, delegate)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.UInt8.DECODER.decode(source)
            val arg2 = Solidity.Bytes32.DECODER.decode(source)
            val arg3 = Solidity.Bytes32.DECODER.decode(source)
            val arg4 = Solidity.Bytes32.DECODER.decode(source)
            val arg5 = Solidity.Address.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2, arg3, arg4, arg5)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val sigv: Solidity.UInt8,
            val sigr: Solidity.Bytes32,
            val sigs: Solidity.Bytes32,
            val delegatetype: Solidity.Bytes32,
            val delegate: Solidity.Address
        )
    }

    object SetAttribute {
        private const val METHOD_ID: String = "7ad4b0a4"

        fun encode(
            identity: Solidity.Address,
            name: Solidity.Bytes32,
            value: Solidity.Bytes,
            validity: Solidity.UInt256
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    name, value, validity)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Bytes32.DECODER.decode(source)
            val arg2Offset = BigIntegerUtils.exact(BigInteger(source.consume(), 16))
            val arg2 = Solidity.Bytes.DECODER.decode(source.subData(arg2Offset))
            val arg3 = Solidity.UInt256.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2, arg3)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val name: Solidity.Bytes32,
            val value: Solidity.Bytes,
            val validity: Solidity.UInt256
        )
    }

    object SetAttributeSigned {
        private const val METHOD_ID: String = "123b5e98"

        fun encode(
            identity: Solidity.Address,
            sigV: Solidity.UInt8,
            sigR: Solidity.Bytes32,
            sigS: Solidity.Bytes32,
            name: Solidity.Bytes32,
            value: Solidity.Bytes,
            validity: Solidity.UInt256
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    sigV, sigR, sigS, name, value, validity)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.UInt8.DECODER.decode(source)
            val arg2 = Solidity.Bytes32.DECODER.decode(source)
            val arg3 = Solidity.Bytes32.DECODER.decode(source)
            val arg4 = Solidity.Bytes32.DECODER.decode(source)
            val arg5Offset = BigIntegerUtils.exact(BigInteger(source.consume(), 16))
            val arg5 = Solidity.Bytes.DECODER.decode(source.subData(arg5Offset))
            val arg6 = Solidity.UInt256.DECODER.decode(source)

            return Arguments(arg0, arg1, arg2, arg3, arg4, arg5, arg6)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val sigv: Solidity.UInt8,
            val sigr: Solidity.Bytes32,
            val sigs: Solidity.Bytes32,
            val name: Solidity.Bytes32,
            val value: Solidity.Bytes,
            val validity: Solidity.UInt256
        )
    }

    object RevokeAttribute {
        private const val METHOD_ID: String = "00c023da"

        fun encode(
            identity: Solidity.Address,
            name: Solidity.Bytes32,
            value: Solidity.Bytes
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    name, value)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.Bytes32.DECODER.decode(source)
            val arg2Offset = BigIntegerUtils.exact(BigInteger(source.consume(), 16))
            val arg2 = Solidity.Bytes.DECODER.decode(source.subData(arg2Offset))

            return Arguments(arg0, arg1, arg2)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val name: Solidity.Bytes32,
            val value: Solidity.Bytes
        )
    }

    object RevokeAttributeSigned {
        private const val METHOD_ID: String = "e476af5c"

        fun encode(
            identity: Solidity.Address,
            sigV: Solidity.UInt8,
            sigR: Solidity.Bytes32,
            sigS: Solidity.Bytes32,
            name: Solidity.Bytes32,
            value: Solidity.Bytes
        ): String {
            return "0x$METHOD_ID" + SolidityBase.encodeFunctionArguments(identity,
                    sigV, sigR, sigS, name, value)
        }

        fun decodeArguments(data: String): Arguments {
            val source = SolidityBase.PartitionData.of(data)

            // Add decoders
            val arg0 = Solidity.Address.DECODER.decode(source)
            val arg1 = Solidity.UInt8.DECODER.decode(source)
            val arg2 = Solidity.Bytes32.DECODER.decode(source)
            val arg3 = Solidity.Bytes32.DECODER.decode(source)
            val arg4 = Solidity.Bytes32.DECODER.decode(source)
            val arg5Offset = BigIntegerUtils.exact(BigInteger(source.consume(), 16))
            val arg5 = Solidity.Bytes.DECODER.decode(source.subData(arg5Offset))

            return Arguments(arg0, arg1, arg2, arg3, arg4, arg5)
        }

        data class Arguments(
            val identity: Solidity.Address,
            val sigv: Solidity.UInt8,
            val sigr: Solidity.Bytes32,
            val sigs: Solidity.Bytes32,
            val name: Solidity.Bytes32,
            val value: Solidity.Bytes
        )
    }

    object Events {
        object DIDOwnerChanged {
            private const val EVENT_ID: String =
                    "38a5a6e68f30ed1ab45860a4afb34bcb2fc00f22ca462d249b8a8d40cda6f7a3"

            fun decode(topics: List<String>, data: String): Arguments {
                // Decode topics
                if (topics.first().removePrefix("0x") != EVENT_ID) throw IllegalArgumentException("topics[0] does not match event id")
                val source1 = SolidityBase.PartitionData.of(topics[1])
                val t1 = Solidity.Address.DECODER.decode(source1)

                // Decode data
                val source = SolidityBase.PartitionData.of(data)
                val arg0 = Solidity.Address.DECODER.decode(source)
                val arg1 = Solidity.UInt256.DECODER.decode(source)
                return Arguments(t1, arg0, arg1)
            }

            data class Arguments(
                val identity: Solidity.Address,
                val owner: Solidity.Address,
                val previouschange: Solidity.UInt256
            )
        }

        object DIDDelegateChanged {
            private const val EVENT_ID: String =
                    "5a5084339536bcab65f20799fcc58724588145ca054bd2be626174b27ba156f7"

            fun decode(topics: List<String>, data: String): Arguments {
                // Decode topics
                if (topics.first().removePrefix("0x") != EVENT_ID) throw IllegalArgumentException("topics[0] does not match event id")
                val source1 = SolidityBase.PartitionData.of(topics[1])
                val t1 = Solidity.Address.DECODER.decode(source1)

                // Decode data
                val source = SolidityBase.PartitionData.of(data)
                val arg0 = Solidity.Bytes32.DECODER.decode(source)
                val arg1 = Solidity.Address.DECODER.decode(source)
                val arg2 = Solidity.UInt256.DECODER.decode(source)
                val arg3 = Solidity.UInt256.DECODER.decode(source)
                return Arguments(t1, arg0, arg1, arg2, arg3)
            }

            data class Arguments(
                val identity: Solidity.Address,
                val delegatetype: Solidity.Bytes32,
                val delegate: Solidity.Address,
                val validto: Solidity.UInt256,
                val previouschange: Solidity.UInt256
            )
        }

        object DIDAttributeChanged {
            private const val EVENT_ID: String =
                    "18ab6b2ae3d64306c00ce663125f2bd680e441a098de1635bd7ad8b0d44965e4"

            fun decode(topics: List<String>, data: String): Arguments {
                // Decode topics
                if (topics.first().removePrefix("0x") != EVENT_ID) throw IllegalArgumentException("topics[0] does not match event id")
                val source1 = SolidityBase.PartitionData.of(topics[1])
                val t1 = Solidity.Address.DECODER.decode(source1)

                // Decode data
                val source = SolidityBase.PartitionData.of(data)
                val arg0 = Solidity.Bytes32.DECODER.decode(source)
                val arg1Offset = BigIntegerUtils.exact(BigInteger(source.consume(), 16))
                val arg1 = Solidity.Bytes.DECODER.decode(source.subData(arg1Offset))
                val arg2 = Solidity.UInt256.DECODER.decode(source)
                val arg3 = Solidity.UInt256.DECODER.decode(source)
                return Arguments(t1, arg0, arg1, arg2, arg3)
            }

            data class Arguments(
                val identity: Solidity.Address,
                val name: Solidity.Bytes32,
                val value: Solidity.Bytes,
                val validto: Solidity.UInt256,
                val previouschange: Solidity.UInt256
            )
        }
    }
}
