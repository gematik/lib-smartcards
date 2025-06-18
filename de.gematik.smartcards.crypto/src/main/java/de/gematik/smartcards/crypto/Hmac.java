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
package de.gematik.smartcards.crypto;

import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Arrays;

/**
 * This class calculates HMAC.
 *
 * <p>From the perspective of this class
 *
 * <ol>
 *   <li>Instances are immutable value-types. Thus, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()} are overwritten, but {@link Object#clone() clone()} isn't
 *       overwritten.
 *   <li>where data is passed in or out, defensive cloning is performed.
 *   <li>methods are thread-safe.
 * </ol>
 *
 * <p>It follows that from the perspective of this class object sharing is possible without side
 * effects.
 *
 * <p>The implementation of this class is in conformance to <a
 * href="https://www.rfc-editor.org/rfc/rfc2104">RFC 2104</a> and the description on <a
 * href="https://en.wikipedia.org/wiki/HMAC">Wikipedia</a>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.ShortClassName"})
public final class Hmac {

  /** Inner padding. */
  private final byte[] insIpad; // */

  /** Outer padding. */
  private final byte[] insOpad; // */

  /** Hash function. */
  private final EafiHashAlgorithm insHash; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Because only immutable instance attributes of this class are taken into account for
   *       this instance attribute, lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  private volatile int insHashCode; // NOPMD volatile not recommended */

  /**
   * Comfort constructor.
   *
   * @param key secret key of arbitrary length and content
   * @param hash hash function
   */
  public Hmac(final byte[] key, final EafiHashAlgorithm hash) {
    // --- set instance attribute hash
    insHash = hash;

    // --- calculate effective key
    final var blockLength = insHash.getBlockLength();
    final var keyDash = (key.length > blockLength) ? insHash.digest(key) : key.clone();

    // --- set instance attributes used for padding
    insIpad = new byte[blockLength];
    insOpad = new byte[blockLength];
    Arrays.fill(insIpad, (byte) 0x36);
    Arrays.fill(insOpad, (byte) 0x5c);

    for (int i = keyDash.length; i-- > 0; ) { // NOPMD assignment in operand
      insIpad[i] ^= keyDash[i];
      insOpad[i] ^= keyDash[i];
    } // end For (i...)
  } // end constructor */

  /**
   * Calculates HMAC for given octet string.
   *
   * @param data for which a HMAC is calculated
   * @return HMAC of given {@code data} according to <a
   *     href="https://www.rfc-editor.org/rfc/rfc2104">RFC 2104</a>
   */
  public byte[] doFinal(final byte[] data) {
    return getHash()
        .digest(
            AfiUtils.concatenate(
                getOpad(), getHash().digest(AfiUtils.concatenate(getIpad(), data))));
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is a direct subclass of Object calling
    //         super.equals(...) would be wrong.
    //         Instead, special checks are performed.

    // --- reflexive
    if (this == obj) {
      return true;
    } // end fi
    // ... obj not same as this

    // --- null
    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    // --- check type
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of RsaPublicKeyImpl

    final Hmac other = (Hmac) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    // Note: Intentionally, insOpad is NOT taken into account here, because
    //       if insIpad are equal, then insOpad are also equal.
    return this.getHash().equals(other.getHash()) && Arrays.equals(this.insIpad, other.insIpad);
  } // end method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... obviously attribute hashCode has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      final int hashCodeMultiplier = 31; // small prime number
      result = getHash().hashCode(); // start value
      result = result * hashCodeMultiplier + Arrays.hashCode(insIpad);

      // Note: Intentionally, insOpad is NOT taken into account here, because
      //       equals(Object)-method also does not take it into account.

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Getter.
   *
   * @return inner padding
   */
  private byte[] getIpad() {
    return insIpad;
  } // end method */

  /**
   * Getter.
   *
   * @return outer padding
   */
  private byte[] getOpad() {
    return insOpad;
  } // end method */

  /**
   * Getter.
   *
   * @return hash function
   */
  private EafiHashAlgorithm getHash() {
    return insHash;
  } // end method */
} // end class
