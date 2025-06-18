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
package de.gematik.smartcards.g2icc.cvc;

import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.PrimitiveBerTlv;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.Collections;
import java.util.List;

/**
 * This class defines common functionality for all components of a {@link Cvc}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public abstract class CvcComponent {

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the visibility of this instance attribute is "default" (i.e. package
   *       private). Thus, subclasses in this package are able to get and set it.</i>
   *   <li><i>Because only immutable instance attributes of this class and all subclasses are taken
   *       into account for this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  /* package */ volatile int insHashCode; // NOPMD volatile */

  /** Value-field of {@link PrimitiveBerTlv} used during construction. */
  private final BerTlv insDataObject; // */

  /**
   * Comfort constructor.
   *
   * @param tlv primitive data
   */
  /* package */ CvcComponent(final PrimitiveBerTlv tlv) {
    insDataObject = tlv;
  } // end constructor */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable and
   *       return value is primitive.</i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public final boolean equals(final @CheckForNull Object obj) {
    // Note 1: Because this class is a direct subclass of Object calling
    //         super.equals(...) would be wrong. Instead, special checks are
    //         performed.

    // --- reflexive
    if (this == obj) {
      return true;
    } // end fi
    // ... obj not same as this

    if (null == obj) {
      // ... this differs from null
      return false;
    } // end fi
    // ... obj not null

    // Note 2: Although this is an abstract class we check the classes of this
    //         and obj. Thus, this check isn't necessary in subclasses.
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of CvcComponent

    final CvcComponent other = (CvcComponent) obj;

    // --- compare primitive instance attributes
    // -/-

    // --- compare reference types
    // ... assertion: instance attributes are never null
    return this.insDataObject.equals(other.insDataObject);
  } // end method */

  /**
   * Returns {@link BerTlv} representation.
   *
   * @return {@link BerTlv} representation
   */
  /* package */
  final BerTlv getDataObject() {
    return insDataObject;
  } // end method */

  /**
   * Explanation of content of this component.
   *
   * @return {@link List} of {@link String} with explanation
   */
  /* package */
  abstract List<String> getExplanation(); // */

  /**
   * Report of findings in this component.
   *
   * <p>A finding which is reported is something unexpected, possibly in or not in accordance to
   * gematik specifications.
   *
   * @return {@link List} of {@link String} with findings
   */
  /* package */ List<String> getReport() {
    return Collections.emptyList();
  } // end default method */

  /**
   * Returns value-field of this CV-certificate component.
   *
   * @return value-field of CV-certificate component
   */
  public final byte[] getValue() {
    return getDataObject().getValueField();
  } // end method */

  /**
   * Signals critical findings.
   *
   * @return {@code TRUE} if the component is not in conformance to gemSpec_PKI, {@code FALSE}
   *     otherwise
   */
  /* package */ boolean hasCriticalFindings() {
    return false;
  } // end default method */

  /**
   * The implementation of this method fulfills the hashCode-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return hash-code of object
   * @see java.lang.Object#hashCode()
   */
  @Override
  public final int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into account just insDataObject we can do here the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes here some
    //         calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion: instance attributes are never null

      // --- take into account primitive instance attributes
      // -/-

      // --- take into account reference types (currently none)
      result = insDataObject.hashCode(); // start value

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */
} // end interface
