/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.common.parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * This class holds the result of the validation of an arbitrary bean.
 */
public class BeanValidationResult extends ValidationResultImpl {

    /**
     * Validation results for each item in the bean.
     */
    private final List<ValidationResult> itemResults = new ArrayList<>();


    /**
     * Constructs the object.
     *
     * @param name name of the bean being validated
     * @param object object being validated
     */
    public BeanValidationResult(String name, Object object) {
        super(name, object);
    }

    /**
     * Adds a result to this result.
     *
     * @param result the result to be added
     * @return {@code true} if the result is {@code null} or valid, {@code false} if the
     *         result is invalid
     */
    public boolean addResult(ValidationResult result) {
        if (result == null) {
            return true;
        }

        itemResults.add(result);
        setResult(result.getStatus());

        return result.isValid();
    }

    /**
     * Adds a result to this result.
     * @param name name of the object of this result
     * @param object object being validated
     * @param status status of the new result
     * @param message new result message
     * @return {@code true} if the status is {@code null} or valid, {@code false} if the
     *         status is invalid
     */
    public boolean addResult(String name, Object object, ValidationStatus status, String message) {
        return addResult(new ObjectValidationResult(name, object, status, message));
    }

    /**
     * Validates that a sub-object within the bean is not {@code null}.
     *
     * @param subName name of the sub-object
     * @param subObject the sub-object
     * @return {@code true} if the value is not null, {@code false} otherwise
     */
    public boolean validateNotNull(String subName, Object subObject) {
        var result = new ObjectValidationResult(subName, subObject);

        if (result.validateNotNull()) {
            return true;

        } else {
            addResult(result);
            return false;
        }
    }

    /**
     * Validates the items in a list, after validating that the list, itself, is not null.
     *
     * @param listName name of the list
     * @param list list whose items are to be validated, or {@code null}
     * @param itemValidator function to validate an item in the list
     * @return {@code true} if all items in the list are valid, {@code false} otherwise
     */
    public <T> boolean validateNotNullList(String listName, Collection<T> list,
                    Function<T, ValidationResult> itemValidator) {

        return validateNotNull(listName, list) && validateList(listName, list, itemValidator);
    }

    /**
     * Validates the items in a list.
     *
     * @param listName name of the list
     * @param list list whose items are to be validated, or {@code null}
     * @param itemValidator function to validate an item in the list
     * @return {@code true} if all items in the list are valid, {@code false} otherwise
     */
    public <T> boolean validateList(String listName, Collection<T> list, Function<T, ValidationResult> itemValidator) {
        if (list == null) {
            return true;
        }

        var result = new BeanValidationResult(listName, null);
        for (T item : list) {
            if (item == null) {
                result.addResult("item", item, ValidationStatus.INVALID, "null");
            } else {
                result.addResult(itemValidator.apply(item));
            }
        }

        if (result.isValid()) {
            return true;

        } else {
            addResult(result);
            return false;
        }
    }

    /**
     * Validates the entries in a map.
     *
     * @param mapName name of the list
     * @param map map whose entries are to be validated, or {@code null}
     * @param entryValidator function to validate an entry in the map
     * @return {@code true} if all entries in the map are valid, {@code false} otherwise
     */
    public <V> boolean validateMap(String mapName, Map<String, V> map,
                    BiConsumer<BeanValidationResult, Entry<String, V>> entryValidator) {
        if (map == null) {
            return true;
        }

        var result = new BeanValidationResult(mapName, null);
        for (Entry<String, V> ent : map.entrySet()) {
            entryValidator.accept(result, ent);
        }

        if (result.isValid()) {
            return true;

        } else {
            addResult(result);
            return false;
        }
    }

    /**
     * Gets the validation result.
     *
     * @param initialIndentation the indentation to use on the main result output
     * @param subIndentation the indentation to use on sub parts of the result output
     * @param showClean output information on clean fields
     * @return the result
     */
    @Override
    public String getResult(final String initialIndentation, final String subIndentation, final boolean showClean) {
        if (!showClean && getStatus() == ValidationStatus.CLEAN) {
            return null;
        }

        var builder = new StringBuilder();

        builder.append(initialIndentation);
        builder.append('"');
        builder.append(getName());

        builder.append("\" ");
        builder.append(getStatus());
        builder.append(", ");
        builder.append(getMessage());
        builder.append('\n');

        for (ValidationResult itemResult : itemResults) {
            String message = itemResult.getResult(initialIndentation + subIndentation, subIndentation, showClean);
            if (message != null) {
                builder.append(message);
            }
        }

        return builder.toString();
    }
}
