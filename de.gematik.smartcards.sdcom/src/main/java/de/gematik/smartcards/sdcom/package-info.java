/*
 *  Copyright 2024 gematik GmbH
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

/**
 * This package provides functions for communication with the ICC.
 *
 * <p>This package provides rather general interfaces for communicating with an ICC. Although the
 * abbreviation "ICC" is used, the interfaces from this package can also be used for other types of
 * security devices.
 *
 * <p>Basically, this package defines:
 *
 * <ol>
 *   <li>{@link de.gematik.smartcards.sdcom.Message} as a generalization for all messages which are
 *       exchanged to and from an ICC.
 *   <li>{@link de.gematik.smartcards.sdcom.apdu.Command} as a generalization for all messages
 *       containing a command to be performed.<br>
 *       <i><b>Note:</b> Although, typically a command message is always sent to an ISO/IEC 7816
 *       compliant ICC a command message can also be sent by other types of ICC.</i>
 *   <li>{@link de.gematik.smartcards.sdcom.apdu.Response} as a generalization for all messages sent
 *       in response to a {@link de.gematik.smartcards.sdcom.apdu.Command}.<br>
 *       <i><b>Note:</b> Typically, for each {@link de.gematik.smartcards.sdcom.apdu.Command} there
 *       is exactly one {@link de.gematik.smartcards.sdcom.apdu.Response}.</i>
 * </ol>
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 */
@DefaultAnnotation(NonNull.class) // help spotbugs
@ReturnValuesAreNonnullByDefault // help spotbugs
package de.gematik.smartcards.sdcom;

import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.ReturnValuesAreNonnullByDefault;
