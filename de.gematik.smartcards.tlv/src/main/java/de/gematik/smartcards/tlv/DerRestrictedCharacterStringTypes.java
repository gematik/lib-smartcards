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

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Superclass for restricted character string types.
 *
 * <p>Known subclasses:<br>
 * {@link DerIa5String}, {@link DerPrintableString} {@link DerTeletexString}.
 *
 * <p>For more information about the encoding, see <a
 * href="https://www.itu.int/rec/T-REC-X.680-202102-I/en">ISO/IEC 8824-1:2021</a> clause 41.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
// Note 1: Spotbugs claims "CT_CONSTRUCTOR_THROW"
//         Short message: Classes that throw exceptions in their constructors
//             are vulnerable to Finalizer attacks.
//         That finding is not correct, because an empty finalize() declared
//             "final" is present in superclass.
// Note 2: Spotbugs claims: "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR"
//         Explanation: Calling an overridable method during in a constructor
//                      may result in the use of uninitialized data. It may
//                      also leak the "this" reference of the partially
//                      constructed object. Only static, final or private
//                      methods should be invoked from a constructor.
//                      See SEI CERT rule MET05-J.
//                      Ensure that constructors do not call overridable methods.
//         The finding is correct. Intentionally, the code is NOT changed to
//         avoid code duplication in subclasses.
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings({
  "CT_CONSTRUCTOR_THROW", // see note 1
  "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR" // see note 2
}) // */
public abstract class DerRestrictedCharacterStringTypes extends PrimitiveSpecific<String> {

  /** Message in case of invalid characters. */
  /* package */ static final String MESSAGE = "invalid characters"; // */

  /**
   * De-coded value of value-field.
   *
   * <p>Similar classes use lazy initialization for this attribute. Intentionally no lazy
   * initialization occurs here, because the value extracted from an {@link InputStream} is
   * immediately checked.
   */
  /* package */ final String insDecoded; // */

  /**
   * Comfort constructor using value.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe, because input parameter(s) are immutable.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are immutable.</i>
   * </ol>
   *
   * @param tag used for tag-field
   * @param value encoded in this primitive TLV object
   * @throws IllegalArgumentException if parameter {@code value} contains invalid characters, see
   *     {@link #invalidCharacters()}
   */
  /* package */ DerRestrictedCharacterStringTypes(final int tag, final String value) {
    // CT_CONSTRUCTOR_THROW
    super(tag, toBytes(tag, value));

    // --- set instance attribute
    insDecoded = value;

    if (invalidCharacters()) { // NOPMD MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR
      // ... invalid characters in value-field
      throw new IllegalArgumentException(MESSAGE);
    } // end fi
  } // end constructor */

  /**
   * Constructor reading length- and value-field from an {@link ByteBuffer}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param tag the tag-field
   * @param buffer form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws BufferUnderflowException if the length-field or the value-field ends early
   */
  /* package */
  @SuppressWarnings({"PMD.ConstructorCallsOverridableMethod"})
  DerRestrictedCharacterStringTypes(final byte[] tag, final ByteBuffer buffer) {
    // CT_CONSTRUCTOR_THROW
    super(tag, buffer);

    // --- set instance attribute
    insDecoded = fromBytes(insValueField); // NOPMD MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR

    check();
  } // end constructor */

  /**
   * Constructor reading length- and value-field from an {@link InputStream}.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is NOT thread-safe, because it is possible to change the content of
   *       the input parameter(s) while this constructor is running.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are only read and
   *       content is defensively cloned.</i>
   * </ol>
   *
   * @param tag the tag-field
   * @param inputStream form which the length- and value-field are read
   * @throws ArithmeticException if the length-field indicates a length greater than {@link
   *     Long#MAX_VALUE}
   * @throws IOException if underlying methods do so
   */
  /* package */
  @SuppressWarnings({"PMD.ConstructorCallsOverridableMethod"})
  DerRestrictedCharacterStringTypes(final byte[] tag, final InputStream inputStream)
      throws IOException {
    // CT_CONSTRUCTOR_THROW
    super(tag, inputStream);

    // --- set instance attribute
    insDecoded = fromBytes(insValueField); // NOPMD MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR

    check();
  } // end constructor */

  private void check() {
    if (invalidCharacters()) {
      // ... invalid characters in value-field
      insFindings.add(MESSAGE);
    } // end fi
  } // end method */

  /**
   * Converts given {@code byte[]} into corresponding {@link String}.
   *
   * <p>This is the inverse function to {@link #toBytes(int, String)}.
   *
   * @param octets to be converted
   * @return corresponding {@link String}
   */
  /* package */
  abstract String fromBytes(byte[] octets); // */

  /**
   * Returns a comment describing the content of the object.
   *
   * <p>Findings are added to the comment if present.
   *
   * @return comment about the TLV-object content, here {@link DerSpecific#DELIMITER} plus {@link
   *     #getDecoded()} value
   */
  /* package */
  final String getComment(final String type) {
    return DELIMITER + type + " := \"" + getDecoded() + '"' + getFindings();
  } // end method */

  /**
   * Returns value this universal, primitive TLV object encodes.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is immutable.</i>
   * </ol>
   *
   * @return value represented by this universal-class primitive TLV object of type restricted
   *     character string, i.e. {@link String}
   * @see PrimitiveSpecific#getDecoded()
   */
  @Override
  public final String getDecoded() {
    return insDecoded;
  } // end method */

  /**
   * Checks for invalid characters.
   *
   * @return {@code TRUE} if value-field contains invalid characters, {@code FALSE} otherwise
   */
  /* package */
  abstract boolean invalidCharacters(); // */

  /**
   * Converts given {@link String} to appropriate {@code byte[]}.
   *
   * <p>This is the inverse function to {@link #fromBytes(byte[])}.
   *
   * <p><i><b>Note:</b> Because this method is used in the constructor of this class it is not
   * possible to make it abstract and move its functionality to subclasses.</i>
   *
   * @param tag indicating type of restricted character string
   * @param value {@link String} to be converted
   * @return appropriate octet-string
   */
  /* package */
  static byte[] toBytes(final int tag, final String value) {
    return switch (tag) {
      case DerIa5String.TAG -> DerIa5String.toBytes(value);
      case DerPrintableString.TAG -> DerPrintableString.toBytes(value);
      case DerTeletexString.TAG -> DerTeletexString.toBytes(value);
      default ->
          throw new IllegalArgumentException(
              String.format("tag = '%x' not (yet) implemented", tag));
    }; // end Switch (tag)
  } // end method */
} // end class
