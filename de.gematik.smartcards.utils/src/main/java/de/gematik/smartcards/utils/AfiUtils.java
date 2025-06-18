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
package de.gematik.smartcards.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This class contains static methods useful in various situations.
 *
 * <p><i><b>Notes:</b></i>
 *
 * <ol>
 *   <li><i>This is a final class and {@link Object} is the direct superclass.</i>
 *   <li><i>This class has no instance attributes and no usable constructor.</i>
 *   <li><i>Thus, this class is an entity-type.</i>
 *   <li><i>It follows that this class intentionally does not implement the following methods:
 *       {@link Object#clone() clone()}, {@link Object#equals(Object) equals()}, {@link
 *       Object#hashCode() hashCode()}.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public final class AfiUtils {

  /**
   * Binary prefix 2°10 = 1 kibi, see <a
   * href="https://en.wikipedia.org/wiki/Binary_prefix#Adoption_by_IEC,_NIST_and_ISO">Wikipedia</a>.
   */
  public static final int KIBI = 1024; // */

  /**
   * Binary prefix 2^20 = 1 mebi, see <a
   * href="https://en.wikipedia.org/wiki/Binary_prefix#Adoption_by_IEC,_NIST_and_ISO">Wikipedia</a>.
   */
  public static final int MEBI = 1024 * KIBI; // */

  /**
   * Binary prefix 2^30 = 1 gibi, see <a
   * href="https://en.wikipedia.org/wiki/Binary_prefix#Adoption_by_IEC,_NIST_and_ISO">Wikipedia</a>.
   */
  public static final int GIBI = 1024 * MEBI; // */

  /**
   * Binary prefix 2^40 = 1 tebi, see <a
   * href="https://en.wikipedia.org/wiki/Binary_prefix#Adoption_by_IEC,_NIST_and_ISO">Wikipedia</a>.
   */
  public static final long TEBI = 1024L * GIBI; // */

  /**
   * Binary prefix 2^50 = 1 pebi, see <a
   * href="https://en.wikipedia.org/wiki/Binary_prefix#Adoption_by_IEC,_NIST_and_ISO">Wikipedia</a>.
   */
  public static final long PEBI = 1024 * TEBI; // */

  /**
   * Binary prefix 2^60 = 1 exbi, see <a
   * href="https://en.wikipedia.org/wiki/Binary_prefix#Adoption_by_IEC,_NIST_and_ISO">Wikipedia</a>.
   */
  public static final long EXBI = 1024 * PEBI; // */

  /** Empty octet string. */
  public static final byte[] EMPTY_OS = new byte[0]; // */

  /** {@code 1 / ln(2)} = conversion factor used for calculating the logarithm of basis 2. */
  public static final double INVERSE_LN_2 = 1 / Math.log(2); // */

  /** Result of {@link System#lineSeparator()}. */
  public static final String LINE_SEPARATOR = System.lineSeparator(); // */

  /**
   * An unmodifiable {@link Set} with special {@link Integer} values.
   *
   * <p>The set contains values with special bit patterns and close neighbors, in particular:
   *
   * <ul>
   *   <li>0
   *   <li>0x80, 0xff,
   *   <li>0x8000, 0xffff,
   *   <li>0x8000_0000, 0xffff_ffff,
   *   <li>and integers from range {@code [i - 2, i + 2]}
   * </ul>
   */
  public static final Set<Integer> SPECIAL_INT =
      Arrays.stream(
              new int[] {
                0, 0x80, 0xff, 0x8000, 0xffff, 0x800000, 0xffffff, 0x80000000, 0xffffffff,
              })
          .mapToObj(i -> Set.of(i - 2, i - 1, i, i + 1, i + 2))
          .flatMap(Collection::stream)
          .collect(Collectors.toUnmodifiableSet()); // */

  /**
   * An unmodifiable {@link Set} with special {@link Long} values.
   *
   * <p>The set contains values with special bit patterns and close neighbors, in particular:
   *
   * <ul>
   *   <li>0
   *   <li>0x80, 0xff,
   *   <li>0x8000, 0xffff,
   *   <li>0x80_0000, 0xff_ffff,
   *   <li>. . .
   *   <li>0x8000_0000_0000_0000, 0xffff_ffff_ffff_ffff,
   *   <li>and integers from range {@code [i - 2, i + 2]}
   * </ul>
   */
  public static final Set<Long> SPECIAL_LONG =
      Arrays.stream(
              new long[] {
                0,
                0x80L,
                0xffL,
                0x8000L,
                0xffffL,
                0x800000L,
                0xffffffL,
                0x80000000L,
                0xffffffffL,
                0x8000000000L,
                0xffffffffffL,
                0x800000000000L,
                0xffffffffffffL,
                0x80000000000000L,
                0xffffffffffffffL,
                0x8000_0000_0000_0000L,
                0xffffffffffffffffL,
              })
          .mapToObj(i -> Set.of(i - 2, i - 1, i, i + 1, i + 2))
          .flatMap(Collection::stream)
          .collect(Collectors.toUnmodifiableSet()); // */

  /**
   * Exhaustive collection of boolean values (typically used for testing).
   *
   * <p><i><b>Note:</b> Intentionally this is not an array (i.e. {@code boolean[]}), because such an
   * array is not safe for object sharing.</i>
   */
  public static final List<Boolean> VALUES_BOOLEAN = List.of(true, false); // */

  /** Typical message in unexpected exceptions. */
  public static final String UNEXPECTED = "UNEXPECTED"; // */

  /** String constant. */
  private static final String INPUT_NEGATIVE = "input negative"; // */

  /** Private default constructor prevents instantiation of this class. */
  private AfiUtils() {
    super();
    // intentionally empty
  } // end constructor */

  /**
   * Calls {@link Thread#sleep(long)}.
   *
   * <p>This is a convenience method for {@link Thread#sleep(long)}. This method does not through an
   * exception.
   *
   * <p>Causes the currently executing thread to sleep (temporarily cease execution) for the
   * specified number of milliseconds, subject to the precision and accuracy of system timers and
   * schedulers. The thread does not lose ownership of any monitors.
   *
   * <p>If {@code millis} is negative, then {@link Thread#sleep(long)} is not called. Thus, no
   * {@link IllegalArgumentException} is thrown.
   *
   * @param millis the length of time to sleep in milliseconds
   */
  public static void chill(final long millis) {
    if (millis > 0) {
      try {
        Thread.sleep(millis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } // end Catch (...)
    } // end fi
  } // end method */

  /**
   * Concatenates given byte-arrays.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of any
   *       byte-arrays after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param input array of byte-arrays to be concatenated, <b>NEITHER</b> {@code input} <b>NOR</b>
   *     any of its elements <b>SHALL</b> be {@code null}
   * @return concatenation of all arrays
   */
  public static byte[] concatenate(final byte[]... input) {
    // ... assertion 1: input is not null
    // ... assertion 2: no element in input is null

    // --- calculate length of result
    final int concatenatedLength = Arrays.stream(input).mapToInt(i -> i.length).sum();

    // --- allocate result
    final byte[] result = new byte[concatenatedLength];

    // --- copy input to result
    int offset = 0;
    for (final byte[] i : input) {
      System.arraycopy(i, 0, result, offset, i.length);
      offset += i.length;
    } // end For (i...)

    return result;
  } // end method */

  /**
   * Concatenates given byte-arrays.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change the content of any
   *       byte-arrays after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param input list of byte-arrays to be concatenated, <b>NEITHER</b> {@code input} <b>NOR</b>
   *     any of its elements <b>SHALL</b> be {@code null}
   * @return concatenation of all arrays
   */
  public static byte[] concatenate(final Collection<byte[]> input) {
    // ... assertion 1: input is not null
    // ... assertion 2: no element in input is null

    // --- calculate length of result
    final int concatenatedLength = input.stream().mapToInt(i -> i.length).sum();

    // --- allocate result
    final byte[] result = new byte[concatenatedLength];

    // --- copy input to result
    int offset = 0;
    for (final byte[] i : input) {
      System.arraycopy(i, 0, result, offset, i.length);
      offset += i.length;
    } // end For (i...)

    return result;
  } // end method */

  /**
   * Estimates the entropy of the given {@code input} array.
   *
   * <p>The entropy is calculated as follows, see <a
   * href="https://en.wikipedia.org/wiki/Entropy_(information_theory)">Wikipedia</a>:
   *
   * <ol>
   *   <li>The probability <i>p_i</i> of each value is estimated.
   *   <li><i>entropy = - sum(over all values) p_i * log2(p_i)</i>.
   * </ol>
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change any of the entries in
   *       the input array after calling this method.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is primitive.</i>
   * </ol>
   *
   * @param input data for which the entropy is calculated
   * @return entropy per element
   */
  public static double entropy(final int... input) {
    // Note 1: Because (typically) multiplication is faster than division we define a constant here.
    // Note 2: If input.length is zero then numberOfElementsInvers = NaN. This doesn't matter and
    //         this method will return 0.0 in that case.
    final double numberOfElementsInvers = 1.0 / input.length;
    final Map<Integer, AtomicInteger> map = new HashMap<>(); // NOPMD use ConcurrentHashMap
    for (final int data : input) {
      map.computeIfAbsent(data, k -> new AtomicInteger()).incrementAndGet(); // NOPMD new in loop
    } // end For (data...)

    return -map.values().parallelStream()
            .mapToDouble(
                i -> { // calculate entropy per observed value
                  final double probability = i.get() * numberOfElementsInvers;
                  return probability * Math.log(probability);
                })
            .sum()
        * INVERSE_LN_2;
  } // end method */

  /**
   * Returns result of given command
   *
   * <p>In case of exceptions an empty string is returned.
   *
   * @param command to be executed
   * @return name of host
   */
  @VisibleForTesting // otherwise: private
  /* package */ static String exec(final String... command) {
    final var prefix = "";
    try (var br =
        new BufferedReader(
            new InputStreamReader(
                Runtime.getRuntime().exec(command).getInputStream(), StandardCharsets.UTF_8))) {
      // Note: readLine()-method may return null.
      //       Intentionally, here no if-then-else construct is used, because
      //       that would decrease code-coverage during JUnit-tests.
      return prefix + br.readLine(); // NOPMD add empty string
    } catch (final IOException | IllegalArgumentException e) {
      return prefix;
    } // end Catch (...)
  } // end method */

  /**
   * Returns name of host.
   *
   * @return name of host
   */
  public static String hostname() {
    return exec("hostname");
  } // end method */

  /**
   * Treats input-array as a big-endian counter which is incremented by one in situ.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is NOT thread-safe, because it is possible to change entries in the input
   *       array after calling this method by another thread.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is only read and
   *       otherwise not used and the return value is never used again within this class.</i>
   * </ol>
   *
   * @param counter input-array with at least one byte, SHALL NOT be {@code null}
   * @return the same but changed array {@code counter}
   */
  public static byte[] incrementCounter(final byte[] counter) {
    int register = counter.length; // length of counter
    do {
      counter[--register]++; // increment octet at position 'register'
    } while ((0 == counter[register]) // overflow at position 'register' => increment next octet
        && (0 != register) // no more octet to the left to increment => stop iteration
    );
    // ... either no overflow or
    //     complete overflow of counter from 'FF...FF' to '00...00'

    return counter;
  } // end method */

  /**
   * Converts milliseconds to year, day, hour, minute and seconds.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is primitive and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param seconds input to be converted
   * @return {@link String} representing the input in human-readable form
   * @throws IllegalArgumentException if input is negative
   */
  public static String seconds2Time(final long seconds) {
    if (seconds < 0) {
      throw new IllegalArgumentException(INPUT_NEGATIVE);
    } // end fi

    // extract seconds
    final String sec = String.format("%2d\"", seconds % 60);
    long input = seconds / 60; // calculate whole minutes
    if (0 == input) {
      // ... no minutes, just seconds
      return sec;
    } // end fi

    // extract minutes
    final String minutes = String.format("%2d' ", input % 60) + sec;
    input /= 60; // calculate whole hours
    if (0 == input) {
      // ... no hours, just minutes
      return minutes;
    } // end fi

    // estimate hours
    final String hour = String.format("%2dh ", input % 24) + minutes;
    input /= 24; // calculate whole days
    if (0 == input) {
      // ... no days, just hours
      return hour;
    } // end fi

    // estimate days
    final String days = String.format("%3dd ", input % 365) + hour;
    input /= 365; // calculate whole years (do not care about leap year)
    if (0 == input) {
      // ... no years, just days
      return days;
    } // end fi

    // estimate years
    return String.format("%dy ", input) + days;
  } // end method */

  /**
   * Converts milliseconds to year, day, hour, minute and seconds.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is primitive and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param milliSeconds input to be converted
   * @return {@link String} representing the input in human-readable form
   * @throws IllegalArgumentException if input is negative
   */
  public static String milliSeconds2Time(final long milliSeconds) {
    if (milliSeconds < 0) {
      throw new IllegalArgumentException(INPUT_NEGATIVE);
    } // end fi

    // extract milliseconds
    final int millis = (int) (milliSeconds % 1000);
    long input = milliSeconds / 1000; // calculate whole seconds
    if (0 == input) {
      // ... no seconds, just milliseconds
      return millis + " ms";
    } // end fi

    // extract seconds
    final String seconds = String.format("%6.3f\"", (input % 60) + millis / 1000.0);
    input /= 60; // calculate whole minutes
    if (0 == input) {
      // ... no minutes, just seconds
      return seconds;
    } // end fi

    // extract minutes
    final String minutes = String.format("%2d' ", input % 60) + seconds;
    input /= 60; // calculate whole hours
    if (0 == input) {
      // ... no hours, just minutes
      return minutes;
    } // end fi

    // estimate hours
    final String hour = String.format("%2dh ", input % 24) + minutes;
    input /= 24; // calculate whole days
    if (0 == input) {
      // ... no days, just hours
      return hour;
    } // end fi

    // estimate days
    final String days = String.format("%3dd ", input % 365) + hour;
    input /= 365; // calculate whole years (do not care about leap year)
    if (0 == input) {
      // ... no years, just days
      return days;
    } // end fi

    // estimate years
    return String.format("%dy ", input) + days;
  } // end method */

  /**
   * Converts nanoseconds to year, day, hour, minute, seconds.
   *
   * <p><i><b>Notes:</b></i>
   *
   * <ol>
   *   <li><i>This method is thread-safe.</i>
   *   <li><i>Object sharing is not a problem here, because the input parameter is primitive and
   *       return value is immutable.</i>
   * </ol>
   *
   * @param nanoSeconds input to be converted
   * @return {@link String} representing the input in human-readable form
   * @throws IllegalArgumentException if input is negative
   */
  public static String nanoSeconds2Time(final long nanoSeconds) {
    if (nanoSeconds < 0) {
      throw new IllegalArgumentException(INPUT_NEGATIVE);
    } // end fi

    if (nanoSeconds < 1_000) { // NOPMD avoid literals in conditional statements
      // ... less than 1 us
      return nanoSeconds + " ns";
    } else if (nanoSeconds < 1_000_000) { // NOPMD avoid literals in conditional statements
      // ... less than 1 ms
      return String.format("%.3f us", nanoSeconds * 1e-3);
    } else if (nanoSeconds <= 1_000_000_000) { // NOPMD avoid literals in conditional statements
      // ... less than or equal to 1 s
      return String.format("%.6f ms", nanoSeconds * 1e-6);
    } else {
      // ... more than 1 s
      return milliSeconds2Time(Math.round(nanoSeconds * 1e-6));
    } // end fi
  } // end method */

  /**
   * Converts information about a {@link SelectionKey} into a {@link String}.
   *
   * <p>The return value is compiled as follows:
   *
   * <ol>
   *   <li><b>{@code name}:</b> If {@link SelectionKey#attachment()}
   *       <ol>
   *         <li>is {@code null} then {@code name="null"}.
   *         <li>is a {@link List} then {@code name="[]"} if {@link List#isEmpty()}, otherwise the
   *             first element of the list is converted to {@link String}.
   *         <li>otherwise the attachment is converted to {@link String}.
   *       </ol>
   *   <li><b>information:</b> If the {@link SelectionKey#isValid()} is
   *       <ol>
   *         <li>{@code FALSE} (i.e. the key is invalid), then {@code "=i----"} is appended to
   *             {@code name} and this concatenation is returned.
   *         <li>{@code TRUE} (i.e. the key is valid), then {@code "=vACRW"} is appended to {@code
   *             name} and this concatenation is returned:
   *             <ul>
   *               <li>"=v": key is valid
   *               <li>"A" if {@link SelectionKey#isAcceptable()}, otherwise "-".
   *               <li>"C" if {@link SelectionKey#isConnectable()}, otherwise "-".
   *               <li>"R" if {@link SelectionKey#isReadable()}, otherwise "-".
   *               <li>"W" if {@link SelectionKey#isWritable()}, otherwise "-".
   *             </ul>
   *       </ol>
   * </ol>
   *
   * <p>Examples:
   *
   * <ol>
   *   <li>"foo=v--R-": A key named "foo" is readable.
   *   <li>"bar=i----": A key named "bar" is invalid.
   * </ol>
   *
   * @param key to be converted.
   * @param readyOps if {@code TRUE} then information about {@link SelectionKey#readyOps() readyOps}
   *     is shown, otherwise {@link SelectionKey#interestOps() interestOps}.
   * @return {@link String} representation of given {@link SelectionKey}
   */
  @VisibleForTesting // otherwise: private
  /* package */ static String skOps(final SelectionKey key, final boolean readyOps) {
    final Object attachment = key.attachment();
    final String name = estimateName(attachment);

    if (key.isValid()) {
      // ... valid key
      final int ops = readyOps ? key.readyOps() : key.interestOps();

      return name
          + "=v"
          + (0 == (ops & SelectionKey.OP_ACCEPT) ? "-" : "A")
          + (0 == (ops & SelectionKey.OP_CONNECT) ? "-" : "C")
          + (0 == (ops & SelectionKey.OP_READ) ? "-" : "R")
          + (0 == (ops & SelectionKey.OP_WRITE) ? "-" : "W");
    } else {
      // ... invalid key
      return name + "=i----";
    } // end else
  } // end method */

  private static String estimateName(final Object attachment) {
    if (null == attachment) {
      return "null";
    } else if (attachment instanceof final List<?> list) {
      return list.isEmpty() ? "[]" : list.getFirst().toString();
    } else {
      return attachment.toString();
    } // end else
  } // end method */

  /**
   * Converts information about {@code interestedOps} of a {@link SelectionKey} into a {@link
   * String}.
   *
   * <p>The return value is compiled as follows:
   *
   * <ol>
   *   <li><b>{@code name}:</b> If {@link SelectionKey#attachment()}
   *       <ol>
   *         <li>is {@code null} then {@code name="null"}.
   *         <li>is a {@link List} then {@code name="[]"} if {@link List#isEmpty()}, otherwise the
   *             first element of the list is converted to {@link String}.
   *         <li>otherwise the attachment is converted to {@link String}.
   *       </ol>
   *   <li><b>information:</b> If the {@link SelectionKey#isValid()} is
   *       <ol>
   *         <li>{@code FALSE} (i.e. the key is invalid), then {@code "=i----"} is appended to
   *             {@code name} and this concatenation is returned.
   *         <li>{@code TRUE} (i.e. the key is valid), then {@code "=vACRW"} is appended to {@code
   *             name} and this concatenation is returned:
   *             <ul>
   *               <li>"=v": key is valid
   *               <li>"A" if {@link SelectionKey#isAcceptable()}, otherwise "-".
   *               <li>"C" if {@link SelectionKey#isConnectable()}, otherwise "-".
   *               <li>"R" if {@link SelectionKey#isReadable()}, otherwise "-".
   *               <li>"W" if {@link SelectionKey#isWritable()}, otherwise "-".
   *             </ul>
   *       </ol>
   * </ol>
   *
   * <p>Examples:
   *
   * <ol>
   *   <li>"foo=v--R-": A key named "foo" is interested in "readable".
   *   <li>"bar=i----": A key named "bar" is invalid.
   * </ol>
   *
   * @param key whose {@link SelectionKey#interestOps() interestedOps} are converted into a {@link
   *     String}
   * @return {@link String} representation of "interestedOps" for given {@link SelectionKey}
   */
  public static String skInterestedOps(final SelectionKey key) {
    return skOps(key, false);
  } // end method */

  /**
   * Converts elements in this {@code selectionKeys} via {@link #skInterestedOps(SelectionKey)} into
   * a sorted list of {@link String Strings}.
   *
   * @param selectionKeys set of {@link SelectionKey}
   * @return sorted {@link List} of {@link String Strings}
   */
  public static List<String> skInterestedOps(final Collection<SelectionKey> selectionKeys) {
    return selectionKeys.stream().map(AfiUtils::skInterestedOps).sorted().toList();
  } // end method */

  /**
   * Converts information about {@code readyOps} of a {@link SelectionKey} into a {@link String}.
   *
   * <p>The return value is compiled as follows:
   *
   * <ol>
   *   <li><b>{@code name}:</b> If {@link SelectionKey#attachment()}
   *       <ol>
   *         <li>is {@code null} then {@code name="null"}.
   *         <li>is a {@link List} then {@code name="[]"} if {@link List#isEmpty()}, otherwise the
   *             first element of the list is converted to {@link String}.
   *         <li>otherwise the attachment is converted to {@link String}.
   *       </ol>
   *   <li><b>information:</b> If the {@link SelectionKey#isValid()} is
   *       <ol>
   *         <li>{@code FALSE} (i.e. the key is invalid), then {@code "=i----"} is appended to
   *             {@code name} and this concatenation is returned.
   *         <li>{@code TRUE} (i.e. the key is valid), then {@code "=vACRW"} is appended to {@code
   *             name} and this concatenation is returned:
   *             <ul>
   *               <li>"=v": key is valid
   *               <li>"A" if {@link SelectionKey#isAcceptable()}, otherwise "-".
   *               <li>"C" if {@link SelectionKey#isConnectable()}, otherwise "-".
   *               <li>"R" if {@link SelectionKey#isReadable()}, otherwise "-".
   *               <li>"W" if {@link SelectionKey#isWritable()}, otherwise "-".
   *             </ul>
   *       </ol>
   * </ol>
   *
   * <p>Examples:
   *
   * <ol>
   *   <li>"foo=v--R-": A key named "foo" is ready for "read".
   *   <li>"bar=i----": A key named "bar" is invalid.
   * </ol>
   *
   * @param key whose {@link SelectionKey#readyOps() readyOps} are converted into a {@link String}
   * @return {@link String} representation of "readyOps" for given {@link SelectionKey}
   */
  public static String skReadyOps(final SelectionKey key) {
    return skOps(key, true);
  } // end method */

  /**
   * Converts elements in {@code selectionKeys} via {@link #skReadyOps(SelectionKey)} into a sorted
   * list of {@link String Strings}.
   *
   * @param selectionKeys set of {@link SelectionKey}
   * @return sorted {@link List} of {@link String Strings}
   */
  public static List<String> skReadyOps(final Collection<SelectionKey> selectionKeys) {
    return selectionKeys.stream().map(AfiUtils::skReadyOps).sorted().toList();
  } // end method */
} // end class
