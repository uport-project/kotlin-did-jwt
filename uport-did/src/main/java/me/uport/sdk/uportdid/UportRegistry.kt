@file:Suppress("UndocumentedPublicFunction", "UndocumentedPublicClass")

package me.uport.sdk.uportdid

import pm.gnosis.model.Solidity
import pm.gnosis.model.SolidityBase

/**
 * This is the minimalistic interface for the UportRegistry contract.
 * To get the full interface use the [bivrost](https://github.com/gnosis/bivrost-kotlin)
 * code generating tool with the UportRegistry.json ABI as input
 */
class UportRegistry {
    object Get {
        private const val METHOD_ID: String = "447885f0"

        fun encode(
                registrationIdentifier: Solidity.Bytes32,
                issuer: Solidity.Address,
                subject: Solidity.Address
        ): String {

            return "0x$METHOD_ID${SolidityBase
                    .encodeFunctionArguments(
                            registrationIdentifier,
                            issuer,
                            subject)}"
        }

    }

}
