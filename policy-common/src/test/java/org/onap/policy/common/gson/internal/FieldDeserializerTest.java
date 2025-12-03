/*-
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.JacksonExclusionStrategy;
import org.onap.policy.common.gson.internal.DataAdapterFactory.Data;

class FieldDeserializerTest {
    private static final String TEXT_FIELD_NAME = "text";
    private static final String LIST_FIELD_NAME = "listField";
    private static final String INITIAL_VALUE = "initial value";
    private static final String NEW_VALUE = "new value";

    private static DataAdapterFactory dataAdapter = new DataAdapterFactory();

    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(dataAdapter)
                    .setExclusionStrategies(new JacksonExclusionStrategy()).create();

    private FieldDeserializer deser;

    private String text;

    private List<Data> listField;

    @Test
    void testGetFromTree() throws Exception {
        deser = new FieldDeserializer(gson, FieldDeserializerTest.class.getDeclaredField(TEXT_FIELD_NAME));

        JsonObject json = new JsonObject();

        // no value in tree - text remains unchanged
        text = INITIAL_VALUE;
        deser.getFromTree(json, this);
        assertEquals(INITIAL_VALUE, text);

        // null value in tree - text remains unchanged
        json.add(TEXT_FIELD_NAME, JsonNull.INSTANCE);
        deser.getFromTree(json, this);
        assertEquals(INITIAL_VALUE, text);

        // now assign a value - text should be changed now
        json.addProperty(TEXT_FIELD_NAME, NEW_VALUE);

        deser.getFromTree(json, this);
        assertEquals(NEW_VALUE, text);

        /*
         * check list field
         */
        deser = new FieldDeserializer(gson, FieldDeserializerTest.class.getDeclaredField(LIST_FIELD_NAME));

        json.add(LIST_FIELD_NAME, DataAdapterFactory.makeArray());

        dataAdapter.reset();
        listField = null;
        deser.getFromTree(json, this);

        assertTrue(dataAdapter.isDataRead());
        assertEquals(DataAdapterFactory.makeList().toString(), listField.toString());
    }

    @Test
    void testGetFromTree_SetEx() throws Exception {
        deser = new FieldDeserializer(gson, FieldDeserializerTest.class.getDeclaredField(TEXT_FIELD_NAME)) {
            @Override
            public Object fromJsonTree(JsonElement tree) {
                // return an int, which won't fit in a String - cause an exception
                return 10;
            }
        };

        JsonObject json = new JsonObject();
        json.addProperty(TEXT_FIELD_NAME, NEW_VALUE);

        assertThatThrownBy(() -> deser.getFromTree(json, this)).isInstanceOf(JsonParseException.class)
                        .hasMessage(FieldDeserializer.SET_ERR + FieldDeserializerTest.class.getName() + ".text");
    }
}
