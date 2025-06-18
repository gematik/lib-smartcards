# Changelog
This is the changelog for a [Gradle][] (sub-)project `utils` providing
basic functionality and utility classes.

The changelog follows [Keep a Changelog v1.0.0][] i.e., each release has the
following sections (if non-empty):
- Summary: Git-commit message
- Added
- Changed
- Deprecated
- Removed
- Fixed
- Security

The versioning policy of this project follows [Semantic Versioning v2.0.0][].

## vx.y.z, 202x-yy-zz
Summary of changes in x.y.z since 0.8.5:
1. TODO

## v0.8.5, 2025-06-14
Summary of changes in 0.8.5 since 0.8.4
1. add
   1. annotation `@VisibleForTesting`
2. remove
   1. dependency to `com.google.guava:guava-annotations:r03`

## v0.8.4, 2025-05-26
Summary of changes in 0.8.4 since 0.8.3:
1. add
   1. constant `AfiUtils.LINE_SEPARATOR` 

## v0.8.3, 2025-05-05
Summary of changes in 0.8.3 since 0.8.2:
1. add
   1. collection of all boolean values, i.e.: `AfiUtils.VALUES_BOOLEAN`
   2. special value `AfiOid.INVALID`
2. changed
   1. constructor `AfiOid(byte[])` instead of throwing a
      `BufferUnderflowException` in case of invalid input, that constructor now
      throws an `IllegalArgumentException`

## v0.8.2, 2025-01-23
Summary of changes in 0.8.2 since 0.8.1:
1. added
   1. constant `AfiOid.OID_POPP_APDU_PACKET_SIGNER`
   2. constant `AfiOid.OID_POPP_TOKEN_SIGNER`

## v0.8.1, 2024-10-31
Fork from a non-public repository.


[Gradle]:https://gradle.org/
[Keep a Changelog v1.0.0]:http://keepachangelog.com/en/1.0.0/
[Semantic Versioning v2.0.0]:http://semver.org/spec/v2.0.0.html
