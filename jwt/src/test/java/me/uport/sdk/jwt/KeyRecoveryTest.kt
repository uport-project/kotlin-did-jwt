@file:Suppress("unused")

package me.uport.sdk.jwt

import kotlinx.coroutines.runBlocking
import me.uport.sdk.signer.KPSigner
import me.uport.sdk.signer.signJWT
import org.junit.Assert.assertEquals
import org.kethereum.crypto.publicKeyFromPrivate
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.extensions.toHexStringNoPrefix
import org.kethereum.hashes.sha256
import org.kethereum.model.PrivateKey
import org.walleth.khex.toHexString

class KeyRecoveryTest {

    //  Uncomment `@Test` to iterate over 1000 * 1000 key/message combinations. Takes a lot of time.
    //@Test
    fun `can recover key from JWT signature`() = runBlocking {
        for (i in 0 until 1000) {
            val privateKey = "super secret $i".toByteArray().sha256().toHexString()
            val pubKey = publicKeyFromPrivate(PrivateKey(privateKey.hexToBigInteger())).key.toHexStringNoPrefix()
            val signer = KPSigner(privateKey)
            println("trying key $i on 1000 messages")
            for (j in 0 until 1000) {
                val message = "hello $i".toByteArray(Charsets.UTF_8)

                val sigData = signer.signJWT(message)

                val recovered = signedJwtToKey(message, sigData).toHexStringNoPrefix()

                assertEquals("failed at key $i, message $j", pubKey, recovered)
            }
        }
    }

}