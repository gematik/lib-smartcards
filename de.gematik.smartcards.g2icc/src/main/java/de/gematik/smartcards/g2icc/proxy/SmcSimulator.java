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

import de.gematik.smartcards.crypto.AfiElcParameterSpec;
import de.gematik.smartcards.crypto.AfiElcUtils;
import de.gematik.smartcards.crypto.EcPrivateKeyImpl;
import de.gematik.smartcards.crypto.EcPublicKeyImpl;
import de.gematik.smartcards.g2icc.cvc.TrustCenter;
import de.gematik.smartcards.sdcom.MessageLayer;
import de.gematik.smartcards.sdcom.apdu.ApduLayer;
import de.gematik.smartcards.sdcom.apdu.CommandApdu;
import de.gematik.smartcards.sdcom.apdu.ResponseApdu;
import de.gematik.smartcards.tlv.BerTlv;
import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.utils.AfiUtils;
import de.gematik.smartcards.utils.Hex;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class simulates a gSMC-KT.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.TooManyMethods"})
public final class SmcSimulator implements ApduLayer {

  /** Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SmcSimulator.class); // */

  /** Default response APDU. */
  private static final ResponseApdu EXECUTION_ERROR = new ResponseApdu("6400"); // */

  /** Verification error. */
  private static final ResponseApdu VERIFICATON_ERROR = new ResponseApdu("6a80"); // */

  /** Instruction code not supported, see ISO/IEC 7816-4. */
  private static final ResponseApdu INSTRUCTION_CODE_NOT_SUPPORTED = // NOPMD long name
      new ResponseApdu("6d00"); // */

  /** Class not supported, see ISO/IEC 7816-4. */
  private static final ResponseApdu CLASS_NOT_SUPPORTED = new ResponseApdu("6e00"); // */

  /** No error. */
  private static final ResponseApdu NO_ERROR = new ResponseApdu("9000"); // */

  /** EF.ATR. */
  private static final ResponseApdu EF_ATR =
      new ResponseApdu(
          Hex.toByteArray(
              """
                  e0 14
                  |  02 03 010007
                  |  02 03 010002
                  |  02 03 010007
                  |  02 03 010002
                  -------------------
                  5f52 0c 8066054445545343739621f3
                  -------------------
                  d0 03 040300
                  -------------------
                  d2 10 4445494658534c453738313434010000
                  -------------------
                  d3 10 545359534954434f5346433230020102
                  -------------------
                  d4 10 545359534954434f5353543230020101
                  -------------------
                  d6 10 54535953494944315049303031010000
                  -------------------
                  cf 15 000000000000000000000000000000000000000000
                  """),
          0x9000); // */

  /** EF.Version2. */
  private static final ResponseApdu EF_VERSION_2 =
      new ResponseApdu(
          Hex.toByteArray(
              """
                  ef 2b
                  |  c0 03 020000
                  |  c1 03 040300
                  |  c2 10 545359534954434f5353543230020101
                  |  c4 03 010000
                  |  c5 03 020000
                  |  c6 03 010000
                  """),
          0x9000); // */

  /** ICCSN. */
  private static final ResponseApdu ICCSN =
      new ResponseApdu(Hex.toByteArray("80276000000000000000"), 0x9000); // */

  /** CV-certificate of Sub-CA (special). */
  private static final ResponseApdu CVC_CA_E256 =
      new ResponseApdu(
          Hex.toByteArray(
              """
                  7f21 81d8
                  |    7f4e 8191
                  |    |    5f29 01 70
                  |    |    42 08 4445475858870222
                  |    |    7f49 4d
                  |    |    |    06 08 2a8648ce3d040302
                  |    |    |    86 41 04962f87dfaca7bf6047df6008abe318fe8324fa1509fa
                                          5f97edbf309831c3615f772fff60bdd7e8c65c969f3
                                          06743130c624766867875800208882db7db3c835f
                  |    |    5f20 08 4445475858120223
                  |    |    7f4c 13
                  |    |    |    06 08 2a8214004c048118
                  |    |    |    53 07 80000000000003
                  |    |    5f25 06 020300080001
                  |    |    5f24 06 030100070301
                  |    5f37 40 492a399fd015ece67b9ad696509f66a11b899bafb296f3a93eec61c0c8c19504
                               7c23b3ff3a8b8636e27bf0ef5078aa24b64c47e4a165d32037d5b43b61884486
                  """),
          0x9000); // */

  /** CV-certificate of End-Entity (special). */
  private static final ResponseApdu CVC_SMC_AUTD_E256 =
      new ResponseApdu(
          Hex.toByteArray(
              """
                  7f21 81da
                  |    7f4e 8193
                  |    |    5f29 01 70
                  |    |    42 08 4445475858120223
                  |    |    7f49 4b
                  |    |    |    06 06 2b2403050301
                  |    |    |    86 41 0403f54df417a6fd3e504cc7291d3a90f12087b50e85987070
                                         1512ca2f2269cfeea3c635950ea0e98048b7641a85d651de
                                         942620df3072173ac563e34ae8d52aa7
                  |    |    5f20 0c 000980276883110000107637
                  |    |    7f4c 13
                  |    |    |    06 08 2a8214004c048118
                  |    |    |    53 07 00000000000000
                  |    |    5f25 06 020401000204
                  |    |    5f24 06 020901000203
                  |    5f37 40 2cf76ac0fe205f37074e53ffc2f6e529384ba678523cb8e8058714036d11592c
                               77ff529bdf17b77a1f99137c64dec1baa79a455ac0c9abad23e48b59374ea234
                  """),
          0x9000); // */

  /** PrK.SMC.AUTD_RPS_CVC.E256. */
  private static final EcPrivateKeyImpl PRK_SMC_AUTD_RPS_CVC_E256 =
      new EcPrivateKeyImpl(
          new BigInteger("a94093f56fd172f81ad54d0551c7a870e07caa64a441e275230f3ebfa5779376", 16),
          AfiElcParameterSpec.brainpoolP256r1); // */

  /** Ephemeral private key. */
  private EcPrivateKeyImpl insEphemeralSelf =
      new EcPrivateKeyImpl(BigInteger.ONE, AfiElcParameterSpec.brainpoolP256r1); // */

  /** Public key of opponent. */
  private EcPublicKeyImpl insPukOpponent = insEphemeralSelf.getPublicKey(); // */

  /** Session key context. */
  private SoftwareContext insSoftwareContext; // */

  /** Execution time of the previous command-response pair in seconds. */
  private double insTime; // */

  /** Default constructor. */
  public SmcSimulator() {
    // --- establish a default session key context
    insSoftwareContext = new SoftwareContext(new byte[32], 128);

    // --- invalidate session by verifying a wrong MAC
    insSoftwareContext.verifyCryptographicChecksum(new byte[1], new byte[2]);
  } // end constructor */

  @SuppressWarnings("PMD.CyclomaticComplexity")
  /* package */ byte[] performCommand(final byte[] command) {
    final String cmd = Hex.toHexDigits(command);

    // spotless:off
    final ResponseApdu rsp = switch (cmd) {
      case "00a4040c", // SELECT MF (without AID)
           "00a4040c07d2760001448003", // SELECT MF (with AID)
           "002241a40684010a8001d4" // MSE Set PrK.SMC.AUTD_RPS_CVC.E256
          -> NO_ERROR;
      case "00b09d0000", "00b09d00000000" -> EF_ATR;
      case "00b0910000", "00b09100000000" -> EF_VERSION_2;
      case "00b0820200", "00b08202000000" -> ICCSN;
      case "00b0870000", "00b08700000000" -> CVC_CA_E256;
      case "00b08a0000", "00b08a00000000" -> CVC_SMC_AUTD_E256;
      default -> mainLoop(new CommandApdu(command));
    }; // end Switch (cmd)
    // spotless:on

    return rsp.getBytes();
  } // end method */

  /* package */ ResponseApdu mainLoop(final CommandApdu cmd) {
    // spotless:off
    return switch (cmd.getCla()) {
      case 0x10 -> generalAuthenticateStep1(cmd);
      case 0x00 -> switch (cmd.getIns()) {
        case 0x86 -> generalAuthenticateStep2(cmd);
        case 0x2a -> performSecurityOperation(cmd);
        default -> INSTRUCTION_CODE_NOT_SUPPORTED;
      }; // end Switch (INS)
      default -> CLASS_NOT_SUPPORTED;
    }; // end Switch (CLA)
    // spotless:on
  } // end method */

  /**
   * Step 1 of an asymmetric establishment of session keys.
   *
   * <p>The method performs steps from gemSpec_COS (N085.052).
   *
   * @param cmd {@link CommandApdu}
   * @return response according to (N085.052)h
   */
  /* package */ ResponseApdu generalAuthenticateStep1(final CommandApdu cmd) {
    // --- (N085.012) extract information from command data field
    final String keyRef =
        Hex.toHexDigits(
            ((ConstructedBerTlv) BerTlv.getInstance(cmd.getData()))
                .getPrimitive(0xc3)
                .orElseThrow()
                .getValueField());

    // --- (N085.052)a, compare the eight LSByte of keyRef with iccsn8
    // TODO

    // --- (N085.052)b, search for private key
    // not necessary in this implementation

    // --- (N085.052)c, search for public key
    insPukOpponent = TrustCenter.getPublicKey(keyRef);

    // --- (N085.052)d, compare affectedObjectPuK.expirationDate with pointInTime
    // TODO

    // --- (N085.052)e, compare domain parameter
    // TODO

    // --- (N085.052)f, generate ephemeral key pair
    insEphemeralSelf = new EcPrivateKeyImpl(PRK_SMC_AUTD_RPS_CVC_E256.getParams());

    // --- (N085.052)g, store key
    // no action in this implementation

    // --- (N085.052)h, create response APDU
    return new ResponseApdu(
        BerTlv.getInstance(
                0x7c,
                List.of(
                    BerTlv.getInstance(
                        0x85,
                        AfiElcUtils.p2osUncompressed(
                            insEphemeralSelf.getPublicKey().getW(), insEphemeralSelf.getParams()))))
            .getEncoded(),
        0x9000);
  } // end method */

  /**
   * Step 2 of the asymmetric establishment of session keys.
   *
   * <p>The method performs steps from gemSpec_COS (N085.056).
   *
   * @param cmd {@link CommandApdu}
   * @return response according to (N085.056)e
   */
  /* package */ ResponseApdu generalAuthenticateStep2(final CommandApdu cmd) {
    // --- (N085.056)a, extract public key from command data field
    final EcPublicKeyImpl ephemeralOpponent =
        new EcPublicKeyImpl(
            AfiElcUtils.os2p(
                ((ConstructedBerTlv) BerTlv.getInstance(cmd.getData()))
                    .getPrimitive(0x85)
                    .orElseThrow()
                    .getValueField(),
                insEphemeralSelf.getParams()),
            insEphemeralSelf.getParams());

    // --- (N085.056)b, check access rules of PrK.SMC.AUTD_RPS_CVC.E256
    // not necessary in this implementation

    // --- (N085.056)c, Diffie-Hellman
    final byte[] k1 = AfiElcUtils.sharedSecret(insEphemeralSelf, insPukOpponent);
    final byte[] k2 = AfiElcUtils.sharedSecret(PRK_SMC_AUTD_RPS_CVC_E256, ephemeralOpponent);
    final byte[] kd = AfiUtils.concatenate(k1, k2);

    // --- set up a session key context
    insSoftwareContext = new SoftwareContext(kd, 128);

    return NO_ERROR;
  } // end method */

  /**
   * Perform Security Operation (PSO) command.
   *
   * @param cmd {@link CommandApdu}
   * @return response
   */
  /* package */ ResponseApdu performSecurityOperation(final CommandApdu cmd) {
    final int p1p2 = (cmd.getP1() << 8) | cmd.getP2();

    return switch (p1p2) {
      case 0x00a2 -> psoVerifyCryptographicChecksum(cmd);
      case 0x8086 -> psoDecipher(cmd);
      case 0x8680 -> psoEncipher(cmd);
      case 0x8e80 -> psoComputeCryptographicChecksum(cmd);
      default -> EXECUTION_ERROR;
    }; // end Switch (p1p2)
  } // end method */

  /* package */ ResponseApdu psoComputeCryptographicChecksum(final CommandApdu cmd) {
    final byte[] cmdData = cmd.getData();
    final boolean flagIncrement = cmdData[0] == 1;
    final byte[] data = Arrays.copyOfRange(cmdData, 1, cmdData.length);

    return new ResponseApdu(
        insSoftwareContext.computeCryptographicChecksum(data, flagIncrement), 0x9000);
  } // end method */

  /* package */ ResponseApdu psoDecipher(final CommandApdu cmd) {
    final byte[] data = cmd.getData();

    return new ResponseApdu(insSoftwareContext.decipher(data), 0x9000);
  } // end method */

  /* package */ ResponseApdu psoEncipher(final CommandApdu cmd) {
    final byte[] message = cmd.getData();

    return new ResponseApdu(insSoftwareContext.encipher(message), 0x9000);
  } // end method */

  /* package */ ResponseApdu psoVerifyCryptographicChecksum(final CommandApdu cmd) {
    final ConstructedBerTlv cmdData = (ConstructedBerTlv) BerTlv.getInstance(0xe0, cmd.getData());
    final byte[] data = cmdData.getPrimitive(0x80).orElseThrow().getValueField();
    final byte[] mac = cmdData.getPrimitive(0x8e).orElseThrow().getValueField();

    return insSoftwareContext.verifyCryptographicChecksum(data, mac) ? NO_ERROR : VERIFICATON_ERROR;
  } // end method */

  /**
   * Return execution time of the previous command-response pair.
   *
   * @return execution time in seconds of previous command-response pair
   * @see MessageLayer#getTime()
   */
  @Override
  public double getTime() {
    return insTime;
  } // end method */

  /**
   * Set execution time based on start- and end time.
   *
   * @param startTime start time in nanoseconds
   * @param endTime end time in nanoseconds
   */
  /* package */ void setTime(final long startTime, final long endTime) {
    insTime = (endTime - startTime) * 1e-9; // convert from nanosecond to second
  } // end method */

  /**
   * Sends given message.
   *
   * <p>This method is used by the implementation of {@link #send(CommandApdu)}. The purpose of this
   * method is to provide a transparent channel. E.g. if a {@link CommandApdu} is constructed from
   * octet string {@code '00 B0 8102 00 0003'} (i.e READ BINARY with ShortFileIdentifier=1 and
   * offset=2 and Ne=3, ISO-case 2E (extended)), then {@link #send(CommandApdu)} converts that
   * {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e. ISO-case 2S (short)).
   *
   * @param command command APDU
   * @return corresponding response APDU
   * @see MessageLayer#send(byte[])
   */
  @Override
  public byte[] send(final byte[] command) {
    LOGGER.atDebug().log("cmd: '{}'", Hex.toHexDigits(command));

    final long startTime = System.nanoTime();
    final byte[] result = performCommand(command);
    setTime(startTime, System.nanoTime());

    LOGGER.atDebug().log(
        "rsp: {},  '{}'", String.format("time=%7.3f s", getTime()), Hex.toHexDigits(result));

    return result;
  } // end method */

  /**
   * Sends given command APDU.
   *
   * <p>E.g. if a {@link CommandApdu} is constructed from octet string {@code '00 B0 8102 00 0003'}
   * (i.e., READ BINARY with ShortFileIdentifier=1 and offset=2 and Ne=3, ISO-case 2E (extended)),
   * then this method converts the {@link CommandApdu} to {@code '00 B0 8102 03'} (i.e., ISO-case 2S
   * (short)). Thus, if the intention is to send an ISO-case 2E method {@link #send(byte[])} has to
   * be used.
   *
   * @param apdu command APDU to be sent
   * @return corresponding {@link ResponseApdu}
   */
  @Override
  public ResponseApdu send(final CommandApdu apdu) {
    LOGGER.atDebug().log("cmd: {}", apdu.toString());

    final ResponseApdu result = new ResponseApdu(send(apdu.getBytes()));

    LOGGER.atDebug().log("rsp: {}", result.toString());

    return result;
  } // end method */
} // end class
