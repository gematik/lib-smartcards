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
package de.gematik.smartcards.g2icc.proxy;

import static de.gematik.smartcards.utils.AfiUtils.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import de.gematik.smartcards.crypto.EafiElcPukFormat;
import de.gematik.smartcards.crypto.EafiRsaPukFormat;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.crypto.Pkcs1Utils;
import de.gematik.smartcards.crypto.RsaPublicKeyImpl;
import de.gematik.smartcards.g2icc.cos.EafiCosAlgId;
import de.gematik.smartcards.pcsc.AfiPcsc;
import de.gematik.smartcards.pcsc.Icc;
import de.gematik.smartcards.pcsc.IccChannel;
import de.gematik.smartcards.pcsc.Ifd;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.sdcom.apdu.isoiec7816apdu.ReadBinary;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerOid;
import de.gematik.smartcards.utils.AfiOid;
import de.gematik.smartcards.utils.AfiRng;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.EafiHashAlgorithm;
import java.io.ByteArrayInputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class performing white-box tests for {@link IccProxy}.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({
  "PMD.ExcessiveImports",
  "PMD.MethodNamingConventions",
  "PMD.TestClassWithoutTestCases",
  "PMD.TooManyStaticImports"
})
class TestIccProxy {

  /** Logger. */
  /* package */ static final Logger LOGGER = LoggerFactory.getLogger(TestIccProxy.class); // */

  /** Random Number Generator. */
  /* package */ static final AfiRng RNG = new AfiRng(); // */

  /** Line separator. */
  private static final String LINE_SEPARATOR = AfiUtils.LINE_SEPARATOR + "      - "; // */

  /** Protocol used for card communication. */
  /* package */ static final String T_1 = "T=1"; // */

  /** Terminal factory. */
  private static final TerminalFactory TERMINAL_FACTORY = getFactory(); // */

  /** Card terminals. */
  /* package */ static final CardTerminals CARD_TERMINALS = TERMINAL_FACTORY.terminals(); // */

  /**
   * Get {@link TerminalFactory}.
   *
   * @return terminal factory
   */
  private static TerminalFactory getFactory() {
    try {
      return TerminalFactory.getInstance(AfiPcsc.TYPE, null, new AfiPcsc());
    } catch (NoSuchAlgorithmException e) {
      LOGGER.atError().log(UNEXPECTED, e);
      fail(UNEXPECTED, e);

      throw new AssertionError(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Select one card terminal from (possibly) many for Smart Card communication.
   *
   * <p>The method retrieves all card terminals and selects those with a smart card present of the
   * given type. If more than one card terminal has such a smart card present, then the user is
   * asked at the GUI which card terminal to use.
   *
   * @return card terminal with a smart card inserted
   * @throws RuntimeException if
   *     <ol>
   *       <li>no card terminals are available
   *     </ol>
   */
  /* package */
  static Ifd getCardTerminal(final Class<? extends IccProxy> expectedType) {
    final Set<Ifd> subList = getIfds(expectedType);
    final String selected;
    if (1 == subList.size()) { // NOPMD literal in if statement
      // ... just one IFD with status SmartCardPresent
      //     => use that
      selected = subList.iterator().next().getName();
    } else {
      // ... more than one IFD with the expected type

      /*/ implementation 1
      // Note: This implementation is useful if a user runs this tests and has
      //       a GUI.
      //     => ask user at GUI which to use

      // compile a panel with radio buttons
      final javax.swing.JPanel panel = new javax.swing.JPanel();
      panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
      final javax.swing.ButtonGroup buttonGroup = new javax.swing.ButtonGroup();

      subList.forEach(ifd -> {
        final javax.swing.JRadioButton radioButton = new javax.swing.JRadioButton(ifd.getName());
        radioButton.setActionCommand(ifd.getName());
        buttonGroup.add(radioButton);
        panel.add(radioButton);
      }); // end forEach(ifd -> ...)

      while (selected.isEmpty()) {
        javax.swing.JOptionPane.showMessageDialog(
            null, // parentComponent
            panel, // message to display
            "Choose an IFD", // title
            javax.swing.JOptionPane.PLAIN_MESSAGE
        );

        final javax.swing.ButtonModel selection = buttonGroup.getSelection();

        if (null != selection) {
          selected = buttonGroup.getSelection().getActionCommand();
        } // end fi
      } // end While (nothing selected)
      // end implementation 1 */

      // implementation 2
      // Note: This implementation is useful for automatic tests.
      //     => use "first" element in set
      selected = subList.stream().toList().getFirst().getName();
      // end implementation 2 */
    } // end else

    LOGGER.atTrace().log("use IFD: {}", selected);

    return (Ifd) CARD_TERMINALS.getTerminal(selected);
  } // end method */

  /**
   * Select all card terminals for Smart Card communication.
   *
   * <p>The method retrieves all card terminals with a smart card present of the given type.
   *
   * @return card terminal with a smart card inserted
   * @throws RuntimeException if
   *     <ol>
   *       <li>no card terminals are available
   *     </ol>
   */
  /* package */
  @SafeVarargs
  static Set<Ifd> getIfds(final Class<? extends IccProxy>... expectedType) {
    final Set<Ifd> result = new HashSet<>();

    try {
      for (final CardTerminal ifd : CARD_TERMINALS.list(CardTerminals.State.CARD_PRESENT)) {
        try {
          final Icc icc = (Icc) ifd.connect(T_1);
          final IccProxy iccProxy = IccProxy.getInstance(icc);
          icc.disconnect(true);

          final List<Class<?>> superClasses =
              List.of(
                  iccProxy.getClass(),
                  iccProxy.getClass().getSuperclass(),
                  iccProxy.getClass().getSuperclass().getSuperclass());

          for (final Class<? extends IccProxy> i : expectedType) {
            if (superClasses.contains(i)) {
              result.add((Ifd) ifd);
            } // end fi
          } // end For (i...)
        } catch (CardException e) {
          // ... possibly protocol mismatch if a T=0 only card is also present in an IFD
          //     => log the exception, but continue the loop
          LOGGER.atTrace().log(UNEXPECTED, e);
        } // end Catch (...)
      } // end For (ifd...)
    } catch (CardException e) {
      LOGGER.atInfo().log(UNEXPECTED, e);
    } // end Catch (...)

    if (result.isEmpty()) {
      fail("no proper ICC available");
    } // end fi
    // ... at least one IFD available, show them all

    LOGGER
        .atTrace()
        .log(
            "Number of IFD with appropriate type: {}{}",
            result.size(),
            String.format(
                "%n      - %s",
                result.stream()
                    .map(CardTerminal::getName)
                    .collect(Collectors.joining(AfiUtils.LINE_SEPARATOR))));

    return result;
  } // end method */

  /**
   * Test a private ELC key with algorithm {@code signEcdsa}.
   *
   * @param iccChannel used for communication
   * @param aid application identifier where to find the private key
   * @param fileIdentifier if non-negative, then file identifier where to find an X.509 certificate
   *     for the private key
   * @param keyReference key reference of a private key
   */
  /* package */ void zzzSignEcdsa(
      final IccChannel iccChannel,
      final String aid,
      final int fileIdentifier,
      final int keyReference) {
    LOGGER.atDebug().log(
        "start test_signEcdsa: AID = {}, keyReference = {}",
        aid,
        String.format("%02x", keyReference));

    // Assertions:
    // ... a. application with given AID is present
    // ... b. public key can be read either from transparent EF or by GAKP command

    // Test strategy:
    // --- a. select application
    // --- b. get public key via GAKP
    // --- c. get public key from X.509 in transparent EF
    // --- d. sign a bunch of messages with algorithm signEcdsa

    try {
      final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

      // --- a. select application
      iccChannel.send(new CommandApdu(0x00, 0xa4, 0x04, 0x0c, aid), 0x9000);

      // --- b. get public key via GAKP
      ResponseApdu rsp =
          iccChannel.send(
              new CommandApdu(0x00, 0x46, 0x81, keyReference, CommandApdu.NE_EXTENDED_WILDCARD),
              0x9000);
      final EcPublicKeyImpl pukGakp =
          new EcPublicKeyImpl(
              BerTlv.getInstance(
                  0x7f49,
                  AfiUtils.concatenate(
                      new DerOid(AfiOid.brainpoolP256r1).getEncoded(),
                      BerTlv.getInstance(rsp.getData()).getValueField())),
              EafiElcPukFormat.ISOIEC7816);

      // --- c. get public key from X.509 in transparent EF
      if (fileIdentifier >= 0) {
        iccChannel.send(
            new CommandApdu(0x00, 0xa4, 0x02, 0x0c, String.format("%04x", fileIdentifier)), 0x9000);
        rsp = iccChannel.send(new ReadBinary(0, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000);
        final ByteArrayInputStream bais = new ByteArrayInputStream(rsp.getData());
        final X509Certificate x509 = (X509Certificate) certificateFactory.generateCertificate(bais);
        final EcPublicKeyImpl pukX509 = new EcPublicKeyImpl((ECPublicKey) x509.getPublicKey());
        LOGGER
            .atTrace()
            .log(
                "X.509:{}{}",
                AfiUtils.LINE_SEPARATOR,
                BerTlv.getInstance(rsp.getData()).toStringTree());

        assertEquals(pukGakp, pukX509);
      } // end fi

      // --- d. sign a bunch of messages with algorithm signEcdsa
      iccChannel.send(
          new CommandApdu(
              0x00,
              0x22,
              0x41,
              0xb6,
              AfiUtils.concatenate(
                  BerTlv.getInstance(0x84, new byte[] {(byte) keyReference}).getEncoded(),
                  BerTlv.getInstance(0x80, EafiCosAlgId.SIGN_ECDSA.getAlgId()).getEncoded())),
          0x9000);
      RNG.intsClosed(0, 0x1_0000, 5)
          .forEach(
              messageLength -> {
                final byte[] message = RNG.nextBytes(messageLength);
                final byte[] signature =
                    iccChannel
                        .send(
                            new CommandApdu(
                                0x00,
                                0x2a,
                                0x9e,
                                0x9a,
                                EafiHashAlgorithm.SHA_256.digest(message),
                                CommandApdu.NE_EXTENDED_WILDCARD),
                            0x9000)
                        .getData();

                assertTrue(pukGakp.verifyEcdsa(message, signature));
              }); // end forEach(messageLength -> ...)
      // end --- d.
    } catch (CertificateException | IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test a private RSA key with algorithm {@code signPKCS1_V1_5}.
   *
   * @param iccChannel used for communication
   * @param aid application identifier where to find the private key
   * @param fileIdentifier if non-negative, then file identifier where to find an X.509 certificate
   *     for the private key
   * @param keyReference key reference of a private key
   */
  /* package */ void zzzSignPkcs1V15(
      final IccChannel iccChannel,
      final String aid,
      final int fileIdentifier,
      final int keyReference) {
    LOGGER.atDebug().log(
        "start test_signPkcs1_V1_5: AID = {}, keyReference = {}",
        aid,
        String.format("%02x", keyReference));

    // Assertions:
    // ... a. application with given AID is present
    // ... b. public key can be read either from transparent EF or by GAKP command

    // Test strategy:
    // --- a. select application
    // --- b. get public key via GAKP
    // --- c. get public key from X.509 in transparent EF
    // --- d. sign a bunch of messages with algorithm signPKCS1_V1_5

    try {
      final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      // --- a. select application
      iccChannel.send(new CommandApdu(0x00, 0xa4, 0x04, 0x0c, aid), 0x9000);

      // --- b. get public key via GAKP
      ResponseApdu rsp =
          iccChannel.send(
              new CommandApdu(0x00, 0x46, 0x81, keyReference, CommandApdu.NE_EXTENDED_WILDCARD),
              0x9000);
      final RsaPublicKeyImpl pukGakp =
          new RsaPublicKeyImpl(
              (ConstructedBerTlv) BerTlv.getInstance(rsp.getBytes()), EafiRsaPukFormat.ISO7816);

      // --- c. get public key from X.509 in transparent EF
      if (fileIdentifier >= 0) {
        iccChannel.send(
            new CommandApdu(0x00, 0xa4, 0x02, 0x0c, String.format("%04x", fileIdentifier)), 0x9000);
        rsp = iccChannel.send(new ReadBinary(0, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000);
        final ByteArrayInputStream bais = new ByteArrayInputStream(rsp.getData());
        final X509Certificate x509 = (X509Certificate) certificateFactory.generateCertificate(bais);
        final RsaPublicKeyImpl pukX509 = new RsaPublicKeyImpl((RSAPublicKey) x509.getPublicKey());

        assertEquals(pukGakp, pukX509);
      } // end fi

      // --- d. sign a bunch of messages with algorithm signPKCS1_V1_5
      iccChannel.send(
          new CommandApdu(
              0x00,
              0x22,
              0x41,
              0xb6,
              AfiUtils.concatenate(
                  BerTlv.getInstance(0x84, new byte[] {(byte) keyReference}).getEncoded(),
                  BerTlv.getInstance(0x80, EafiCosAlgId.SIGN_PKCS_1_V_1_5.getAlgId())
                      .getEncoded())),
          0x9000);
      RNG.intsClosed(0, 0x1_0000, 5)
          .forEach(
              messageLength -> {
                final byte[] message = RNG.nextBytes(messageLength);
                final byte[] signature =
                    iccChannel
                        .send(
                            new CommandApdu(
                                0x00,
                                0x2a,
                                0x9e,
                                0x9a,
                                Pkcs1Utils.pkcs1DigestInfo(message, EafiHashAlgorithm.SHA_256)
                                    .getEncoded(),
                                CommandApdu.NE_EXTENDED_WILDCARD),
                            0x9000)
                        .getData();

                assertDoesNotThrow(() -> pukGakp.pkcs1RsaSsaPkcs1V15Verify(message, signature));
              }); // end forEach(messageLength -> ...)
      // end --- d.
    } catch (CertificateException | IllegalArgumentException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */

  /**
   * Test a private RSA key with algorithm {@code signPSS}.
   *
   * @param iccChannel used for communication
   * @param aid application identifier where to find the private key
   * @param fileIdentifier if non-negative, then file identifier where to find an X.509 certificate
   *     for the private key
   * @param keyReference reference of a private key
   */
  /* package */ void zzzSignPss(
      final IccChannel iccChannel,
      final String aid,
      final int fileIdentifier,
      final int keyReference) {
    LOGGER.atDebug().log(
        "start test_signPss: AID = {}, keyReference = {}",
        aid,
        String.format("%02x", keyReference));

    // Assertions:
    // ... a. application with given AID is present
    // ... b. public key can be read either from transparent EF or by GAKP command

    // Test strategy:
    // --- a. select application
    // --- b. get public key via GAKP
    // --- c. get public key from X.509 in transparent EF
    // --- d. sign a bunch of messages with algorithm signPSS

    try {
      final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
      // --- a. select application
      iccChannel.send(new CommandApdu(0x00, 0xa4, 0x04, 0x0c, aid), 0x9000);

      // --- b. get public key via GAKP
      ResponseApdu rsp =
          iccChannel.send(
              new CommandApdu(0x00, 0x46, 0x81, keyReference, CommandApdu.NE_EXTENDED_WILDCARD),
              0x9000);
      final RsaPublicKeyImpl pukGakp =
          new RsaPublicKeyImpl(
              (ConstructedBerTlv) BerTlv.getInstance(rsp.getBytes()), EafiRsaPukFormat.ISO7816);

      // --- c. get public key from X.509 in transparent EF
      if (fileIdentifier >= 0) {
        iccChannel.send(
            new CommandApdu(0x00, 0xa4, 0x02, 0x0c, String.format("%04x", fileIdentifier)), 0x9000);
        rsp = iccChannel.send(new ReadBinary(0, 0, CommandApdu.NE_EXTENDED_WILDCARD), 0x9000);
        final ByteArrayInputStream bais = new ByteArrayInputStream(rsp.getData());
        final X509Certificate x509 = (X509Certificate) certificateFactory.generateCertificate(bais);
        final RsaPublicKeyImpl pukX509 = new RsaPublicKeyImpl((RSAPublicKey) x509.getPublicKey());

        assertEquals(pukGakp, pukX509);
      } // end fi

      // --- d. sign a bunch of messages with algorithm signPKCS1_V1_5
      iccChannel.send(
          new CommandApdu(
              0x00,
              0x22,
              0x41,
              0xb6,
              AfiUtils.concatenate(
                  BerTlv.getInstance(0x84, new byte[] {(byte) keyReference}).getEncoded(),
                  BerTlv.getInstance(0x80, EafiCosAlgId.SIGN_PSS.getAlgId()).getEncoded())),
          0x9000);
      RNG.intsClosed(0, 0x1_0000, 5)
          .forEach(
              messageLength -> {
                final byte[] message = RNG.nextBytes(messageLength);
                final byte[] signature =
                    iccChannel
                        .send(
                            new CommandApdu(
                                0x00,
                                0x2a,
                                0x9e,
                                0x9a,
                                EafiHashAlgorithm.SHA_256.digest(message),
                                CommandApdu.NE_EXTENDED_WILDCARD),
                            0x9000)
                        .getData();

                assertDoesNotThrow(
                    () ->
                        pukGakp.pkcs1RsaSsaPssVerify(
                            message, signature, EafiHashAlgorithm.SHA_256));
              }); // end forEach(messageLength -> ...)
      // end --- d.
    } catch (CertificateException e) {
      fail(UNEXPECTED, e);
    } // end Catch (...)
  } // end method */
} // end class
