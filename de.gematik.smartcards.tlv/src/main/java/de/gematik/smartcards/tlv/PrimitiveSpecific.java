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
package de.gematik.smartcards.tlv;

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class for specific primitive DER-TLV objects.
 *
 * <p>Known subclasses:<br>
 * {@link DerBitString}, {@link DerBoolean}, {@link DerDate}, {@link DerEndOfContent}, {@link
 * DerInteger}, {@link DerNull}, {@link DerOctetString}, {@link DerOid}, {@link
 * DerRestrictedCharacterStringTypes} {@link DerUtcTime} {@link DerUtf8String}.
 *
 * <p>This is the superclass for all primitive DER-TLV objects with a specific interpretation of the
 * value-field. Such an interpretation is typically shown by {@link #getComment()}-method.
 *
 * <p>It is possible that the requirements of a subclass for its value-field is not fulfilled, e.g.
 * {@link DerEndOfContent} with a present value-field. Instead of throwing exceptions during the
 * construction of such DER-TLV objects violating their specification, the construction is allowed,
 * but the violation is registered and shown.
 *
 * <p>From the perspective of this class instances are immutable value-types, although {@link
 * PrimitiveBerTlv#equals(Object) equals()} and {@link PrimitiveBerTlv#hashCode() hashCode()} are
 * not overwritten because they are declared {@code final} in the superclass.
 *
 * <p>It follows that from the perspective of this class that object sharing is possible without
 * side effects.
 *
 * @param <T> type in value field
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
//         Short message: Classes that throw exceptions in their constructors
//             are vulnerable to Finalizer attacks.
//         That finding is not correct, because an empty finalize() declared
//             "final" is present in superclass.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW" // see note 1
}) // */
public abstract class PrimitiveSpecific<T> extends PrimitiveBerTlv implements DerSpecific {

  /** Findings in case TLV-object is not in accordance to its specification. */
  protected final List<String> insFindings = new ArrayList<>(); // */

  /**
   * Constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
   * </ol>
   *
   * @param tag of data object
   * @param valueField of data object
   */
  protected PrimitiveSpecific(final long tag, final byte[] valueField) {
    super(tag, valueField);
  } // end constructor */

  /**
   * Constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
   * </ol>
   *
   * @param tag the tag-field
   * @param buffer form which the TLV object is constructed
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws IllegalArgumentException if underlying constructors do so
   */
  protected PrimitiveSpecific(final byte[] tag, final ByteBuffer buffer) {
    // CT_CONSTRUCTOR_THROW
    super(tag, buffer);
  } // end constructor */

  /**
   * Constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because object is immutable.</i>
   * </ol>
   *
   * @param tag the tag-field
   * @param inputStream form which the TLV object is constructed
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Integer#MAX_VALUE}
   * @throws IOException if underlying methods do so
   * @throws IllegalArgumentException if underlying constructors do so
   */
  protected PrimitiveSpecific(final byte[] tag, final InputStream inputStream) throws IOException {
    // CT_CONSTRUCTOR_THROW
    super(tag, inputStream);
  } // end constructor */

  /**
   * Comment decoded value-field.
   *
   * <p>This method is used in case the value-field contains an octet-string. If that octet-string
   * is a valid TLV-object then that TLV-object is shown as {@link BerTlv#toStringTree()}.
   *
   * @param noIndentation indicates how often the indent parameter {@code delo} is used before
   *     printing the tag
   * @param decoded decoded value, here an octet-string
   * @return if {@code decoded} is not a valid TLV-object
   */
  /* package */ String commentDecoded(final int noIndentation, final byte[] decoded) {
    try {
      final StringBuilder result = new StringBuilder();

      // Note 1: The decoded octet-string possibly contains more than one
      //         TLV-object. Thus, assume the decoded octet-string to be the
      //         value-field of a constructed TLV-object.
      final List<BerTlv> list = ((ConstructedBerTlv) getInstance(0xfa, decoded)).insValueField;

      if (!list.isEmpty()) {
        // ... at least one element in list
        //     => add all elements to return value
        final String indentation = indentation("|  ", noIndentation).toString();

        // --- add prefix for commenting decoded TLV-object
        result.append(String.format("%n%s   ##########%n", indentation));

        // --- add decoded TLV-objects
        final String[] lines =
            list.stream()
                .map(BerTlv::toStringTree) // convert all list-elements toStringTree()
                .collect(Collectors.joining(LINE_SEPARATOR)) // join
                .split(LINE_SEPARATOR); // split into lines

        result
            // add indentation for each line of inner TLV-object
            .append(
                Arrays.stream(lines)
                    .map(line -> String.format("%s   # %s", indentation, line))
                    .collect(Collectors.joining(LINE_SEPARATOR)))
            // add suffix for commenting decoded TLV-object
            .append(String.format("%n%s   ##########", indentation));
      } // end fi

      return result.toString();
    } catch (RuntimeException e) { // NOPMD avoid catching generic exceptions
      // ... something went wrong, possibly decoded is not a valid TLV-object
      //     => do not comment decoded value
      return "";
    } // end Catch (...)
  } // end method */

  /**
   * Getter.
   *
   * @return decoded value
   */
  public abstract T getDecoded(); // */

  /**
   * Concatenates findings.
   *
   * @return concatenated findings
   */
  protected final String getFindings() {
    return insFindings.isEmpty()
        ? ""
        : insFindings.stream()
            .collect(
                Collectors.joining(
                    ", ", // delimiter
                    ", findings: ", // prefix
                    "" // suffix
                    ));
  } // end method */

  /**
   * Returns whether TLV-object is in accordance to its specification.
   *
   * @return {@code TRUE} if TLV-object is in accordance to its specification, {@code FALSE}
   *     otherwise
   * @see DerSpecific#isValid()
   */
  @Override
  public final boolean isValid() {
    return getFindings().isEmpty();
  } // end method */
} // end class
