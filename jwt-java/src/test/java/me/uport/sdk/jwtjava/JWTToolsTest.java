package me.uport.sdk.jwtjava;

import kotlin.Triple;
import me.uport.sdk.ethrdid.EthrDIDResolver;
import me.uport.sdk.jwt.model.JwtHeader;
import me.uport.sdk.jwt.model.JwtPayload;
import me.uport.sdk.jwt.test.EthrDIDTestHelpers;
import me.uport.sdk.signer.KPSigner;
import me.uport.sdk.testhelpers.TestTimeProvider;
import me.uport.sdk.universaldid.UniversalDID;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;

public class JWTToolsTest {

    @Test
    public void creates_a_token_with_completable_future_result() {

        JWTTools tools = new JWTTools(
                new TestTimeProvider(1234L),
                null
        );

        HashMap<String, Object> put = new HashMap<>();
        put.put("hello", "world");

        KPSigner signer = new KPSigner("0x1234");
        String issuer = "did:ethr:" + signer.getAddress();
        CompletableFuture<String> jwtFuture = tools.createJWT(
                put,
                issuer,
                signer,
                -1,
                JwtHeader.ES256K);

        String token = jwtFuture.join();
        assertEquals("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJoZWxsbyI6IndvcmxkIiwiaWF0IjoxLCJpc3MiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.04NvsvvAQH75H0k28fC2Pu9L9oSC5vQhF6RkWYCQXvl8TtVKugwJaY1seTiHl0N0yQcIn0GT26L9djDrc95DSg", token);
    }

    @Test
    public void verifies_a_token_using_completable_future_result() {

        EthrDIDResolver resolver = EthrDIDTestHelpers.getMockResolverForAddress("0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed");
        UniversalDID.INSTANCE.registerResolver(resolver);

        JWTTools tools = new JWTTools();
        CompletableFuture<JwtPayload> jwtPayloadCompletableFuture =
                tools.verify(
                        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJoZWxsbyI6IndvcmxkIiwiaWF0IjoxNTYxOTcxMTE5LCJpc3MiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.t5o1vzZExArlrrTVHmwtti7fnicXqvWrX6SS3F-Lu3budH7p6zQHjG8X7EvUTRUxhvr-eENCbXeteSE4rgF7MA",
                        false,
                        null);

        JwtPayload result = jwtPayloadCompletableFuture.join();
        assertEquals("did:ethr:0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed", result.getIss());
    }

    @Test
    public void decodes_a_token_into_triple_with_typed_payload() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJoZWxsbyI6IndvcmxkIiwiaWF0IjoxNTYxOTcxMTE5LCJpc3MiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.t5o1vzZExArlrrTVHmwtti7fnicXqvWrX6SS3F-Lu3budH7p6zQHjG8X7EvUTRUxhvr-eENCbXeteSE4rgF7MA";
        Triple<JwtHeader, JwtPayload, byte[]> decodedTriple = new JWTTools().decode(token);
        assertEquals(decodedTriple.component2().getIss(), "did:ethr:0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed");
    }

    @Test
    public void decodes_a_token_into_triple_with_map_payload() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksifQ.eyJoZWxsbyI6IndvcmxkIiwiaWF0IjoxNTYxOTcxMTE5LCJpc3MiOiJkaWQ6ZXRocjoweGNmMDNkZDBhODk0ZWY3OWNiNWI2MDFhNDNjNGIyNWUzYWU0YzY3ZWQifQ.t5o1vzZExArlrrTVHmwtti7fnicXqvWrX6SS3F-Lu3budH7p6zQHjG8X7EvUTRUxhvr-eENCbXeteSE4rgF7MA";
        Triple<JwtHeader, Map<String, Object>, byte[]> decodedTriple = new JWTTools().decodeRaw(token);
        assertEquals(decodedTriple.component2().get("iss"), "did:ethr:0xcf03dd0a894ef79cb5b601a43c4b25e3ae4c67ed");
    }

}