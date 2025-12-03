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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.JacksonExclusionStrategy;
import org.onap.policy.common.gson.internal.DataAdapterFactory.Data;

class MethodDeserializerTest {
    private static final String PROP_NAME = "text";
    private static final String METHOD_NAME = "setText";
    private static final String INITIAL_VALUE = "initial value";
    private static final String NEW_VALUE = "new value";

    private static DataAdapterFactory dataAdapter = new DataAdapterFactory();

    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(dataAdapter)
            .setExclusionStrategies(new JacksonExclusionStrategy()).create();

    private MethodDeserializer deser;

    private String text;

    private List<Data> listField;

    @Test
    void testGetFromTree() throws Exception {
        deser = new MethodDeserializer(gson, MethodDeserializerTest.class.getDeclaredMethod(METHOD_NAME, String.class));

        // non-existent value - should not overwrite
        text = INITIAL_VALUE;
        JsonObject json = new JsonObject();
        deser.getFromTree(json, this);
        assertEquals(INITIAL_VALUE, text);

        // null value - should not overwrite
        text = INITIAL_VALUE;
        json.add(PROP_NAME, JsonNull.INSTANCE);
        deser.getFromTree(json, this);
        assertEquals(INITIAL_VALUE, text);

        // has a value - should store it
        text = INITIAL_VALUE;
        json.addProperty(PROP_NAME, NEW_VALUE);
        deser.getFromTree(json, this);
        assertEquals(NEW_VALUE, text);

        /*
         * check list field
         */
        deser = new MethodDeserializer(gson, MethodDeserializerTest.class.getDeclaredMethod("setTheList", List.class));

        json = new JsonObject();
        json.add("theList", DataAdapterFactory.makeArray());

        dataAdapter.reset();
        listField = null;
        deser.getFromTree(json, this);

        assertTrue(dataAdapter.isDataRead());
        assertNotNull(listField);
        assertEquals(DataAdapterFactory.makeList().toString(), listField.toString());
    }

    protected void setText(String text) {
        this.text = text;
    }

    protected void setTheList(List<Data> lst) {
        listField = lst;
    }
}
