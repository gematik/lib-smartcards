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
package de.gematik.smartcards.pcsc;

import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.utils.Hex;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;

/**
 * Class collecting information about interface devices and cards.
 *
 * <p>The intention and idea behind this is as follows:
 *
 * <ol>
 *   <li>Upon construction a certain smart card provider is used.
 *   <li>That provider controls the underlying implementation.
 *   <li>Based on the functionality from {@code java.smartcardio} then a bunch of information is
 *       collected.
 *   <li>When different smart card providers are used, then the result of investigation can be
 *       compared.
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
final class InvestigateIfds {

  /** List of APDU covering all ISO cases. */
  /* package */ static final List<CommandApdu> COMMAND_APDUS =
      List.of(
          // ISO-case 3, SELECT EF.GDO with fileIdentifier, no response
          new CommandApdu(0x00, 0xa4, 0x02, 0x0c, "2f02"),
          // ISO-case 2, READ BINARY without shortFileIdentifier
          new ReadBinary(0, 0, CommandApdu.NE_SHORT_WILDCARD),
          // ISO-case 4, SELECT EF.ATR with fileIdentifier, retrieve FCP
          new CommandApdu(0x00, 0xa4, 0x02, 0x04, "2f01", CommandApdu.NE_EXTENDED_WILDCARD),
          // ISO-case 1, SELECT MF, no response
          new CommandApdu(0x00, 0xa4, 0x04, 0x0c)); // */

  /** Collection of all card terminals. */
  private final List<CardTerminal> insCardTerminals; // */

  /** Collection of card terminals without a smart card. */
  private final List<CardTerminal> insIfdEmpty; // */

  /**
   * Mapping of IFD to list with responses from ICC.
   *
   * <p>The first element contains the ATR of the ICC. The remaining elements contain the response
   * APDU for commands from {@link #COMMAND_APDUS}.
   */
  private final Map<String, List<String>> insIccResponses; // */

  /**
   * Constructor using an {@link IfdCollection}.
   *
   * @param ifdCollection used to collect all the other information
   */
  /* package */ InvestigateIfds(final CardTerminals ifdCollection) throws CardException {
    insCardTerminals = ifdCollection.list();
    insIfdEmpty = ifdCollection.list(CardTerminals.State.CARD_ABSENT);

    insIccResponses =
        ifdCollection.list(CardTerminals.State.CARD_PRESENT).stream()
            .collect(
                Collectors.toMap(
                    CardTerminal::getName, // key
                    ifd -> {
                      try {
                        final List<String> result = new ArrayList<>();
                        final Card icc = ifd.connect("T=1");
                        CardChannel cc = icc.getBasicChannel();

                        result.add(Hex.toHexDigits(icc.getATR().getBytes()));

                        for (final CommandApdu cmd : COMMAND_APDUS) {
                          final byte[] rsp =
                              (cc instanceof IccChannel)
                                  ? ((IccChannel) cc).send(cmd).getBytes()
                                  : cc.transmit(new CommandAPDU(cmd.getBytes())) // NOPMD new in lo.
                                      .getBytes();

                          result.add(Hex.toHexDigits(rsp));
                        } // end For (cmd...)

                        // open another logical channel and issue the commands again
                        cc = icc.openLogicalChannel();

                        for (final CommandApdu cmd : COMMAND_APDUS) {
                          final byte[] rsp =
                              (cc instanceof IccChannel)
                                  ? ((IccChannel) cc).send(cmd).getBytes()
                                  : cc.transmit(new CommandAPDU(cmd.getBytes())) // NOPMD new in lo.
                                      .getBytes();

                          result.add(Hex.toHexDigits(rsp));
                        } // end For (cmd...)

                        // disconnect
                        icc.disconnect(true);

                        return result;
                      } catch (CardException e) {
                        return List.of(e.toString());
                      } // end Catch
                    } // end computation of value
                    ));
  } // end constructor */

  /**
   * Constructor using a {@link TerminalFactory}.
   *
   * @param terminalFactory used to collect all the other information
   */
  /* package */ InvestigateIfds(final TerminalFactory terminalFactory) throws CardException {
    this(terminalFactory.terminals());
  } // end constructor */

  /**
   * Getter.
   *
   * @return list with all card terminals
   */
  public List<CardTerminal> getCardTerminals() {
    return insCardTerminals;
  } // end method */

  /**
   * Getter.
   *
   * @return list of card terminals without a smart card
   */
  public List<CardTerminal> getIfdEmpty() {
    return insIfdEmpty;
  } // end method */

  /**
   * Getter.
   *
   * @return mapping from card reader name to responses from ICC.
   */
  public Map<String, List<String>> getIccResponses() {
    return insIccResponses;
  } // end method */
} // end class
