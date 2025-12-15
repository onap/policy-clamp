/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.common.utils.coder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.Serial;
import java.io.Serializable;

/**
 * Object type used by the {@link StandardCoder}. Different serialization tools have
 * different "standard objects". For instance, Jackson uses {@link JsonNode}. This class
 * wraps that object so that it can be used without exposing the object, itself.
 */
public class StandardCoderObject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    private final transient JsonNode data;

    public StandardCoderObject() {
        this.data = null;
    }

    @JsonCreator
    public StandardCoderObject(JsonNode data) {
        this.data = data;
    }

    @JsonValue
    public JsonNode value() {
        return data;
    }

    /**
     * Gets a field's value from this object, traversing the object hierarchy.
     *
     * @param fields field hierarchy. These may be strings, identifying fields within the
     *               object, or Integers, identifying an index within an array
     * @return the field value or {@code null} if the field does not exist or is not a primitive
     */
    public String getString(Object... fields) {
        JsonNode node = this.data;
        for (Object field : fields) {
            if (node == null) {
                return null;
            }
            if (field instanceof String) {
                node = getFieldFromObject(node, (String) field);
            } else if (field instanceof Integer) {
                node = getItemFromArray(node, (Integer) field);
            } else {
                throw new IllegalArgumentException("subscript is not a string or integer: " + field);
            }
        }
        return (node != null && (node.isValueNode() || node.isTextual())) ? node.asText() : null;
    }

    /**
     * Gets an item from an object.
     *
     * @param element object from which to extract the item
     * @param field   name of the field from which to extract the item
     * @return the item, or {@code null} if the element is not an object or if the field does not exist
     */
    protected JsonNode getFieldFromObject(JsonNode element, String field) {
        return element.isObject() ? element.get(field) : null;
    }

    /**
     * Gets an item from an array.
     *
     * @param element array from which to extract the item
     * @param index   index of the item to extract
     * @return the item, or {@code null} if the element is not an array or if the index is out of bounds
     */
    protected JsonNode getItemFromArray(JsonNode element, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("subscript is invalid: " + index);
        }
        if (!element.isArray()) {
            return null;
        }
        var array = (ArrayNode) element;
        return index >= array.size() ? null : array.get(index);
    }

    protected JsonNode getData() {
        return this.data;
    }
}
