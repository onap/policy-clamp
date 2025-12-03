/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Serializer for methods having a JsonAnyGetter annotation.
 */
public class AnyGetterSerializer extends Lifter implements Serializer {

    public static final String NOT_AN_OBJECT_ERR = "expecting a JsonObject for ";

    /**
     * Constructs the object.
     *
     * @param gson Gson object providing type adapters
     * @param unliftedProps property names that should not be lifted
     * @param getter method used to get the item from within an object
     */
    public AnyGetterSerializer(Gson gson, Set<String> unliftedProps, Method getter) {
        super(gson, unliftedProps, getter, getter.getGenericReturnType());
    }

    @Override
    public void addToTree(Object source, JsonObject target) {
        // get the value from the object
        Object value = invoke(source);
        if (value == null) {
            // nothing to lift
            return;
        }

        JsonElement inner = toJsonTree(value);
        if (!inner.isJsonObject()) {
            throw new JsonParseException(makeError(NOT_AN_OBJECT_ERR));
        }

        // lift items from inner into the target
        copyLiftedItems(inner.getAsJsonObject(), target);
    }

    /**
     * Copies lifted items from one tree into another, without removing them from the
     * source tree.
     *
     * @param source tree from which items are to be copied
     * @param target tree into which items are to be copied
     */
    private void copyLiftedItems(JsonObject source, JsonObject target) {
        for (Entry<String, JsonElement> ent : source.entrySet()) {
            String name = ent.getKey();
            if (shouldLift(name)) {
                target.add(name, ent.getValue());
            }
        }
    }
}
