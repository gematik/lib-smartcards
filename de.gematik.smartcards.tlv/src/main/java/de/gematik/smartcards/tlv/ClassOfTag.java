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

/**
 * This class provides an enumeration for encoding the class of tag.
 *
 * <p>For more information see <a href="https://www.itu.int/rec/T-REC-X.690-202102-I/en">ISO/IEC
 * 8825-1:2021</a> clause 8.1.2.2.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public enum ClassOfTag {
  /** Universal class. */
  UNIVERSAL(0x00),

  /** Application class. */
  APPLICATION(0x40),

  /** Context-specific class. */
  CONTEXT_SPECIFIC(0x80),

  /** Private class. */
  PRIVATE(0xc0);

  /** Instance attribute with encoding of tag-class. */
  private final int insEncoding; // */

  /**
   * Comfort constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This constructor is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive.</i>
   * </ol>
   *
   * @param encoding class specific encoding
   */
  ClassOfTag(final int encoding) {
    // Note: Enumeration constructors are implicitly private
    insEncoding = encoding;
  } // end constructor */

  /**
   * Returns encoding of tag-class.
   *
   * <p>For more information see ISO/IEC 8825:2008 clause 8.1.2.2, table 1.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because return value is primitive.</i>
   * </ol>
   *
   * @return encoding of tag-class.
   */
  public int getEncoding() {
    return insEncoding;
  } // end method */

  /**
   * Pseudo constructor.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) are primitive and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param leadingOctet of tag
   * @return corresponding class of tag
   */
  public static ClassOfTag getInstance(final byte leadingOctet) {
    final int classOfTag = leadingOctet & 0xc0;

    if (0x00 == classOfTag) { // NOPMD literal in if statement
      return UNIVERSAL;
    } else if (0x40 == classOfTag) { // NOPMD literal in if statement
      return APPLICATION;
    } else if (0x80 == classOfTag) { // NOPMD literal in if statement
      return CONTEXT_SPECIFIC;
    } else {
      return PRIVATE;
    } // end fi
  } // end method */
} // end enumeration
