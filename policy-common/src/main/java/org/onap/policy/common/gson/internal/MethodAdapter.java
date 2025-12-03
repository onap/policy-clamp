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
import com.google.gson.JsonParseException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Super class of adapters used to serialize and de-serialize a method.
 */
public class MethodAdapter extends Adapter {

    public static final String INVOKE_ERR = "cannot invoke method to serialize/deserialize: ";

    /**
     * Method used to access the item within an object.
     */
    private final Method accessor;

    /**
     * Constructs the object.
     *
     * @param gson Gson object providing type adapters
     * @param accessor method used to access the item from within an object
     * @param type the class of value on which this operates
     */
    public MethodAdapter(Gson gson, Method accessor, Type type) {
        super(gson, accessor, type);

        this.accessor = accessor;
    }

    /**
     * Invokes the accessor method.
     *
     * @param self object on which to invoke the method
     * @param args arguments to be passed to the method
     * @return the method's result
     */
    public Object invoke(Object self, Object... args) {
        try {
            return accessor.invoke(self, args);

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new JsonParseException(makeError(INVOKE_ERR), e);
        }
    }
}
