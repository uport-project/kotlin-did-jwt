# kotlin-did-jwt
[![](https://jitpack.io/v/uport-project/kotlin-did-jwt.svg)](https://jitpack.io/#uport-project/kotlin-did-jwt)
[![CircleCI](https://circleci.com/gh/uport-project/kotlin-did-jwt.svg?style=svg)](https://circleci.com/gh/uport-project/kotlin-did-jwt)
[![Twitter Follow](https://img.shields.io/twitter/follow/uport_me.svg?style=social&label=Follow)](https://twitter.com/uport_me)

This is the Kotlin implementation of the basic JWT methods for DID-JWTs

# did-jwt

The kotlin-did-JWT library allows you to sign and verify
[JSON Web Tokens (JWT)](https://tools.ietf.org/html/rfc7519) using ES256K, and ES256K-R algorithms. 

Public keys are resolved using the 
[Decentralized ID (DID)](https://w3c-ccg.github.io/did-spec/#decentralized-identifiers-dids)
of the signing identity of the claim, which is passed as the `iss` attribute of the encoded JWT.

## DID methods

We currently support the following DID methods:

- [`ethr`](https://github.com/uport-project/ethr-did-resolver)
- [`uport`](https://github.com/uport-project/uport-did-resolver)
- [`web`](https://github.com/uport-project/https-did-resolver)
- [`https`](https://github.com/uport-project/https-did-resolver) *DEPRECATED*


Defaults are automatically installed but you can customize to fit your needs.

Support for other DID methods should be simple.
Write a DID resolver supporting the 
[`DIDResolver`](https://github.com/uport-project/kotlin-did-jwt/blob/master/universal-did/src/main/java/me/uport/sdk/universaldid/DIDResolver.kt)
interface.
Install it using `UniversalDID.registerResolver(<your own resolver implementation>)`
Once you've verified that it works, please add a PR adding it to the above list so people can find it.

If your DID method requires a different signing algorithm than what is already supported, 
please create a PR.

## Installation

The libraries built here are distributed through [jitpack](https://jitpack.io/)

In your main `build.gradle` file, add:

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
        //...
    }
}
```

In your application `build.gradle` file, add:

```groovy
def did_jwt_version = "0.2.0"
dependencies {
    //...
    implementation "com.github.uport-project.kotlin-did-jwt:jwt:$did_jwt_version"
}
```

## Example

### 1. Create a did-JWT

In practice you should secure the key passed to KPSigner. 
The key provided in code below is for informational purposes.

```kotlin
val jwt = JWTTools()
//...
val payload = mapOf(
        "claims" to mapOf("name" to "R Daneel Olivaw")
)

val signer = KPSigner("0x54ece214d38fe6b46110a21c69fd55230f09688bf85b95fc7c1e4e160441ece1")
val issuerDID = "did:ethr:${signer.getAddress()}"

val token = jwt.createJWT(payload, issuerDID, signer)
```


### 2. Decode a did-JWT

Try decoding the JWT.  You can also do this using [jwt.io](https://jwt.io)

```kotlin
val (header, payload, sig) = jwt.decodeRaw("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NkstUiJ9.eyJjbGFpbXMiOnsibmFtZSI6IlIgRGFuZWVsIE9saXZhdyJ9LCJpYXQiOjEyMzQ1Njc4LCJleHAiOjEyMzQ1OTc4LCJpc3MiOiJkaWQ6ZXRocjoweDQxMjNjYmQxNDNiNTVjMDZlNDUxZmYyNTNhZjA5Mjg2YjY4N2E5NTAifQ.o6eDKYjHJnak1ylkpe9g8krxvK9UEhKf-1T0EYhH8pGyb8MjOEepRJi8DYlVEnZno0DkVYXQCf3u1i_HThBKtAA")

```

The decoded payload resembles:

```kotlin
mapOf(
    "claims" to mapOf("name" to "R Daneel Olivaw"),
    "iat" to 1.2345678E7,
    "exp" to 1.2345978E7,
    "iss" to "did:ethr:0x4123cbd143b55c06e451ff253af09286b687a950"
)
```

You can also use `jwt.decode("<token>")` to get a `JwtPayload` object instead of a map
but that is a more rigid structure and will be phased away in future releases. 

### 3. Verify a did-JWT


```kotlin
val payload : JwtPayload = JwtTools().verify("<token>")
```

If the token is valid, the method returns the decoded payload,
otherwise throws a `InvalidJWTException` or `JWTEncodingException`

Verifying a token means checking that the signature was produced by a
key associated with the issuer DID (`iss` field).

This association is resolved by a DID resolver, which can produce a `DIDDocument`
listing various public keys and service endpoints for a given DID.

#### Audience verification

If the token contains a non null `aud` field, an additional soft-check is performed to
match the verification against an intended audience. 
This same `aud` DID must be supplied to the `verify()` method for the token to be marked as valid
(after passing all the cryptographic checks as well).

Generally your app will have its own DID which should always be passed to the `verify` method
so that only tokens intended for your app are considered valid. 


## CHANGELOG

* 0.2.1
    - add support for web DID, deprecating https DID (#5)
    - allow creation of JWTs with no expiry (#6)
    - fallback to ES256K-R style verification if ES256K algorithm fails because of missing key encoding (#7)
    - [bugfix] delegate keys in ethr-did documents were not being resolved properly (#9)
* 0.2.0
    - [breaking] add audience checking for JWT verification (#2)
    - add `jwt-test` module with helpers for testing
* 0.1.2
    - fix crash when parsing legacy identity document 
* 0.1.1
    - initial stable release isolating the did-jwt implementation in kotlin along with resolvers