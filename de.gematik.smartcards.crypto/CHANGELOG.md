# Changelog
This is the changelog for a [Gradle][] (sub-)project providing functionality for
cryptography.

The changelog follows [Keep a Changelog v1.0.0][] i.e., each release has the
following sections (if non-empty):
- Summary: Git-commit message
- Added
- Changed
- Deprecated
- Removed
- Fixed
- Security

Currently, the versioning policy of this project follows [Semantic Versioning v2.0.0][].

TODO-list:
1. Improve RSA key generation

## vx.y.z, 202x-yy-zz
Summary of changes in x.y.z since 0.6.6:
1. TODO

## v0.6.5, 2025-06-14
Summary of changes in 0.6.5 since 0.6.4:
1. add
   1. method `EcPrivateKeyImpl.sign(BigInteger)` 
2. changed
   1. bump versions
3. remove
   1. dependency to `com.google.guava:guava-annotations:r03`

## v0.6.4, 2025-05-26
Summary of changes in 0.6.4 since 0.6.3:
1. bump versions

## v0.6.3, 2025-05-05
Summary of changes in 0.6.3 since 0.6.2:
1. bump versions

## v0.6.2, 2024-11-11
Summary of changes in 0.6.2 since 0.6.1:
1. bump versions 

## v0.6.1, 2024-10-31
Fork from a non-public repository.

Summary of changes in 0.6.1 since forking:
1. rename method `Pkcs1Utils.pkcs1_emsa_v1_5(...)` to `Pkcs1Utils.pkcs1EmsaV15`
2. rename methods in class `RsaPrivateKeyImpl`:
   - `pkcs1_RsaDp` to `pkcs1RsaDp`
   - `pkcs1_RsaEs_Oaep_Decrypt` to `pkcs1RsaEsOaepDecrypt`
   - `pkcs1_RsaEs_Pkcs1_v1_5_Decrypt` to `pkcs1RsaEsPkcs1V15Decrypt`
   - `pkcs1_RsaSp1` to `pkcs1RsaSp1`
   - `pkcs1_RsaSsa_Pkcs1_v1_5_Sign` to `pkcs1RsaSsaPkcs1V15Sign`
   - `pkcs1_RsaSsa_Pss_Sign` to `pkcs1RsaSsaPssSign`
   - `sign_IsoIec9796_2_A4` to `signIsoIec9796p2A4`
   - `sign_IsoIec9796_2_A6` to `signIsoIec9796p2A6`
   - `sign_IsoIec9796_2_ds1` to `signIsoIec9796p2ds1`
   - `sign_IsoIec9796_2_ds2` to `signIsoIec9796p2ds2`
   - `sign_IsoIec9796_2_ds3` to `signIsoIec9796p2ds3` 
3. rename methods in class `RsaPublicKeyImpl`:
   - `pkcs1_RsaEp` to `pkcs1RsaEp`
   - `pkcs1_RsaEs_Oaep_Encrypt` to `pkcs1RsaEsOaepEncrypt`
   - `pkcs1_RsaEs_Pkcs1_V1_5_Encrypt` to `pkcs1RsaEsPkcs1V15Encrypt`
   - `pkcs1_RsaSsa_Pkcs1_v1_5_Verify` to `pkcs1RsaSsaPkcs1V15Verify`
   - `pkcs1_RsaSsa_Pss_Verify` to `pkcs1RsaSsaPssVerify`
   - `pkcs1_RsaVp1` to `pkcs1RsaVp1` 
   - `verify_IsoIec9796_2_ds1` to `verifyIsoIec9796p2ds1`
   - `verify_IsoIec9796_2_ds2` to `verifyIsoIec9796p2ds2`
   - `verify_IsoIec9796_2_ds3` to `verifyIsoIec9796p2ds3`


[Gradle]:https://gradle.org/
[Keep a Changelog v1.0.0]:http://keepachangelog.com/en/1.0.0/
[Semantic Versioning v2.0.0]:http://semver.org/spec/v2.0.0.html
