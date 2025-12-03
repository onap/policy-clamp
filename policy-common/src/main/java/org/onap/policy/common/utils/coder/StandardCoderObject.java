/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

package org.onap.policy.common.utils.coder;

import com.google.gson.JsonElement;
import java.io.Serial;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Object type used by the {@link StandardCoder}. Different serialization tools have
 * different "standard objects". For instance, GSON uses {@link JsonElement}. This class
 * wraps that object so that it can be used without exposing the object, itself.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class StandardCoderObject implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Data wrapped by this.
     */
    /*
     * this should not be transient, but since it isn't serializable, we're stuck with it
     * until there's time to address the issue
     */
    @Getter(AccessLevel.PROTECTED)
    private final transient JsonElement data;

    /**
     * Constructs the object.
     */
    public StandardCoderObject() {
        data = null;
    }

    /**
     * Gets a field's value from this object, traversing the object hierarchy.
     *
     * @param fields field hierarchy. These may be strings, identifying fields within the
     *               object, or Integers, identifying an index within an array
     * @return the field value or {@code null} if the field does not exist or is not a primitive
     */
    public String getString(Object... fields) {

        JsonElement jel = data;

        for (Object field : fields) {
            if (jel == null) {
                return null;
            }

            if (field instanceof String) {
                jel = getFieldFromObject(jel, field.toString());

            } else if (field instanceof Integer) {
                jel = getItemFromArray(jel, (int) field);

            } else {
                throw new IllegalArgumentException("subscript is not a string or integer: " + field);
            }
        }

        return (jel != null && jel.isJsonPrimitive() ? jel.getAsString() : null);
    }

    /**
     * Gets an item from an object.
     *
     * @param element object from which to extract the item
     * @param field   name of the field from which to extract the item
     * @return the item, or {@code null} if the element is not an object or if the field does not exist
     */
    protected JsonElement getFieldFromObject(JsonElement element, String field) {
        if (!element.isJsonObject()) {
            return null;
        }

        return element.getAsJsonObject().get(field);
    }

    /**
     * Gets an item from an array.
     *
     * @param element array from which to extract the item
     * @param index   index of the item to extract
     * @return the item, or {@code null} if the element is not an array or if the index is out of bounds
     */
    protected JsonElement getItemFromArray(JsonElement element, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("subscript is invalid: " + index);
        }

        if (!element.isJsonArray()) {
            return null;
        }

        var array = element.getAsJsonArray();

        if (index >= array.size()) {
            return null;
        }

        return array.get(index);
    }
}
