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
import com.google.gson.JsonObject;
import java.lang.reflect.Method;

/**
 * Serializer for methods that are exposed.
 */
public class MethodSerializer extends MethodAdapter implements Serializer {

    /**
     * Constructs the object.
     *
     * @param gson Gson object providing type adapters
     * @param getter method used to get the item from within an object
     */
    public MethodSerializer(Gson gson, Method getter) {
        super(gson, getter, getter.getGenericReturnType());
    }

    @Override
    public void addToTree(Object source, JsonObject target) {
        Object value = invoke(source);
        target.add(getPropName(), (value == null ? null : toJsonTree(value)));
    }
}
