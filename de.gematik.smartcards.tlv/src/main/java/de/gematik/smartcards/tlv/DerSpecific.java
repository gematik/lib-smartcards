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

import java.io.InputStream;

/**
 * This interface enhances specific classes in this package willing to explain its content.
 *
 * <p>Typically classes implementing this interface check whether an instance is in accordance to
 * its specification. If not, then {@link #isValid()} indicates so. Typically {@link #getComment()}
 * will indicate where an instance differs from its specification.
 *
 * <p>In general classes implementing this interface will throw {@link IllegalArgumentException}
 * during the construction of an instance if parameters are not in accordance to a specification,
 * e.g. {@link DerBitString#DerBitString(int, byte[])}. Intentionally, if an instance is constructed
 * from an {@link java.io.InputStream} (e.g. {@link DerBitString#DerBitString(InputStream)})
 * throwing exceptions is discouraged and findings are collected and
 *
 * <p>SHOULD be accessible by {@link #getComment()}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public interface DerSpecific {

  /** {@link String} pre-pended to comment. */
  String DELIMITER = " # "; // */

  /**
   * Returns a comment describing the content of the object.
   *
   * @return comment about the TLV-object content
   */
  String getComment(); // */

  /**
   * Returns whether TLV-object is in accordance to its specification.
   *
   * @return {@code TRUE} if TLV-object is in accordance to its specification, {@code FALSE}
   *     otherwise
   */
  boolean isValid(); // */
} // end interface
