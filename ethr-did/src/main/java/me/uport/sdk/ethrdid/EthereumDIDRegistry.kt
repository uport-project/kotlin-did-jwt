@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")

package me.uport.sdk.ethrdid

import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase
import pm.gnosis.utils.BigIntegerUtils
import java.math.BigInteger

/**
 * This is a minimalistic interface to the
 * [EIP1056](https://github.com/ethereum/EIPs/issues/1056) registry contract.
 * Only the methods used to perform DID resolution are defined.
 *
 * To generate the full interface use the
 * [bivrost](https://github.com/gnosis/bivrost-kotlin) tool on `/abi/EthereumDIDRegistry.json`
 */
class EthereumDIDRegistry {

    object Changed {
        private const val METHOD_ID: String = "f96d0f9f"

        fun encode(arg1: Solidity.Address): String {
            return "0x$METHOD_ID${SolidityBase.encodeFunctionArguments(arg1)}"
        }

        data class Return(val param0: Solidity.UInt256)

    }

    object IdentityOwner {
        private const val METHOD_ID: String = "8733d4e8"

        fun encode(identity: Solidity.Address): String {
            return "0x$METHOD_ID${SolidityBase.encodeFunctionArguments(identity)}"
        }

        data class Return(val param0: Solidity.Address)

    }

    object ChangeOwner {
        private const val METHOD_ID: String = "f00d4b5d"

        fun encode(identity: Solidity.Address, newOwner: Solidity.Address): String {
            return "0x$METHOD_ID${SolidityBase.encodeFunctionArguments(identity, newOwner)}"
        }

    }

    object AddDelegate {
        private const val METHOD_ID: String = "a7068d66"

        fun encode(
            identity: Solidity.Address,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address,
            validity: Solidity.UInt256
        ): String {
            return "0x$METHOD_ID${SolidityBase.encodeFunctionArguments(identity, delegateType, delegate, validity)}"
        }

    }

    object RevokeDelegate {
        private const val METHOD_ID: String = "80b29f7c"

        fun encode(
            identity: Solidity.Address,
            delegateType: Solidity.Bytes32,
            delegate: Solidity.Address
        ): String {
            return "0x$METHOD_ID${SolidityBase.encodeFunctionArguments(identity, delegateType, delegate)}"
        }

    }

    object SetAttribute {
        private const val METHOD_ID: String = "7ad4b0a4"

        fun encode(
            identity: Solidity.Address,
            name: Solidity.Bytes32,
            value: Solidity.Bytes,
            validity: Solidity.UInt256
        ): String {
            return "0x$METHOD_ID${SolidityBase.encodeFunctionArguments(identity, name, value, validity)}"
        }

    }

    object Events {
        object DIDOwnerChanged {
            private const val EVENT_ID: String =
                "38a5a6e68f30ed1ab45860a4afb34bcb2fc00f22ca462d249b8a8d40cda6f7a3"

            fun decode(topics: List<String>, data: String): Arguments {
                // Decode topics
                if (topics.first().removePrefix("0x") != EVENT_ID)
                    throw IllegalArgumentException("topics[0] does not match event id")
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
                if (topics.first().removePrefix("0x") != EVENT_ID)
                    throw IllegalArgumentException("topics[0] does not match event id")
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
                if (topics.first().removePrefix("0x") != EVENT_ID)
                    throw IllegalArgumentException("topics[0] does not match event id")
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
