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

/**
 * This interface indicates that {@code clone()}-method is available.
 *
 * <p>In contrast to the standard interface {@link Cloneable} this interface has a {@code clone()}
 * method. It is supposed to be used in lieu of the standard {@link java.lang.Cloneable} interface.
 *
 * @author <a href="mailto:software-development@gematik.de">gematik</a>
 * @see java.lang.Cloneable
 */
public interface CloneableWithCloneMethod extends Cloneable {

  /**
   * Creates and returns a copy of this object.
   *
   * <p>The precise meaning of "copy" may depend on the class of the object. The general intent is
   * that, for any object x, the following expression will be true:
   *
   * <ol>
   *   <li>{@code x.clone() != x} will be true, and
   *   <li>{@code x.clone().getClass() == x.getClass()} will be true, and
   *   <li>{@code x.clone().equals(x)}
   * </ol>
   *
   * <p>By convention, the returned object should be obtained by calling {@code super.clone}. If a
   * class and all of its superclasses (except Object) obey this convention, it will be the case
   * that {@code x.clone().getClass() == x.getClass()}.
   *
   * <p>By convention, the object returned by this method should be independent of this object
   * (which is being cloned). To achieve this independence, it may be necessary to modify one or
   * more fields of the object returned by {@code super.clone} before returning it. Typically, this
   * means copying any mutable objects that comprise the internal "deep structure" of the object
   * being cloned and replacing the references to these objects with references to the copies. If a
   * class contains only primitive fields or references to immutable objects, then it is usually the
   * case that no fields in the object returned by {@code super.clone} need to be modified.
   *
   * @return a clone of this instance.
   * @throws OutOfMemoryError in case of not enough memory.
   * @throws InternalError in case of an unexpected {@link CloneNotSupportedException}.
   */
  CloneableWithCloneMethod clone(); // NOPMD should throw CloneNotSupported Exception */

  // Note 1: SonarQube claims the following blocker code smell on this method:
  //         "Remove this "clone" implementation; use a copy constructor or copy
  //         factory instead."
  // Note 2: This interface and its method are intentional as they are. I agree
  //         That doing "clone()" is tricky, but usefull when you know how to do
  //         it.
} // end interface
