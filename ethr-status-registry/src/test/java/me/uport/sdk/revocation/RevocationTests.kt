package me.uport.sdk.revocation

import kotlinx.coroutines.runBlocking
import me.uport.sdk.core.Networks
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.signer.KPSigner
import me.uport.sdk.signer.Signer
import me.uport.sdk.signer.signETH
import me.uport.sdk.signer.utf8
import org.junit.Test
import org.kethereum.DEFAULT_GAS_PRICE
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.functions.encodeRLP
import org.kethereum.keccakshortcut.keccak
import org.kethereum.model.Address
import org.kethereum.model.Transaction
import org.kethereum.model.createTransactionWithDefaults
import org.komputing.khex.extensions.hexToByteArray
import org.komputing.khex.extensions.prepend0xPrefix
import org.komputing.khex.extensions.toHexString
import pm.gnosis.model.Solidity
import java.math.BigInteger

class RevocationTests {

    private suspend fun revokeCredential(credential: String, signer: Signer): String {
        val address = signer.getAddress().prepend0xPrefix()

        //later fixme: get this from credential
        val registryAddress = "0x1E4651dca5Ef38636e2E4D7A6Ff4d2413fC56450"

        //later fixme: get the network from the credential
        val rpc = JsonRPC(Networks.rinkeby.rpcUrl)

        val msgDigest = credential.toByteArray(utf8).keccak()

        println("gigel debug 'did:ethr:$address'\n is revoking a credential with hash:\n${msgDigest.toHexString()}")

        val encodedMethodCall = EthrStatusRegistry.Revoke.encode(
            Solidity.Bytes32(msgDigest)
        )

        val unsignedTransaction = createTransactionWithDefaults(
            from = Address(address),
            to = Address(registryAddress),
            gasPrice = DEFAULT_GAS_PRICE,
            gasLimit = 50_000L.toBigInteger(),
            value = BigInteger.ZERO,
            input = encodedMethodCall.hexToByteArray(),
            nonce = getNonce(address, rpc)
        )

        val signedEncodedTransaction = unsignedTransaction.sign(signer)
        val txHash = rpc.sendRawTransaction(signedEncodedTransaction)
        println("https://rinkeby.etherscan.io/tx/$txHash")
        return txHash
    }

    @Test
    fun `can revoke message`() = runBlocking {
        //TODO: add your private key here with a 0x prefix
        val signer =
            KPSigner("0x278a5de700e29faae8e40e366ec5012b5ec63d36ec77e8a2417154cc1d25383f")

        val address = signer.getAddress().prepend0xPrefix()
        //0xf3beac30c498d9e26865f34fcaa57dbb935b0d74

        val txHash = revokeCredential(
            //TODO: paste the credential to be revoked here:
            credential = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJpYXQiOjE1NzMxNDA1NjQsInN0YXR1cyI6eyJ0eXBlIjoiRXRoclN0YXR1c1JlZ2lzdHJ5MjAxOSIsImlkIjoicmlua2VieToweDFFNDY1MWRjYTVFZjM4NjM2ZTJFNEQ3QTZGZjRkMjQxM2ZDNTY0NTAifSwiaXNzIjoiZGlkOmV0aHI6MHgxRkNmOGZmNzhhQzUxMTdkOWM5OUI4MzBjNzRiNjY2OEQ2QUMzMjI5In0.lJbc6Je-tFtQeZnGd6Rv-QvLKJZhN3UmdVZ9hGWRQy7za88fo2YlNeZ6eRN6YnnHLYUl4lnIDkmzg6mVgBqZygA",
            signer = signer
        )
    }


    private suspend fun Transaction.sign(signer: Signer): String =
        this.encodeRLP(signer.signETH(encodeRLP())).toHexString()

    suspend fun getNonce(address: String, rpc: JsonRPC = JsonRPC(Networks.rinkeby.rpcUrl)): BigInteger {
        return rpc.getTransactionCount(address)
    }

    @Test
    fun `can check for revocation`() {

        runBlocking {

            val address = "0x1fcf8ff78ac5117d9c99b830c74b6668d6ac3229"
            val registryAddress = "0x1E4651dca5Ef38636e2E4D7A6Ff4d2413fC56450"
            val rpc = JsonRPC(Networks.rinkeby.rpcUrl)

            val msg = "0x1234".hexToByteArray()
            val msgHash = msg.keccak()
            println("msgHash = ${msgHash.toHexString()}")

            val encodedMethodCall = EthrStatusRegistry.Revoked.encode(
                Solidity.Address(address.hexToBigInteger()),
                Solidity.Bytes32(msgHash)
            )

            val result = rpc.ethCall(registryAddress, encodedMethodCall)
            println(result)


        }
    }
}