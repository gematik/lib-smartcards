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
package de.gematik.smartcards.sdcom.apdu;

import de.gematik.smartcards.sdcom.MessageLayer;
import java.util.stream.IntStream;

/**
 * The purpose of this interface is specifying methods for an ISO/IEC 786-4 APDU layer.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public interface ApduLayer extends MessageLayer {

  /**
   * Sends given command APDU.
   *
   * @param apdu command APDU to be sent
   * @return corresponding {@link ResponseApdu}
   */
  default ResponseApdu send(final CommandApdu apdu) {
    return new ResponseApdu(send(apdu.getBytes()));
  } // */

  /**
   * Sends given {@link CommandApdu} and compares trailer of corresponding {@link ResponseApdu} to
   * expected trailers.
   *
   * @param cmdApdu command APDU to be sent
   * @param expectedTrailer values of trailer (SW1 SW2) expected in {@link ResponseApdu}
   * @return corresponding {@link ResponseApdu}
   * @throws IllegalArgumentException if the trailer in the {@link ResponseApdu} does not match any
   *     of the expected trailers
   */
  default ResponseApdu send(final CommandApdu cmdApdu, final int... expectedTrailer) {
    final ResponseApdu result = send(cmdApdu);

    final int trailer = result.getSw();

    if (IntStream.of(expectedTrailer).filter(i -> (trailer == i)).findFirst().isEmpty()) {
      // ... trailer of response APDU differs from all expected trailer
      //     => throw appropriate exception
      throw new IllegalArgumentException(
          String.format(
              "unexpected trailer: %s, rspApdu = %s", cmdApdu.explainTrailer(trailer), result));
    } // end fi

    return result;
  } // end method */

  /**
   * Sends given {@link CommandApdu} and compares corresponding {@link ResponseApdu} to given one.
   *
   * @param cmdApdu command APDU to be sent
   * @param expectedResponse name says it all
   * @return corresponding {@link ResponseApdu}
   * @throws IllegalArgumentException if corresponding {@link ResponseApdu} differs from expected
   *     {@link ResponseApdu}, see {@link ResponseApdu#difference(ResponseApdu)}
   */
  default ResponseApdu send(final CommandApdu cmdApdu, final ResponseApdu expectedResponse) {
    final var result = send(cmdApdu);
    final var diff = result.difference(expectedResponse);

    if (!diff.isEmpty()) {
      // ... difference found
      //     => throw an appropriate exception
      throw new IllegalArgumentException(
          String.format(
              "%s: %s, expectedResponse %s != %s receivedResponse",
              cmdApdu.explainTrailer(result.getTrailer()), diff, expectedResponse, result));
    } // end fi (diff?)

    return result;
  } // end method */

  /**
   * Sends given {@link CommandApdu} and compares corresponding {@link ResponseApdu} to given one
   * after applying a mask.
   *
   * @param cmdApdu command APDU to be sent
   * @param expectedResponse name says it all
   * @param mask masking expected and real Response APDU before comparing
   * @return corresponding {@link ResponseApdu}
   * @throws IllegalArgumentException if corresponding {@link ResponseApdu} differs from expected
   *     {@link ResponseApdu} after applying a {@code mask}, see {@link
   *     ResponseApdu#difference(ResponseApdu, ResponseApdu)}
   */
  default ResponseApdu send(
      final CommandApdu cmdApdu, final ResponseApdu expectedResponse, final ResponseApdu mask) {
    final ResponseApdu result = send(cmdApdu);
    final var diff = result.difference(expectedResponse, mask);

    if (!diff.isEmpty()) {
      // ... difference found
      //     => throw an appropriate exception
      throw new IllegalArgumentException(
          String.format(
              "%s: %s, expectedResponse %s != %s receivedResponse",
              cmdApdu.explainTrailer(result.getTrailer()), diff, expectedResponse, result));
    } // end fi (diff?)

    return result;
  } // end method */
} // end interface
