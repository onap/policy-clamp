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
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.Set;

/**
 * De-serializer for methods having a JsonAnySetter annotation.
 */
public class AnySetterDeserializer extends Lifter implements Deserializer {

    /**
     * Constructs the object.
     *
     * @param gson Gson object providing type adapters
     * @param unliftedProps property names that should not be lifted
     * @param setter method used to set the item within an object
     */
    public AnySetterDeserializer(Gson gson, Set<String> unliftedProps, Method setter) {
        super(gson, unliftedProps, setter, setter.getGenericParameterTypes()[1]);
    }

    @Override
    public void getFromTree(JsonObject source, Object target) {
        for (Entry<String, JsonElement> ent : source.entrySet()) {
            String name = ent.getKey();

            if (shouldLift(name)) {
                Object value = fromJsonTree(ent.getValue());
                invoke(target, name, value);
            }
        }
    }

}
