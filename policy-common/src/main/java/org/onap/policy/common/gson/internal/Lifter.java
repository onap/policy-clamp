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
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Super class of serializers and de-serializers that deal with "lifted" data, that is,
 * data that is lifted from a nested json object into the containing object.
 */
public class Lifter extends MethodAdapter {

    /**
     * Names of the properties that are <i>not</i> to be lifted.
     */
    private final Set<String> unliftedProps;

    /**
     * Constructs the object.
     *
     * @param gson Gson object providing type adapters
     * @param unliftedProps property names that should not be lifted
     * @param accessor method used to access the item from within an object
     * @param type the class of value on which this operates
     */
    public Lifter(Gson gson, Set<String> unliftedProps, Method accessor, Type type) {
        super(gson, accessor, type);

        this.unliftedProps = unliftedProps;
    }

    /**
     * Determines if a property should be lifted.
     *
     * @param propName the name of the property
     * @return {@code true} if the property should be lifted, {@code false} otherwise
     */
    public boolean shouldLift(String propName) {
        return !unliftedProps.contains(propName);
    }
}
