/*-
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2020-2021 AT&T Intellectual Property. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.parameters;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.onap.policy.common.parameters.annotations.NotNull;

/**
 * Validator of a value.
 * <p/>
 * Note: this currently does not support Min/Max validation of "short" or "byte"; these
 * annotations are simply ignored for these types.
 */
@NoArgsConstructor
public class ValueValidator {

    /**
     * {@code True} if the value is allowed to be {@code null}, {@code false} otherwise.
     * Subclasses are expected to set this, typically based on the validation annotations
     * associated with the value.
     */
    @Getter
    @Setter(AccessLevel.PROTECTED)
    private boolean nullAllowed = true;

    /**
     * Predicates to invoke to validate an object.
     * <p/>
     * Note: each predicate is expected to return {@code true} if the next check is
     * allowed to proceed, {@code false} otherwise. In addition, if {@link #nullAllowed}
     * is {@code true}, then the predicates must be prepared to deal with a {@code null}
     * Object as their input parameter.
     */
    protected List<Checker> checkers = new ArrayList<>(10);

    /**
     * Determines if the validator has anything to check.
     *
     * @return {@code true} if the validator is empty (i.e., has nothing to check)
     */
    public boolean isEmpty() {
        return checkers.isEmpty();
    }

    /**
     * Performs validation of a single field.
     *
     * @param result validation results are added here
     * @param fieldName field whose value is being verified
     * @param value value to be validated
     */
    protected void validateValue(BeanValidationResult result, String fieldName, Object value) {

        if (value == null && isNullAllowed()) {
            // value is null and null is allowed - just return
            return;
        }

        for (Checker checker : checkers) {
            if (!checker.test(result, fieldName, value)) {
                // invalid - don't bother with additional checks
                return;
            }
        }
    }

    /**
     * Looks for an annotation at the class or field level. If an annotation is found at
     * either the field or class level, then it adds a verifier to
     * {@link ValueValidator#checkers}.
     *
     * @param annotClass class of annotation to find
     * @param checker function to validate the value
     */
    public <T extends Annotation> void addAnnotation(Class<T> annotClass, Checker checker) {
        var annot = getAnnotation(annotClass);
        if (annot != null) {
            checkers.add(checker);

            if (annotClass == NotNull.class) {
                setNullAllowed(false);
            }
        }
    }

    /**
     * Looks for an annotation at the class or field level. If an annotation is found at
     * either the field or class level, then it adds a verifier to
     * {@link ValueValidator#checkers}.
     *
     * @param annotClass class of annotation to find
     * @param checker function to validate the value
     */
    public <T extends Annotation> void addAnnotation(Class<T> annotClass, CheckerWithAnnot<T> checker) {
        var annot = getAnnotation(annotClass);
        if (annot != null) {
            checkers.add((result, fieldName, value) -> checker.test(result, fieldName, annot, value));
        }
    }

    /**
     * Gets an annotation from the field or the class. The default method simply returns
     * {@code null}.
     *
     * @param annotClass annotation class of interest
     * @return the annotation, or {@code null} if neither the field nor the class has the
     *         desired annotation
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotClass) {
        return null;
    }

    // functions to validate a value extracted from a field

    public static interface Checker {
        boolean test(BeanValidationResult result, String fieldName, Object value);
    }

    public static interface CheckerWithAnnot<T extends Annotation> {
        boolean test(BeanValidationResult result, String fieldName, T annotation, Object value);
    }
}
