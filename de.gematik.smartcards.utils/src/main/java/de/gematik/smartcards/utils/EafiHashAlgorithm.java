/*
 * Copyright (Date see Readme), gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.smartcards.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Enumeration of popular hash-functions.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum EafiHashAlgorithm {
  /** Represents <a href="https://en.wikipedia.org/wiki/MD2_(hash_function)">MD2</a> algorithm. */
  MD2(
      AfiOid.MD2,
      48,
      16,
      // "30-20" + "30-0c-[(06-08-" + AfiOid.MD2.getOctetString() + ")0500]"        + "04-10-...",
      "MD2",
      0x80), // TODO change 0x80 to real value from ISO/IEC 10118-3 */

  /** Represents <a href="https://en.wikipedia.org/wiki/MD5">MD5</a> algorithm. */
  MD5(
      AfiOid.MD5,
      64,
      16,
      // "30-20" + "30-0c-[(06-08-" + AfiOid.MD5.getOctetString() + ")05-00]"       + "04-10-...",
      "MD5",
      0x81), // TODO change 0x81 to real value from ISO/IEC 10118-3 */

  /** Represents <a href="https://en.wikipedia.org/wiki/RIPEMD">RIPEMD 160</a> algorithm. */
  RIPEMD_160(
      AfiOid.RIPEMD160,
      64,
      20,
      // "30-21" + "30-09-[(06-05-" + AfiOid.RIPEMD160.getOctetString() + ")05_00]" + "04-14-...",
      "RIPEMD160",
      0x31), // */

  /** Represents <a href="https://en.wikipedia.org/wiki/SHA-1">SHA-1</a> algorithm. */
  SHA_1(
      AfiOid.SHA1,
      64,
      20,
      // "30-21" + "30-09-[(06-05-" + AfiOid.SHA1.getOctetString() + ")05.00]"      + "04-14-...",
      "SHA-1",
      0x33), // */

  /** Represents <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-224</a> algorithm. */
  SHA_224(
      AfiOid.SHA224,
      64,
      28,
      // "30-2d" + "30-0d-[(06-09-" + AfiOid.SHA224.getOctetString() + ")(05 00)]"  + "04-1c-...",
      "SHA-224",
      0x38), // */

  /** Represents <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-256</a> algorithm. */
  SHA_256(
      AfiOid.SHA256,
      64,
      32,
      // "30-31" + "30-0d-[(06-09." + AfiOid.SHA256.getOctetString() + ")05-00]"    + "04-20-...",
      "SHA-256",
      0x34), // */

  /** Represents <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-384</a> algorithm. */
  SHA_384(
      AfiOid.SHA384,
      128,
      48,
      // "30-41" + "30-0d-[(06-09_" + AfiOid.SHA384.getOctetString() + ")(0500)]"   + "04-30-...",
      "SHA-384",
      0x36), // */

  /** Represents <a href="https://en.wikipedia.org/wiki/SHA-2">SHA-512</a> algorithm. */
  SHA_512(
      AfiOid.SHA512,
      128,
      64,
      // "30-51" + "30-0d-[(06-09 " + AfiOid.SHA512.getOctetString() + ")05 00]"    + "04-40-...",
      "SHA-512",
      0x35), // */
  ;

  /** Name of algorithm. */
  private final String insAlgorithm; // */

  /**
   * Code of hash-function according to ISO/IEC 10118-3.
   *
   * <p>See also <a
   * href="https://javadoc.io/static/org.bouncycastle/bcprov-debug-jdk18on/1.77/org/bouncycastle/crypto/signers/ISOTrailers.html">ISO-Trailers</a>.
   */
  private final int insHashId; // */

  /** Number of octets per internal block. */
  private final int insBlockLength; // */

  /** Number of octets in hash-value. */
  private final int insDigestLength; // */

  /** Object identifier. */
  private final AfiOid insOid; // */

  /**
   * Comfort constructor.
   *
   * @param algorithm name of algorithm
   * @param code of hash-function according to ISO/IEC 10118-3
   */
  EafiHashAlgorithm(
      final AfiOid oid,
      final int blockLength,
      final int digestLength,
      final String algorithm,
      final int code) {
    insOid = oid;
    insBlockLength = blockLength;
    insDigestLength = digestLength;
    insAlgorithm = algorithm;
    insHashId = code;
  } // end constructor */

  /**
   * Pseudo constructor.
   *
   * @param oid used to identify the requested hash algorithm
   * @return hash algorithm corresponding to given OID
   * @throws IllegalArgumentException if an {@code oid} is given for which no hash algorithm exists
   */
  @SuppressWarnings({"PMD.CyclomaticComplexity"})
  public static EafiHashAlgorithm getInstance(final AfiOid oid) {
    if (AfiOid.SHA256.equals(oid)) {
      return SHA_256;
    } else if (AfiOid.SHA384.equals(oid)) {
      return SHA_384;
    } else if (AfiOid.SHA512.equals(oid)) {
      return SHA_512;
    } else if (AfiOid.SHA224.equals(oid)) {
      return SHA_224;
    } else if (AfiOid.SHA1.equals(oid)) {
      return SHA_1;
    } else if (AfiOid.MD5.equals(oid)) {
      return MD5;
    } else if (AfiOid.MD2.equals(oid)) {
      return MD2;
    } else if (AfiOid.RIPEMD160.equals(oid)) {
      return RIPEMD_160;
    } else {
      throw new IllegalArgumentException("no hash algorithm for OID = " + oid);
    } // end else
  } // end method */

  /**
   * Calculates a digest for the given message.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input array after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param message for which digest is calculated
   * @return digest of {@code message}
   */
  public byte[] digest(final byte[] message) {
    return getMessageDigest().digest(message);
  } // end method */

  /**
   * Calculates the digest for content of given path.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the file content
   *       after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param path to file
   * @return digest, calculated over the content of file
   * @throws IOException if underlying methods do so
   */
  public byte[] digest(final Path path) throws IOException {
    final MessageDigest digest = getMessageDigest();

    // Note: The following implementation intentionally does not use
    //       File.readAllBytes(...)-method, because for large files
    //       that would lead to huge memory footprint.
    final byte[] buffer = new byte[0x10_0000]; // => 1 MiB
    try (InputStream ins = new BufferedInputStream(Files.newInputStream(path), buffer.length)) {
      for (int noRead; -1 != (noRead = ins.read(buffer)); ) { // NOPMD assignment in operands
        digest.update(buffer, 0, noRead);
      } // end For (...)
    } // end try

    return digest.digest();
  } // end method */

  /**
   * Returns name of hash algorithm.
   *
   * @return name usable in {@link MessageDigest#getInstance(String)} method
   */
  public String getAlgorithm() {
    return insAlgorithm;
  } // end method */

  /**
   * Returns number of octet per internal block.
   *
   * @return number of octet per internal block
   */
  public int getBlockLength() {
    return insBlockLength;
  } // end method */

  /**
   * Returns number of octet in a hash value.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is primitive.</i>
   * </ol>
   *
   * @return length of hash value in octet
   */
  public int getDigestLength() {
    return insDigestLength;
  } // end method */

  /**
   * Returns a {@link MessageDigest}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>A message-digest returned by this method is freshly created. Thus, each call returns a
   *       new message-digest which is independent from any other message-digest returned before or
   *       after. This ensures thread-safety if a message-digest is used just in one thread.</i>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is never used again
   *       within this class.</i>
   * </ol>
   *
   * @return freshly created message-digest
   * @throws AssertionError if no provider exists for given {@code algorithm}
   */
  public MessageDigest getMessageDigest() {
    try {
      return MessageDigest.getInstance(insAlgorithm);
    } catch (final NoSuchAlgorithmException e) {
      throw new AssertionError("no such algorithm: " + insAlgorithm, e);
    } // end Catch (...)
  } // end method */

  /**
   * Returns hash-function identifier.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the return value is primitive.</i>
   * </ol>
   *
   * @return hash-function identifier as defined in ISO/IEC 10118-3
   */
  public int getHashId() {
    return insHashId;
  } // end method */

  /**
   * Returns {@link AfiOid} of hash algorithm.
   *
   * @return object identifier of hash algorithm
   */
  public AfiOid getOid() {
    return insOid;
  } // end method */

  /**
   * Generic mask generation function.
   *
   * <p>The behavior is specified in {@code [gemSpec_COS#(N001.100)]}:
   *
   * <ol>
   *   <li>If startValue is 0 then MGF according to PKCS#1 Annex B.2.1 or ISO/IEC 9796-2 Annex B.
   *   <li>If startValue is 1 then MGF according to ANSI X9.63 clause 5.6.3.
   *   <li>Otherwise the output is not in accordance to a known standard.
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input array after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameters are only read or
   *       primitive and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param seed to be taken into account
   * @param length number of octet in returned {@code byte[]}
   * @param start of counter
   * @return output of mask generation function containing {@code outputLength} octet
   */
  public byte[] maskGenerationFunction(final byte[] seed, final int length, int start) {
    // convert startValue into byte[]
    final byte[] counter = new byte[4];
    counter[3] = (byte) start;
    start >>= 8; // NOPMD reassigned parameter
    counter[2] = (byte) start;
    start >>= 8;
    counter[1] = (byte) start;
    start >>= 8;
    counter[0] = (byte) start;

    // initializations
    final byte[] result = new byte[length];
    final byte[] hashInput = AfiUtils.concatenate(seed, counter);

    // define some loop variables
    final MessageDigest digest = getMessageDigest();

    // Note 1: Setting the hash-variable here prevents a compiler error in the catch-block.
    // Note 2: Code-checker pmd complains about "initialized value never used" (which is correct).
    byte[] hash = AfiUtils.EMPTY_OS; // NOPMD initializer never used
    int index = 0;
    try {
      for (; index < result.length; index += hash.length) {
        digest.reset();
        hash = digest.digest(hashInput);
        System.arraycopy(hash, 0, result, index, hash.length);

        // increment counter
        // Note: AfiUtils.incrementCounter(byte[])-method works on whole hashInput,
        //       as long as the 4-byte counter doesn't "overflow" this is not a problem.
        AfiUtils.incrementCounter(hashInput);
      } // end For (index...)
    } catch (final ArrayIndexOutOfBoundsException e) {
      // ... outputLength is not a multiple of hash.length
      System.arraycopy(hash, 0, result, index, result.length - index);
    } // end Catch (...)

    return result;
  } // end method */
} // end enumeration
