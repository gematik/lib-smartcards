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
package de.gematik.smartcards.pcsc.lib;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * The SCARD_IO_REQUEST struct defined in <a
 * href="https://github.com/LudovicRousseau/PCSC">pcsclite.h.in</a>.
 *
 * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
 * visibility of this class has to be "public".</i>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see <a href="https://github.com/jnasmartcardio/jnasmartcardio">jnasmartcardio</a>
 * @see <a href="https://docs.microsoft.com/en-us/windows/win32/secauthn/scard-io-request">MSDN
 *     SCARD_IO_REQUEST</a>
 */
// Note 1: According to the class comment of "com.sun.jna.Structure" it is
//         preferable to have a class annotation with the field order. If so,
//         then overriding getFieldOrder()-method is not necessary.
@Structure.FieldOrder({"dwProtocol", "cbPciLength"})
public final class ScardIoRequest extends Structure {

  /** Formatter used in {@link #toString()}-method. */
  private static final String FORMATTER =
      ScardIoRequest.class.getSimpleName() + "{dwProtocol: %s, cbPciLength: %s}"; // */

  /**
   * Protocol in use.
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   */
  public Dword dwProtocol = new Dword(); // NOPMD no accessors */

  /**
   * Protocol Control Information Length.
   *
   * <p>Length, in bytes, of the <b>SCARD_IO_REQUEST</b> structure plus any following PCI-specific
   * information.
   *
   * <p><i><b>Note:</b> According to the implementation of the superclass {@link Structure} the
   * visibility of this instance attribute has to be "public".</i>
   */
  public Dword cbPciLength = new Dword(); // NOPMD no accessors */

  /** Default-constructor. */
  /* package */ ScardIoRequest() {
    super();
  } // end constructor */

  /**
   * Constructor using protocol indication.
   *
   * @param protocol used for transferring APDU
   */
  /* package */ ScardIoRequest(final long protocol) {
    super();

    dwProtocol = new Dword(protocol);
    cbPciLength = new Dword(this.size());
  } // end constructor */

  /**
   * Constructor using a {@link Pointer}.
   *
   * @param p pointer
   */
  /* package */ ScardIoRequest(final Pointer p) {
    super(p);
  } // end constructor */

  /**
   * Return {@link String} representation.
   *
   * @return name of class and values of instance attributes
   * @see com.sun.jna.Structure#toString()
   */
  @Override
  public String toString() {
    return String.format(FORMATTER, dwProtocol, cbPciLength);
  } // end method */
} // end class
