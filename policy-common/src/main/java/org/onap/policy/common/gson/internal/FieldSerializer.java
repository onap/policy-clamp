/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.gson.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Field;

/**
 * Serializer for fields that are exposed.
 */
public class FieldSerializer extends Adapter implements Serializer {

    public static final String GET_ERR = "cannot get field: ";

    /**
     * Field within the object.
     */
    private final Field field;

    /**
     * Constructs the object.
     *
     * @param gson Gson object providing type adapters
     * @param field field within the object
     */
    public FieldSerializer(Gson gson, Field field) {
        super(gson, field);

        this.field = field;

        /*
         * Turning off sonar, as this is required for emulation of "jackson".
         */
        field.setAccessible(true);  // NOSONAR
    }

    @Override
    public void addToTree(Object source, JsonObject target) {
        Object value;
        try {
            value = getFromObject(source);

        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new JsonParseException(makeError(GET_ERR), e);
        }

        JsonElement jsonEl = (value == null ? JsonNull.INSTANCE : toJsonTree(value));
        target.add(getPropName(), jsonEl);
    }

    /**
     * Gets the field from the source object. Overridden during junit testing.
     *
     * @param source object containing the field
     * @return the field's value
     * @throws IllegalAccessException if an error occurs
     */
    protected Object getFromObject(Object source) throws IllegalAccessException {
        return field.get(source);
    }
}
