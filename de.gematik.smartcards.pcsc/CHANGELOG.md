# Changelog
This is the changelog for a [Gradle][] (sub-)project `pcsc` providing,
access to smart cards, see also the `README.md`.

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
For releasing (i.e. `./gradlew de.gematik.smartcards.pcsc:release`) the following
assertions apply, see classes `TestIcc`, `TestIccChannel`:
1. At least one IFD present with an ICC which provides the following features:
   * supports T=1 protocol only
   * supports logical channels
   * has an EF.GDO i.e., a transparent EF with shortFileIdentifier = 2.
   * responses with NoError = '9000' to all commands from list
     `InvestigateIfds#COMMAND_APDUS}`
2. At least one IFD present with an ICC which does _not_ support T=1.
3. At least one IFD present without an ICC.
4. No IFD is disconnected during test.
5. No ICC is inserted or removed during test.

_**Note:** Sometimes it happens that connection establishment fails.
It seems best to restart the pcsc-layer in such a case._

```shell
sudo service pcscd restart
```

## vx.y.z, 202x-yy-zz
Summary of changes in x.y.z since 0.4.5:
1. TODO

## v0.4.5, 2025-06-14
Summary of changes in 0.4.5 since 0.4.4:
1. changed
   1. bump versions
2. remove
   1. dependency to `com.google.guava:guava-annotations:r03`

## v0.4.4, 2025-05-26
Summary of changes in 0.4.4 since 0.4.3:
1. bump versions

## v0.4.3 2025-05-05
Summary of changes in 0.4.3 since 0.4.2:
1. bump versions
   1. `jna` from 5.15.0 to 5.17.0

## v0.4.2, 2024-11-11
Summary of changes in 0.4.2 since 0.4.1:
1. bump versions

## v0.4.1, 2024-11-01
Fork from a non-public repository.


[Gradle]:https://gradle.org/
[Keep a Changelog v1.0.0]:http://keepachangelog.com/en/1.0.0/
[Semantic Versioning v2.0.0]:http://semver.org/spec/v2.0.0.html
