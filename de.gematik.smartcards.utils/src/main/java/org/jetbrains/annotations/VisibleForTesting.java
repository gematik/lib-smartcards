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
package org.jetbrains.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation indicating, that the visibility is elevated for testing purposes.
 *
 * <p>member or type annotated with {@code VisibleForTesting} claims that its visibility is higher
 * than necessary, only for testing purposes. In particular:
 *
 * <ol>
 *   <li>If public or protected member/type is annotated with VisibleForTesting, it's assumed that
 *       package-private access is enough for production code.
 *   <li>If package-private member/type is annotated with VisibleForTesting, it's assumed that
 *       private access is enough for production code.
 *   <li>It's illegal to annotate private member/type as VisibleForTesting.
 * </ol>
 *
 * <p>This annotation means that the annotated element exposes internal data and breaks
 * encapsulation of the containing class; the annotation won't prevent its use from production code,
 * developers even won't see warnings if their IDE doesn't support the annotation. It's better to
 * provide proper API which can be used in production as well as in tests.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.TYPE})
public @interface VisibleForTesting {}
