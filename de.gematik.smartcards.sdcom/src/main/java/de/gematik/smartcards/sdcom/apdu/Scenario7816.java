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

import static de.gematik.smartcards.utils.AfiUtils.LINE_SEPARATOR;

import de.gematik.smartcards.tlv.ConstructedBerTlv;
import de.gematik.smartcards.tlv.DerInteger;
import de.gematik.smartcards.tlv.DerOctetString;
import de.gematik.smartcards.tlv.DerSequence;
import de.gematik.smartcards.tlv.DerSet;
import de.gematik.smartcards.tlv.DerUtf8String;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.event.Level;

/**
 * Class for scenario handling.
 *
 * <p>Information: A scenario is defined here as a sequence of {@link CommandApdu} which are
 * intended to be sent to an ICC. Furthermore, the scenario may also contain sets of expected status
 * words and messages for a logging layer.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
public final class Scenario7816 {

  /**
   * Version number.
   *
   * <p><b>Version 0:</b><br>
   *
   * <ol>
   *   <li>{@link Scenario7816#Scenario7816(DerSequence)} expects and {@link #toTlv()} produces the
   *       following ASN.1 structure:
   *       <pre>
   *           DerSequence         # Sequence for Scenario7816
   *             DerInteger        #   version 0
   *             DerInteger        #   duration till the next scenario is expected
   *             DerSequence       #   list of objects with the following types
   *               DerSequence     #     with integers for "expected status word"
   *               DerOctetString  #     command APDU
   *               DerSet          #     logging information
   *                 DerInteger    #       log-level
   *                 DerUtf8String #       log-message
   *       </pre>
   * </ol>
   */
  public static final int VERSION = 0; // */

  /**
   * Cash the hash code.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>Intentionally the visibility of this instance attribute is "protected". Thus,
   *       subclasses are able to get and set it.</i>
   *   <li><i>Because only immutable instance attributes of this class and all subclasses are taken
   *       into account for this instance attribute lazy initialization is possible.</i>
   *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
   *       initialization) nor synchronized (to avoid synchronization overhead).</i>
   * </ol>
   */
  private volatile int insHashCode; // NOPMD volatile not recommended */

  /**
   * List of elements, relevant for a scenario.
   *
   * <p>The following types of objects are relevant for a scenario:
   *
   * <ol>
   *   <li>{@link List} with expected status words as {@link Integer}
   *   <li>{@link CommandApdu} to be sent to an ICC
   *   <li>{@link LoggingInformation} with log messages
   * </ol>
   */
  private final List<Object> insScenario; // */

  /** Version number. */
  private final int insVersion; // */

  /**
   * Comfort constructor.
   *
   * <p>Unexpected object types in {@code list} are ignored.
   *
   * @param list of objects, only the following types of objects are taken into account:
   *     <ol>
   *       <li><b>{@link Collection}</b> with integers for expected status words
   *       <li><b>{@link CommandApdu}</b>
   *       <li><b>{@link LoggingInformation}</b>
   *     </ol>
   */
  public Scenario7816(final List<Object> list) {
    insVersion = 0;

    final var scenario = new ArrayList<>();
    list.forEach(
        i -> {
          if (i instanceof final CommandApdu cmd) {
            // ... assuming command APDU
            scenario.add(cmd);
          } else if (i instanceof final Collection<?> esw) {
            // ... assuming expected status words
            scenario.add(esw.stream().filter(Integer.class::isInstance).toList());
          } else if (i instanceof final LoggingInformation loggingInformation) {
            // ... assuming logging information
            scenario.add(loggingInformation);
          } // end else if
        }); // end forEach(i -> ...)

    insScenario = Collections.unmodifiableList(scenario);
  } // end constructor */

  /**
   * Comfort constructor used for deserialization.
   *
   * <p>Currently, this constructor supports the following versions (see {@link #VERSION}):
   *
   * <ol>
   *   <li>version = 0
   * </ol>
   *
   * <p>Data objects with unexpected tags are ignored.
   *
   * <p><i><b>Note:</b> This is kind of the inverse operation to {@link Scenario7816#toTlv()}.</i>
   *
   * @param tlv serialized representation, see {@link #toTlv()}
   * @throws ArithmeticException if
   *     <ol>
   *       <li>given version number cannot be converted to an {@link Integer}, or
   *       <li>given duration cannot be converted to an {@link Long}
   *     </ol>
   */
  public Scenario7816(final DerSequence tlv) {
    insVersion = tlv.getInteger().getDecoded().intValueExact();

    final var scenario = new ArrayList<>();
    tlv.getSequence()
        .getTemplate()
        .forEach(
            i -> {
              if (i instanceof final DerOctetString cmd) {
                // ... assuming command APDU
                scenario.add(new CommandApdu(cmd.getDecoded()));
              } else if (i instanceof final DerSequence sequence) {
                // ... assuming expected status words
                scenario.add(
                    sequence.getTemplate().stream()
                        .filter(DerInteger.class::isInstance)
                        .mapToInt(j -> ((DerInteger) j).getDecoded().intValue())
                        .boxed()
                        .toList());
              } else if (i instanceof final DerSet loggingInformation) {
                // ... assuming logging information
                scenario.add(new LoggingInformation(loggingInformation));
              } // end else if
            }); // end forEach(i -> ...)

    insScenario = Collections.unmodifiableList(scenario);
  } // end method */

  /**
   * The implementation of this method fulfills the equals-contract.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because input parameter(s) of correct class are
   *       immutable and return value is primitive.</i>
   * </ol>
   *
   * @param obj object used for comparison, can be null
   * @return true if objects are equal, false otherwise
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(final Object obj) {
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

    // --- check class
    if (getClass() != obj.getClass()) {
      // ... different classes
      return false;
    } // end fi
    // ... obj is instance of LoggingInformation

    final Scenario7816 other = (Scenario7816) obj;

    // --- compare instance attributes
    // ... assertion: instance attributes are never null
    return (this.insVersion == other.insVersion) && (this.insScenario.equals(other.insScenario));
  } // end method */

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
  public int hashCode() {
    // Note 1: Because this class is a direct subclass of object
    //         calling super.hashCode(...) would be wrong.
    // Note 2: Because equals() takes into account all instance attributes,
    //         here we can do the same.
    // Note 3: Instead of simple adding the hash-codes of instance attributes
    //         here, some calculation is done.
    // Note 4: Because only immutable instance attributes are taken into account
    //         (for fulfilling the hashCode-contract) it is possible to use
    //         Racy-Single-Check-Idiom hereafter which gives good performance.

    int result = insHashCode; // read attribute from the main memory into thread local memory
    if (0 == result) {
      // ... probably attribute has never been calculated (lazy initialization)
      //     => do so now
      // ... assertion 1: CLA, INS, P1 and P2 are one byte octet
      // ... assertion 2: instance attributes are never null

      final long hashCodeMultiplier = 31L; // small prime number

      // --- take into account primitive instance attributes
      // take into account "version"
      result = insVersion;

      // --- take into account reference types (currently only insScenario)
      result = (int) (result * hashCodeMultiplier + insScenario.hashCode());

      insHashCode = result; // store attribute into thread local memory
    } // end fi

    return result;
  } // end method */

  /**
   * Sends the {@link CommandApdu} in the scenario to the given {@link ApduLayer}.
   *
   * @param apduLayer to which {@link CommandApdu} are sent
   * @param logger used for logging messages
   * @return list of {@link ResponseApdu} corresponding to {@link CommandApdu} from this {@link
   *     Scenario7816}
   */
  public List<ResponseApdu> run(final ApduLayer apduLayer, final Logger logger) {
    final var result = new ArrayList<ResponseApdu>();
    List<Integer> expectedStatusWords = Collections.emptyList(); // default

    for (final var i : getScenario()) {
      if (i instanceof final CommandApdu cmd) {
        // ... command APDU
        final ResponseApdu rsp = apduLayer.send(cmd);
        result.add(rsp);

        if (!expectedStatusWords.contains(rsp.getSw())) {
          // ... unexpected status-word received
          //     => do not issue further command APDU
          logger
              .atWarn()
              .log(
                  "got '{}', expected [{}]",
                  String.format("%04x", rsp.getSw()),
                  expectedStatusWords.stream()
                      .map(j -> String.format("'%04x'", j))
                      .collect(Collectors.joining(", ")));

          break;
        } // end fi
      } else if (i instanceof final List<?> esw) {
        // ... expected status words
        expectedStatusWords =
            esw.stream().filter(Integer.class::isInstance).map(j -> (Integer) j).toList();
      } else {
        // ... logging information
        final var li = (LoggingInformation) i;
        logger.atLevel(li.getLevel()).log(li.getMessage());
      } // end else if
    } // end For (i...)

    return Collections.unmodifiableList(result);
  } // end method */

  /**
   * Converts this scenario to a {@link String}.
   *
   * @return {@link String} representation
   * @see Object#toString()
   */
  @Override
  public String toString() {
    final List<String> result = new ArrayList<>();

    // --- add version information
    result.add("version = " + getVersion());

    // --- add information from the list
    result.addAll(
        getScenario().stream()
            .map(
                i -> {
                  if (i instanceof final CommandApdu cmd) {
                    return cmd.toString();
                  } else if (i instanceof final LoggingInformation li) {
                    return li.toString();
                  } else {
                    // ... assuming a collection with expected status words
                    return ((Collection<?>) i)
                        .stream()
                            .filter(Integer.class::isInstance)
                            .map(j -> String.format("%04x", j))
                            .collect(
                                Collectors.joining(
                                    ", ", // delimiter
                                    "esw = {", // prefix
                                    "}" // suffix
                                    ));
                  } // end else
                })
            .toList());

    return String.join(LINE_SEPARATOR, result);
  } // end method */

  /**
   * Converts this scenario to a DER-TLV-object.
   *
   * <p>The returned DER-TLV-object has a structure defined by {@link #VERSION}.
   *
   * <p><i><b>Note:</b> This is kind of the inverse operation to {@link
   * Scenario7816#Scenario7816(DerSequence)}.</i>
   *
   * @return scenario as DER-TLV-object
   */
  public DerSequence toTlv() {
    return new DerSequence(
        List.of(
            new DerInteger(BigInteger.valueOf(getVersion())),
            new DerSequence(
                getScenario().stream()
                    .map(
                        i -> {
                          if (i instanceof final CommandApdu cmd) {
                            return new DerOctetString(cmd.getBytes());
                          } else if (i instanceof final LoggingInformation li) {
                            return li.toTlv();
                          } else {
                            // ... assuming a collection with expected status words
                            return new DerSequence(
                                ((Collection<?>) i)
                                    .stream()
                                        .filter(Integer.class::isInstance)
                                        .map(j -> new DerInteger(BigInteger.valueOf((Integer) j)))
                                        .toList());
                          } // end else
                        })
                    .toList())));
  } // end method */

  /**
   * Getter.
   *
   * @return list of objects defining the scenario
   */
  public List<Object> getScenario() {
    return insScenario;
  } // end method */

  /**
   * Getter.
   *
   * @return version number
   */
  public int getVersion() {
    return insVersion;
  } // end method */

  // ###########################################################################
  // ###########################################################################
  // ###########################################################################

  /**
   * Logging information in a scenario.
   *
   * @author <a href="mailto:software-development@gematik.de">gematik</a>
   */
  public static class LoggingInformation {

    /** Default TLV-object for logging level. */
    @VisibleForTesting // otherwise = private
    /* package */ static final DerInteger DEFAULT_LEVEL_DO =
        new DerInteger(BigInteger.valueOf(Level.TRACE.toInt())); // */

    /** Default TLV-object for logging message. */
    @VisibleForTesting // otherwise = private
    /* package */ static final DerUtf8String DEFAULT_MESSAGE = new DerUtf8String(""); // */

    /**
     * Cash the hash code.
     *
     * <p><i><b>Notes:</b></i>
     *
     * <ol>
     *   <li><i>Intentionally the visibility of this instance attribute is "protected". Thus,
     *       subclasses are able to get and set it.</i>
     *   <li><i>Because only immutable instance attributes of this class and all subclasses are
     *       taken into account for this instance attribute lazy initialization is possible.</i>
     *   <li><i>Intentionally, this instance attribute is neither final (because of lazy
     *       initialization) nor synchronized (to avoid synchronization overhead).</i>
     * </ol>
     */
    private volatile int insHashCode; // NOPMD volatile not recommended */

    /** Logging level. */
    private final Level insLevel;

    /** Logging message. */
    private final String insMessage;

    /**
     * Comfort constructor.
     *
     * @param level of logging message
     * @param message to be logged
     */
    public LoggingInformation(final Level level, final String message) {
      insLevel = level;
      insMessage = message;
    } // end constructor */

    /**
     * Comfort constructor used for deserialization.
     *
     * <p>Within the template of the given {@link ConstructedBerTlv} object a {@link DerInteger} is
     * used as logging level, if absent then {@link Level#TRACE} is assumed.
     *
     * <p>Within the template of the given {@link ConstructedBerTlv} object a {@link DerUtf8String}
     * is used as a logging message, if absent then an empty {@link String} is used.
     *
     * <p><i><b>Note:</b> This is kind of the inverse operation to {@link
     * LoggingInformation#toTlv()}.</i>
     *
     * @param set serialized representation
     */
    public LoggingInformation(final DerSet set) {
      insLevel =
          Level.intToLevel(
              ((DerInteger) set.getPrimitive(DerInteger.TAG).orElse(DEFAULT_LEVEL_DO))
                  .getDecoded()
                  .intValue());
      insMessage =
          ((DerUtf8String) set.getPrimitive(DerUtf8String.TAG).orElse(DEFAULT_MESSAGE))
              .getDecoded();
    } // end constructor */

    /**
     * The implementation of this method fulfills the equals-contract.
     *
     * <p><i><b>Notes:</b></i>
     *
     * <ol>
     *   <li><i>This method is thread-safe.</i>
     *   <li><i>Object sharing is not a problem here, because input parameter(s) of correct class
     *       are immutable and return value is primitive.</i>
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

      // --- check class
      if (getClass() != obj.getClass()) {
        // ... different classes
        return false;
      } // end fi
      // ... obj is instance of LoggingInformation

      final LoggingInformation other = (LoggingInformation) obj;

      // --- compare instance attributes
      // ... assertion: instance attributes are never null
      return (insLevel == other.insLevel) && insMessage.equals(other.insMessage);
    } // end method */

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
    public int hashCode() {
      // Note 1: Because this class is a direct subclass of object
      //         calling super.hashCode(...) would be wrong.
      // Note 2: Because equals() takes into account "level" and "message",
      //         here we can do the same.
      // Note 3: Instead of simple adding the hash-codes of instance attributes
      //         here, some calculation is done.
      // Note 4: Because only immutable instance attributes are taken into account
      //         (for fulfilling the hashCode-contract) it is possible to use
      //         Racy-Single-Check-Idiom hereafter which gives good performance.

      int result = insHashCode; // read attribute from the main memory into thread local memory
      if (0 == result) {
        // ... probably attribute has never been calculated (lazy initialization)
        //     => do so now
        // ... assertion 1: CLA, INS, P1 and P2 are one byte octet
        // ... assertion 2: instance attributes are never null

        final int hashCodeMultiplier = 31; // small prime number

        // --- take into account primitive instance attributes
        // take into account command header
        result = getLevel().toInt();

        // --- take into account reference types (currently only insData)
        result = result * hashCodeMultiplier + getMessage().hashCode();

        insHashCode = result; // store attribute into thread local memory
      } // end fi

      return result;
    } // end method */

    /**
     * Converts instance to a {@link String}.
     *
     * @return {@link String} representation
     * @see Object#toString()
     */
    @Override
    public String toString() {
      return getLevel().name() + ", \"" + getMessage() + '"';
    } // end method */

    /**
     * Converts logging information to DER-TLV-object.
     *
     * <p><i><b>Note:</b> This is kind of the inverse operation to {@link
     * Scenario7816.LoggingInformation#LoggingInformation(DerSet)}.</i>
     *
     * <p>The returned DER-TLV-object has the following structure:
     *
     * <pre>
     *   DerSet          # logging information
     *     DerInteger    #   log-level
     *     DerUtf8String #   log-message
     * </pre>
     *
     * @return logging information as DER-TLV-object
     */
    public DerSet toTlv() {
      return new DerSet(
          List.of(
              new DerInteger(BigInteger.valueOf(getLevel().toInt())),
              new DerUtf8String(getMessage())));
    } // end method */

    /**
     * Getter.
     *
     * @return logging level
     */
    public Level getLevel() {
      return insLevel;
    } // end method */

    /**
     * Getter.
     *
     * @return logging message
     */
    public String getMessage() {
      return insMessage;
    } // end method */
  } // end inner class
} // end class
