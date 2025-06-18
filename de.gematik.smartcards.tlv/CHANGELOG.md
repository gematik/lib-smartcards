# Changelog
This is the changelog for a [Gradle][] (sub-)project with
classes supporting [ASN.1][] data objects according to [ISO/IEC 8824-1][] and
[ISO/IEC 8825-1][], see also [Wikipedia][].

The changelog follows [Keep a Changelog v1.0.0][] i.e., each release has the following
sections (if non-empty):
- Summary: Git-commit message
- Added
- Changed
- Deprecated
- Removed
- Fixed
- Security

The versioning policy of this project follows [Semantic Versioning v2.0.0][].

## vx.y.z, 202x-yy-zz
Summary of changes in x.y.z since 0.7.5:
1. TODO

## v0.7.5, 2025-06-14
Summary of changes in 0.7.5 since 0.7.4:
1. changed
   1. bump versions 
2. remove
   1. dependency to `com.google.guava:guava-annotations:r03`

## v0.7.4, 2025-05-26
Summary of changes in 0.7.4 since 0.7.3:
1. changed
   1. bump versions
2. removed
   1. deprecated constructor `PrimitiveSpecific(InputStream)`

## v0.7.3, 2025-05-05
Summary of changes in 0.7.3 since 0.7.2:
1. added
   1. `BerTlv.getInstance(ByteBuffer)` method (together with appropriate
      package-private methods and constructors in subclasses of `BerTlv`)
   2. constructor `PrimitiveSpecivic(byte[], InputStream)`
2. changed
   1. `BerTlv.getInstance(InputStream)` method no longer uses `mark(int)` and
      `reset()` methods. This way the `BerTlv.getInstance(InputStream)` method
      can be used with more classes implementing `InputStream`.
3. deprecated
   1. constructor `PrimitiveSpecific(InputStream)`

## v0.7.2, 2024-11-11
Summary of changes in 0.7.2 since 0.7.1:
1. added method `BerTlv.getNumberOfTag()`
2. changed constructors of class `ConstructedBerTlv` and its subclasses
   to use `Collection` instead of `List` as parameters
3. fixed: constructors of class `DerSet` now control the order of tags

## v0.7.1, 2024-10-31
Fork from a non-public repository.


[ASN.1]:https://en.wikipedia.org/wiki/ASN.1
[Gradle]:https://gradle.org/
[ISO/IEC 8824-1]:https://www.itu.int/rec/T-REC-X.680-202102-I/en
[ISO/IEC 8825-1]:https://www.itu.int/rec/T-REC-X.690-202102-I/en
[Keep a Changelog v1.0.0]:http://keepachangelog.com/en/1.0.0/
[Semantic Versioning v2.0.0]:http://semver.org/spec/v2.0.0.html
[Wikipedia]:https://en.wikipedia.org/wiki/X.690
