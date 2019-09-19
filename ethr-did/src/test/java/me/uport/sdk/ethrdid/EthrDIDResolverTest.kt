@file:Suppress("UnnecessaryVariable")

package me.uport.sdk.ethrdid

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import me.uport.sdk.core.HttpClient
import me.uport.sdk.core.Networks
import me.uport.sdk.ethrdid.EthereumDIDRegistry.Events.DIDOwnerChanged
import me.uport.sdk.jsonrpc.JsonRPC
import me.uport.sdk.jsonrpc.model.JsonRpcLogItem
import me.uport.sdk.jsonrpc.model.exceptions.JSON_RPC_INTERNAL_ERROR_CODE
import me.uport.sdk.jsonrpc.model.exceptions.JsonRpcException
import me.uport.sdk.jwt.test.EthrDIDTestHelpers
import me.uport.sdk.signer.hexToBytes32
import me.uport.sdk.signer.utf8
import me.uport.sdk.testhelpers.coAssert
import me.uport.sdk.universaldid.DidResolverError
import org.junit.Test
import org.kethereum.extensions.hexToBigInteger
import pm.gnosis.model.Solidity
import pm.gnosis.utils.hexToByteArray
import java.math.BigInteger

class EthrDIDResolverTest {

    @Test
    fun `last change is blank for new address`() = runBlocking {
        val rpc = spyk(JsonRPC(""))
        val encodedCallSlot = slot<String>()

        coEvery {
            rpc.ethCall(
                any(),
                capture(encodedCallSlot)
            )
        } returns "0x0000000000000000000000000000000000000000000000000000000000000000"

        val imaginaryAddress = "0x1234"
        val lastChanged = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", "0xregistry", rpc))
            .build()
            .lastChanged(imaginaryAddress, rpc, "0xregistry")

        assertThat(encodedCallSlot.captured).isEqualTo("0xf96d0f9f0000000000000000000000000000000000000000000000000000000000001234")
        assertThat(lastChanged.hexToBigInteger()).isEqualTo(BigInteger.ZERO)
    }

    @Test
    fun `last change is non-zero for real address with changed owner`() = runBlocking {
        val rpc = spyk(JsonRPC(""))
        val encodedCallSlot = slot<String>()
        coEvery {
            rpc.ethCall(
                any(),
                capture(encodedCallSlot)
            )
        } returns "0x00000000000000000000000000000000000000000000000000000000002a8a7d"

        val realAddress = "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
        val lastChanged = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", "0xregistry", rpc))
            .build()
            .lastChanged(realAddress, rpc, "0xregistry")

        assertThat(encodedCallSlot.captured).isEqualTo("0xf96d0f9f000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74")
        assertThat(lastChanged.hexToBigInteger()).isNotEqualTo(BigInteger.ZERO)
    }


    @Test
    fun `can parse getLogs response`() = runBlocking {
        val http = mockk<HttpClient>()
        val rpc = JsonRPC("", http)
        val realAddress = "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
        val lastChanged =
            "0x00000000000000000000000000000000000000000000000000000000002a8a7d".hexToBigInteger()

        //language=json
        val cannedLogsResponse =
            """{"jsonrpc":"2.0","id":1,"result":[{"address":"0xdca7ef03e98e0dc2b855be647c39abe984fcf21b","blockHash":"0x10b9345e8c8ba8f5fbd164fc104e4959abb010ddcc38b164ac1c62c55e75856e","blockNumber":"0x2a8a7d","data":"0x536563703235366b31566572696669636174696f6e4b6579323031380000000000000000000000000000000045c4ebd7ffb86891ba6f9f68452f9f0815aacd8b0000000000000000000000000000000000000000000000000000000117656a2f00000000000000000000000000000000000000000000000000000000002a7b24","logIndex":"0x16","removed":false,"topics":["0x5a5084339536bcab65f20799fcc58724588145ca054bd2be626174b27ba156f7","0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74"],"transactionHash":"0x59180d9f3257a538ef77ba7363ec55ed76b609bf0c90cdf7fb710d695ebaa5c0","transactionIndex":"0x17"}]}"""
        coEvery { http.urlPost(any(), any(), any()) } returns cannedLogsResponse

        val logResponse =
            rpc.getLogs(
                "0xregistry",
                listOf(null, realAddress.hexToBytes32()),
                lastChanged,
                lastChanged
            )

        assertThat(logResponse).all {
            isNotNull()
            isNotEmpty()
        }
    }

    @Test
    fun `can parse owner changed logs`() = runBlocking {
        val logItem = JsonRpcLogItem(
            address = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
            topics = listOf(
                "0x38a5a6e68f30ed1ab45860a4afb34bcb2fc00f22ca462d249b8a8d40cda6f7a3",
                "0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74"
            ),
            data = "0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74000000000000000000000000000000000000000000000000000000000029db37",
            blockNumber = BigInteger("2784036"),
            transactionHash = "0xb42e3fbf29fffe53746021837396cf1a2e9ad88a82d5c9213e2725b5e72e123e",
            transactionIndex = BigInteger("17"),
            blockHash = "0xf7b8a4b602e6e47fc190ecbb213d09cd577186b3d2f28a0816eff6da55a6e469",
            logIndex = BigInteger("20"),
            removed = false
        )

        val topics: List<String> = logItem.topics
        val data: String = logItem.data
        val args: DIDOwnerChanged.Arguments = DIDOwnerChanged.decode(topics, data)
        //no assertion about args but it should not crash
        val previousBlock = args.previouschange.value

        assertThat(previousBlock).isGreaterThan(BigInteger.ZERO)
    }

    @Test
    fun `can parse multiple event logs`() = runBlocking {
        val realAddress = "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"

        val rpc = spyk(JsonRPC(""))
        coEvery {
            rpc.ethCall(
                any(),
                eq("0xf96d0f9f000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74")
            )
        }
            .returns("0x00000000000000000000000000000000000000000000000000000000002a8a7d")
        val cannedResponses: List<List<JsonRpcLogItem>> = listOf(
            listOf(
                JsonRpcLogItem(
                    address = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
                    blockHash = "0x10b9345e8c8ba8f5fbd164fc104e4959abb010ddcc38b164ac1c62c55e75856e",
                    blockNumber = "0x2a8a7d".hexToBigInteger(),
                    data = "0x536563703235366b31566572696669636174696f6e4b6579323031380000000000000000000000000000000045c4ebd7ffb86891ba6f9f68452f9f0815aacd8b0000000000000000000000000000000000000000000000000000000117656a2f00000000000000000000000000000000000000000000000000000000002a7b24",
                    logIndex = "0x16".hexToBigInteger(),
                    removed = false,
                    topics = listOf(
                        "0x5a5084339536bcab65f20799fcc58724588145ca054bd2be626174b27ba156f7",
                        "0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74"
                    ),
                    transactionHash = "0x59180d9f3257a538ef77ba7363ec55ed76b609bf0c90cdf7fb710d695ebaa5c0",
                    transactionIndex = "0x17".hexToBigInteger()
                )
            ),
            listOf(
                JsonRpcLogItem(
                    address = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
                    blockHash = "0xf7b8a4b602e6e47fc190ecbb213d09cd577186b3d2f28a0816eff6da55a6e469",
                    blockNumber = "0x2a7b24".hexToBigInteger(),
                    data = "0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74000000000000000000000000000000000000000000000000000000000029db37",
                    logIndex = "0x14".hexToBigInteger(),
                    removed = false,
                    topics = listOf(
                        "0x38a5a6e68f30ed1ab45860a4afb34bcb2fc00f22ca462d249b8a8d40cda6f7a3",
                        "0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74"
                    ),
                    transactionHash = "0xb42e3fbf29fffe53746021837396cf1a2e9ad88a82d5c9213e2725b5e72e123e",
                    transactionIndex = "0x11".hexToBigInteger()
                )
            ),
            listOf(
                JsonRpcLogItem(
                    address = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
                    blockHash = "0xf0bfb1aaa47ce10e6aa99940bafc2bb11f3de742d44d2288b4546250e67b0971",
                    blockNumber = "0x29db37".hexToBigInteger(),
                    data = "0x00000000000000000000000045c4ebd7ffb86891ba6f9f68452f9f0815aacd8b0000000000000000000000000000000000000000000000000000000000000000",
                    logIndex = "0x0".hexToBigInteger(),
                    removed = false,
                    topics = listOf(
                        "0x38a5a6e68f30ed1ab45860a4afb34bcb2fc00f22ca462d249b8a8d40cda6f7a3",
                        "0x000000000000000000000000f3beac30c498d9e26865f34fcaa57dbb935b0d74"
                    ),
                    transactionHash = "0x70829b62ac232269a0524b180054532eff18b5fbc60b7102b6120844b5cdb1d8",
                    transactionIndex = "0x1".hexToBigInteger()
                )
            ),
            emptyList()
        )
        coEvery { rpc.getLogs(any(), any(), any(), any()) }.returnsMany(cannedResponses)

        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", "0xregistry", rpc))
            .build()
        val events = resolver.getHistory(realAddress, rpc, "0xregistry")
        assertThat(events).hasSize(3)
    }

    @Test
    fun `can resolve doc for address with delegate key added`() = runBlocking {

        val rpc = mockk<JsonRPC>()

        coEvery {
            //mock the lookup owner call to return itself
            rpc.ethCall(
                any(),
                eq("0x8733d4e800000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656")
            )
        }.returns("0x00000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656")

        coEvery {
            //mock the lastChanged call to point to block number 4680310 (0x476A76)
            rpc.ethCall(
                any(),
                eq("0xf96d0f9f00000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656")
            )
        }.returns("0x0000000000000000000000000000000000000000000000000000000000476A76")

        coEvery {
            rpc.getLogs(address = any(), topics = any(), fromBlock = any(), toBlock = any())
        }.returnsMany(
            listOf(
                JsonRpcLogItem(
                    address = "0xdca7ef03e98e0dc2b855be647c39abe984fcf21b",
                    topics = listOf(
                        "0x5a5084339536bcab65f20799fcc58724588145ca054bd2be626174b27ba156f7",
                        "0x00000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656"
                    ),
                    //this is the important bit that states that a delegate key was added
                    data = "0x766572694b657900000000000000000000000000000000000000000000000000000000000000000000000000cf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed000000000000000000000000000000000000000000000000000000006245b1050000000000000000000000000000000000000000000000000000000000000000",
                    blockNumber = BigInteger("4680310"),
                    transactionHash = "0x5b1749dd1eb4cee09f114e5b12d82d68c9099ba38482d602f2d939f9082f71e3",
                    transactionIndex = BigInteger("0"),
                    blockHash = "0x4f1acf82e4b2578cb5a5c0fe1c3806dc89d5b28ca4946219cf1a0f04ad654fb8",
                    logIndex = BigInteger("0"),
                    removed = false
                )
            ),
            emptyList()
        )

        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", "0xregistry", rpc))
            .build()
        val ddo = resolver.resolve("0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656")

        val expectedDDO = EthrDIDDocument.fromJson(
            //language=json
            """{
              "id": "did:ethr:0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656",
              "publicKey": [
                {
                  "id": "did:ethr:0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656#owner",
                  "type": "Secp256k1VerificationKey2018",
                  "owner": "did:ethr:0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656",
                  "ethereumAddress": "0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656"
                },
                {
                  "id": "did:ethr:0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656#delegate-1",
                  "type": "Secp256k1VerificationKey2018",
                  "owner": "did:ethr:0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656",
                  "ethereumAddress": "0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed"
                }
              ],
              "authentication": [
                {
                  "type": "Secp256k1SignatureAuthentication2018",
                  "publicKey": "did:ethr:0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656#owner"
                }
              ],
              "service": [],
              "@context": "https://w3id.org/did/v1"
            }""".trimIndent()
        )

        assertThat(ddo).isEqualTo(expectedDDO)
    }

    @Test
    fun `resolves with default doc when the RPC logs are blank or corrupted`() = runBlocking {
        val rpc = mockk<JsonRPC>()

        coEvery {
            //mock the lookup owner call to return itself
            rpc.ethCall(
                any(),
                eq("0x8733d4e800000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656")
            )
        }.returns("0x00000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656")

        coEvery {
            //mock the lastChanged call to point to block numbers 9 and 8
            rpc.ethCall(
                any(),
                eq("0xf96d0f9f00000000000000000000000062d283fe6939c01fc88f02c6d2c9a547cc3e2656")
            )
        }.returnsMany(
            listOf(
                "0x0000000000000000000000000000000000000000000000000000000000000009",
                "0x0000000000000000000000000000000000000000000000000000000000000008"
            )
        )

        coEvery {
            rpc.getLogs(address = any(), topics = any(), fromBlock = any(), toBlock = any())
        }.returnsMany(
            emptyList(),
            emptyList()
        )

        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", "0xregistry", rpc, "0x1")).build()
        val ddo = resolver.resolve("0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656")

        val expectedDDO = EthrDIDTestHelpers
            .mockDocForAddress("0x62d283fe6939c01fc88f02c6d2c9a547cc3e2656")

        assertThat(ddo).isEqualTo(expectedDDO)
    }

    // "did/pub/(Secp256k1|Rsa|Ed25519)/(veriKey|sigAuth)/(hex|base64)",
    private val attributeRegexes = listOf(
        "did/pub/Secp256k1/veriKey/hex",
        "did/pub/Rsa/veriKey/hex",
        "did/pub/Ed25519/veriKey/hex",
        "did/pub/Secp256k1/sigAuth/hex",
        "did/pub/Rsa/sigAuth/hex",
        "did/pub/Ed25519/sigAuth/hex",
        "did/pub/Secp256k1/veriKey/base64",
        "did/pub/Rsa/veriKey/base64",
        "did/pub/Ed25519/veriKey/base64",
        "did/pub/Secp256k1/sigAuth/base64",
        "did/pub/Rsa/sigAuth/base64",
        "did/pub/Ed25519/sigAuth/base64",
        "did/pub/Secp256k1/veriKey",
        "did/pub/Rsa/veriKey",
        "did/pub/Ed25519/veriKey",
        "did/pub/Secp256k1/sigAuth",
        "did/pub/Rsa/sigAuth",
        "did/pub/Ed25519/sigAuth",
        "did/pub/Secp256k1",
        "did/pub/Rsa",
        "did/pub/Ed25519"
    )

    @Suppress("UNUSED_VARIABLE")
    @Test
    fun `can parse attribute regex`() {
        val regex = "^did/(pub|auth|svc)/(\\w+)(/(\\w+))?(/(\\w+))?$".toRegex()
        attributeRegexes.forEach {
            val matchResult = regex.find(it)

            assertThat(matchResult).isNotNull()

            val (section, algo, _, rawType, _, encoding) = matchResult!!.destructured

            assertThat(section).isNotEmpty()
            assertThat(algo).isNotEmpty()
        }
    }

    @Test
    fun `can parse sample attr change event in history`() {
        val soon = System.currentTimeMillis() / 1000 + 600
        val identity = "0xf3beac30c498d9e26865f34fcaa57dbb935b0d74"
        val owner = identity

        val event = EthereumDIDRegistry.Events.DIDAttributeChanged.Arguments(
            identity = Solidity.Address(identity.hexToBigInteger()),
            name = Solidity.Bytes32("did/pub/Secp256k1/veriKey/base64".toByteArray()),
            value = Solidity.Bytes("0x02b97c30de767f084ce3080168ee293053ba33b235d7116a3263d29f1450936b71".hexToByteArray()),
            validto = Solidity.UInt256(soon.toBigInteger()),
            previouschange = Solidity.UInt256(BigInteger.ZERO)
        )

        val rpc = JsonRPC("")

        assertThat {
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", rpc))
                .build()
                .wrapDidDocument("did:ethr:$identity", owner, listOf(event))
        }.doesNotThrowAnyException()
    }

    @Test
    fun `to and from solidity bytes`() {
        val str = "did/pub/Secp256k1/veriKey/hex"
        val sol = Solidity.Bytes32(str.toByteArray())
        val decodedStr = sol.bytes.toString(utf8)
        assertThat(decodedStr).isEqualTo(str)
    }

    @Test
    fun `can resolve real did`() = runBlocking {
        val http = mockk<HttpClient>()

        val referenceDDO =
            EthrDIDTestHelpers.mockDocForAddress("0xb9c5714089478a327f09197987f16f9e5d936e8a")

        val addressHex = "b9c5714089478a327f09197987f16f9e5d936e8a"

        val rpc = spyk(JsonRPC("http://localhost:8545", http))
        //canned response for get owner query
        coEvery {
            rpc.ethCall(
                any(),
                eq("0x8733d4e8000000000000000000000000$addressHex")
            )
        } returns "0x000000000000000000000000$addressHex"
        //canned response for last changed query
        coEvery {
            rpc.ethCall(
                any(),
                eq("0xf96d0f9f000000000000000000000000$addressHex")
            )
        } returns "0x0000000000000000000000000000000000000000000000000000000000000000"
        //canned response for getLogs
        coEvery {
            http.urlPost(
                any(),
                any(),
                any()
            )
        } returns """{"jsonrpc":"2.0","id":1,"result":[]}"""

        val resolver =
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "0xregistry", rpc))
                .build()
        val ddo = resolver.resolve("did:ethr:0x$addressHex")
        assertThat(ddo).isEqualTo(referenceDDO)
    }

    @Test
    fun `can normalize DID`() {

        val validDids = listOf(
            "0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x01:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x01:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner"
        )


        val invalidDids = listOf(
            "0xb9c5714089478a327f09197987f16f9e5d936e",
            "B9C5714089478a327F09197987f16f9E5d936E8a",
            "ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "B9C5714089478a327F09197987f16f9E5d936E8a",
            "B9C5714089478a327F09197987f16f9E5d936E"
        )

        validDids.forEach {
            val normalizedDid = EthrDIDResolver.normalizeDid(it)
            assertThat(normalizedDid.toLowerCase()).isEqualTo("did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a")
        }

        invalidDids.forEach {
            val normalizedDid = EthrDIDResolver.normalizeDid(it)
            assertThat(normalizedDid).isEmpty()
        }
    }

    @Test
    fun `can normalize networked DIDs`() {
        val validNetworkedDIDs = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner"
        )
        validNetworkedDIDs.forEach {
            val normalizedDid = EthrDIDResolver.normalizeDid(it).toLowerCase()
            println("normalizing `$it` got `$normalizedDid`")
            assertThat(normalizedDid).isNotEmpty()
            assertThat(normalizedDid).doesNotContain("#owner")
        }
    }

    @Test
    fun `can extract address from networked DIDs`() {
        val validNetworkedDIDs = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner"
        )
        validNetworkedDIDs.forEach {
            val address = EthrDIDResolver.extractAddress(it).toLowerCase()
            println("extracting address from `$it` got `$address`")
            assertThat(address).isEqualTo("0xb9c5714089478a327f09197987f16f9e5d936e8a")
        }
    }

    @Test
    fun `can extract network from networked DIDs`() {
        val validNetworkedDIDs = mapOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a" to "",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a" to "",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner" to "",
            "did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a" to "mainnet",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a" to "mainnet",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner" to "mainnet",
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a" to "rinkeby",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a" to "rinkeby",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner" to "rinkeby",
            "did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a" to "0x1",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a" to "0x1",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner" to "0x1",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a" to "0x04",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a" to "0x04",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner" to "0x04"
        )
        validNetworkedDIDs.forEach { (did, expectedNetwork) ->
            val extractedNetwork = EthrDIDResolver.extractNetwork(did).toLowerCase()
            println("extracting network from `$did` got `$extractedNetwork`")
            assertThat(extractedNetwork).isEqualTo(expectedNetwork)
        }
    }

    @Test
    fun `can resolve generic did when only provided with mainnet config`() {
        val http = mockk<HttpClient>()
        val referenceDDO =
            EthrDIDTestHelpers.mockDocForAddress("0xb9c5714089478a327f09197987f16f9e5d936e8a")

        val addressHex = "b9c5714089478a327f09197987f16f9e5d936e8a"

        val rpc = spyk(JsonRPC("mainnetRPC", http))
        //canned response for get owner query
        coEvery {
            rpc.ethCall(
                any(),
                eq("0x8733d4e8000000000000000000000000$addressHex")
            )
        } returns "0x000000000000000000000000$addressHex"
        //canned response for last changed query
        coEvery {
            rpc.ethCall(
                any(),
                eq("0xf96d0f9f000000000000000000000000$addressHex")
            )
        } returns "0x0000000000000000000000000000000000000000000000000000000000000000"
        //canned response for getLogs
        coEvery {
            http.urlPost(
                eq("mainnetRPC"),
                any(),
                any()
            )
        } returns """{"jsonrpc":"2.0","id":1,"result":[]}"""


        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("mainnet", "mockregistry", rpc, "0x1"))
            .build()

        coAssert {
            val ddo = resolver.resolve("did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a")
            assertThat(ddo).isEqualTo(referenceDDO)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `can resolve networked dids`() = runBlocking {
        val http = mockk<HttpClient>()

        val addressHex = "b9c5714089478a327f09197987f16f9e5d936e8a"

        val rpc = spyk(JsonRPC("mainnetRPC", http))
        //canned response for get owner query
        coEvery {
            rpc.ethCall(
                any(),
                eq("0x8733d4e8000000000000000000000000$addressHex")
            )
        } returns "0x000000000000000000000000$addressHex"
        //canned response for last changed query
        coEvery {
            rpc.ethCall(
                any(),
                eq("0xf96d0f9f000000000000000000000000$addressHex")
            )
        } returns "0x0000000000000000000000000000000000000000000000000000000000000000"
        //canned response for getLogs
        coEvery {
            http.urlPost(
                any(),
                any(),
                any()
            )
        } returns """{"jsonrpc":"2.0","id":1,"result":[]}"""


        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("mainnet", "0xregistry", rpc, "0x1"))
            .addNetwork(EthrDIDNetwork("rinkeby", "0xregistry", rpc, "0x4"))
            .build()

        val genericDDO = resolver.resolve("did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a")
        val mainnetDDO =
            resolver.resolve("did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a")
        val mainnetDDO0x1 =
            resolver.resolve("did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a")

        val referenceDDO =
            EthrDIDTestHelpers.mockDocForAddress("0xb9c5714089478a327f09197987f16f9e5d936e8a")
        assertThat(genericDDO).isEqualTo(referenceDDO)
        assertThat(mainnetDDO).isEqualTo(referenceDDO)
        assertThat(mainnetDDO0x1).isEqualTo(referenceDDO)

        val rinkebyDDO =
            resolver.resolve("did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a")
        val rinkebyDDO0x04 =
            resolver.resolve("did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a")

        assertThat(rinkebyDDO).isEqualTo(EthrDIDTestHelpers.mockDocForAddress("did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a"))
        assertThat(rinkebyDDO0x04).isEqualTo(EthrDIDTestHelpers.mockDocForAddress("did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a"))
    }

    @Test
    fun `can resolve generic did when only provided with 0x1 config`() {
        val http = mockk<HttpClient>()
        val referenceDDO =
            EthrDIDTestHelpers.mockDocForAddress("0xb9c5714089478a327f09197987f16f9e5d936e8a")

        val addressHex = "b9c5714089478a327f09197987f16f9e5d936e8a"

        val rpc = spyk(JsonRPC("mainnetRPC", http))
        //canned response for get owner query
        coEvery {
            rpc.ethCall(
                eq("0xregistry"),
                eq("0x8733d4e8000000000000000000000000$addressHex")
            )
        } returns "0x000000000000000000000000$addressHex"
        //canned response for last changed query
        coEvery {
            rpc.ethCall(
                eq("0xregistry"),
                eq("0xf96d0f9f000000000000000000000000$addressHex")
            )
        } returns "0x0000000000000000000000000000000000000000000000000000000000000000"
        //canned response for getLogs
        coEvery {
            http.urlPost(
                eq("mainnetRPC"),
                any(),
                any()
            )
        } returns """{"jsonrpc":"2.0","id":1,"result":[]}"""


        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("__default__", "0xregistry", rpc, "0x01"))
            .build()

        coAssert {
            val ddo = resolver.resolve("did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a")
            assertThat(ddo).isEqualTo(referenceDDO)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `throws when missing config`() {
        val resolver = EthrDIDResolver.Builder().build()

        coAssert {
            resolver.resolve("did:ethr:unknown:0xb9c5714089478a327f09197987f16f9e5d936e8a")
        }.thrownError {
            isInstanceOf(IllegalArgumentException::class)
            hasMessage("Missing registry configuration for `unknown`. To resolve did:ethr:unknown:0x... you need to register an `EthrDIDNetwork` in the EthrDIDResolver.Builder")
        }
    }

    @Test
    fun `canResolve reports true on registered networks without mainnet or default`() {
        val dids = listOf(
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x2a:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x2a:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x2a:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner"
        )

        val resolver = EthrDIDResolver.Builder()
            .addNetwork(Networks.rinkeby)
            .addNetwork(Networks.kovan)
            .addNetwork(Networks.ropsten)
            .build()
        dids.forEach {
            assertThat(resolver.canResolve(it)).isTrue()
        }
    }

    @Test
    fun `canResolve reports true on registered networks`() {
        val dids = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner"
        )

        val resolver = EthrDIDResolver.Builder()
            .addNetwork(Networks.mainnet)
            .addNetwork(Networks.rinkeby)
            .addNetwork(Networks.kovan)
            .addNetwork(Networks.ropsten)
            .build()
        dids.forEach {
            assertThat(resolver.canResolve(it)).isTrue()
        }
    }

    @Test
    fun `canResolve reports false on unregistered networks`() {
        val dids = listOf(
            "did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:mainnet:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:mainnet:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:rinkeby:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:rinkeby:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x1:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x1:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner",
            "did:ethr:0x04:0xb9c5714089478a327f09197987f16f9e5d936e8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a",
            "did:ethr:0x04:0xB9C5714089478a327F09197987f16f9E5d936E8a#owner"
        )

        val resolver = EthrDIDResolver.Builder()
            .build()
        dids.forEach {
            assertThat(resolver.canResolve(it)).isFalse()
        }
    }


    @Test
    fun `throws when registry is not configured`() {
        val rpc = mockk<JsonRPC>()
        val resolver =
            EthrDIDResolver.Builder()
                .addNetwork(EthrDIDNetwork("", "", rpc, "0x1"))
                .build()
        coAssert {
            resolver.resolve("did:ethr:0xb9c5714089478a327f09197987f16f9e5d936e8a")
        }.thrownError {
            isInstanceOf(IllegalArgumentException::class)
        }
    }

    @Test
    fun `throws DidResolverError when RPC returns error`() {
        val rpc = mockk<JsonRPC>()

        coEvery {
            rpc.ethCall(any(), any())
        } throws JsonRpcException(JSON_RPC_INTERNAL_ERROR_CODE, "fake error")

        val resolver = EthrDIDResolver.Builder()
            .addNetwork(EthrDIDNetwork("", "0xregistry", rpc, "0x1")).build()
        coAssert {
            resolver.lastChanged("0xb9c5714089478a327f09197987f16f9e5d936e8a", rpc, "0xregistry")
        }.thrownError {
            isInstanceOf(DidResolverError::class)
        }
    }

}
