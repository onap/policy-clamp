/*--
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.JacksonExclusionStrategy;
import org.onap.policy.common.gson.internal.DataAdapterFactory.Data;

class MethodSerializerTest {
    private static final String PROP_NAME = "text";
    private static final String METHOD_NAME = "getText";

    private static DataAdapterFactory dataAdapter = new DataAdapterFactory();

    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(dataAdapter)
            .setExclusionStrategies(new JacksonExclusionStrategy()).create();

    private MethodSerializer ser;

    private String text;

    private List<Data> listField;

    @Test
    void testAddToTree() throws Exception {
        ser = new MethodSerializer(gson, MethodSerializerTest.class.getDeclaredMethod(METHOD_NAME));

        // serialize null value first
        text = null;

        JsonObject json = new JsonObject();
        ser.addToTree(this, json);
        assertTrue(json.get(PROP_NAME).isJsonNull());

        // serialize an actual value
        text = "hello";
        ser.addToTree(this, json);
        assertEquals("hello", json.get(PROP_NAME).getAsString());

        /*
         * check list field
         */
        listField = DataAdapterFactory.makeList();

        ser = new MethodSerializer(gson, MethodSerializerTest.class.getDeclaredMethod("getTheList"));

        dataAdapter.reset();
        JsonElement tree = ser.toJsonTree(listField);

        assertTrue(dataAdapter.isDataWritten());
        assertEquals(DataAdapterFactory.ENCODED_LIST,  tree.toString());
    }

    protected String getText() {
        return text;
    }

    protected List<Data> getTheList() {
        return listField;
    }
}
