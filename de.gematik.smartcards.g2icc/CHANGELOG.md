# Changelog
This is the changelog for a [Gradle][] (sub-)project `g2icc` providing
functionality for communication with generation 2 smart cards.

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

# Assertions for releasing
For releasing (i.e. `./gradlew de.gematik.smartcards.g2icc:release`) the
following assertions apply:
1. see class `TestIccUser`:
   1. at least one `IccProxyEgk`
   2. at least one `IccProxyHba`
   3. at least one `IccProxySmcB`
   4. at least one `IccProxyGsmcK`
   5. at least one `IccProxyGsmcKt`
2. see class `TestTrustCenter`
   1. a configuration file is present and points to a directory valid for
      TrustCenter initialization

## vx.y.z, 202x-yy-zz
Summary of changes in x.y.z since 0.3.6:
1. TODO

## v0.3.6, 2025-06-14
Summary of changes in 0.3.6 since 0.3.5:
1. changed
   1. bump versions
2. remove
   1. dependency to `com.google.guava:guava-annotations:r03`

## v0.3.5, 2025-05-26
Summary of changes in 0.3.5 since 0.3.4:
1. bump versions

## v0.3.4, 2025-05-05
Summary of changes in 0.3.4 since 0.3.3:
1. bump versions

## v0.3.3, 2025-03-27
Summary of changes in 0.3.3 since 0.3.2:
1. added
   1. static method `TrustCenter.initializeCache()` 
2. changed
   1. value of suffix `TrustCenter.SUFFIX_CVC_DER`
3. removed
   1. folder "resources" with CVC-Root-CA and CVC-Sub-CA certificates,
      those artifacts are moved to another git project

## v0.3.2, 2024-11-11
Summary of changes in 0.3.2 since 0.3.1:
1. bump versions

## v0.3.1, 2024-11-01
Fork from a non-public repository.


[Gradle]:https://gradle.org/
[Keep a Changelog v1.0.0]:http://keepachangelog.com/en/1.0.0/
[Semantic Versioning v2.0.0]:http://semver.org/spec/v2.0.0.html
