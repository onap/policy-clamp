/*-
 * ============LICENSE_START=======================================================
 * policy-management
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.common.utils.gson;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import org.onap.policy.common.gson.JacksonHandler;

/**
 * Used to builder a utility class.
 */
public class GsonTestUtilsBuilder {
    private final GsonBuilder gsonBldr;

    /**
     * Constructs the object.
     */
    public GsonTestUtilsBuilder() {
        gsonBldr = new GsonBuilder();

        // register jackson behaviors with the builder
        JacksonHandler.configBuilder(gsonBldr);
    }

    /**
     * Builds the utility.
     *
     * @return a new utility
     */
    public GsonTestUtils build() {
        return new GsonTestUtils(gsonBldr.create());
    }

    /**
     * Adds gson support for serializing a mock of a class.
     *
     * @param clazz mocked class to be supported
     * @param sgson gson serializer
     */
    protected <T> void addMock(Class<T> clazz, TypeAdapterFactory sgson) {
        gsonBldr.registerTypeAdapterFactory(sgson);
    }
}
