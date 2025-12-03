/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.JacksonExclusionStrategy;
import org.onap.policy.common.gson.internal.DataAdapterFactory.Data;

class AnyGetterSerializerTest {

    private static DataAdapterFactory dataAdapter = new DataAdapterFactory();

    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(dataAdapter)
                    .setExclusionStrategies(new JacksonExclusionStrategy()).create();

    private Set<String> set;
    private AnyGetterSerializer ser;

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        set = new HashSet<>(Arrays.asList("id", "value"));
        ser = new AnyGetterSerializer(gson, set, MapData.class.getDeclaredMethod("getTheMap"));
    }

    @Test
    void testAddToTree_testCopyLiftedItems() {
        JsonObject tree = new JsonObject();
        tree.addProperty("hello", "world");

        MapData data = new MapData();

        data.map = DataAdapterFactory.makeMap();

        // this should not be copied because it is in the "set"
        data.map.put("value", Arrays.asList(new Data(1000)));

        dataAdapter.reset();
        JsonObject tree2 = tree.deepCopy();
        ser.addToTree(data, tree2);

        assertTrue(dataAdapter.isDataWritten());

        DataAdapterFactory.addToObject(tree);

        assertEquals(tree.toString(), tree2.toString());
    }

    @Test
    void testAddToTree_NullMap() {
        JsonObject tree = new JsonObject();
        tree.addProperty("hello", "world");

        MapData data = new MapData();

        // leave "map" unset

        JsonObject tree2 = tree.deepCopy();
        ser.addToTree(data, tree2);

        assertEquals(tree.toString(), tree2.toString());
    }

    @Test
    void testAddToTree_NotAnObject() throws Exception {
        ser = new AnyGetterSerializer(gson, set, NotAnObject.class.getDeclaredMethod("getNonMap"));

        JsonObject tree = new JsonObject();

        NotAnObject data = new NotAnObject();
        data.text = "bye bye";

        assertThatThrownBy(() -> ser.addToTree(data, tree)).isInstanceOf(JsonParseException.class)
                        .hasMessage(AnyGetterSerializer.NOT_AN_OBJECT_ERR + NotAnObject.class.getName() + ".getNonMap");
    }

    public static class MapData {
        protected int id;
        protected String value;
        protected Map<String, List<Data>> map;

        protected Map<String, List<Data>> getTheMap() {
            return map;
        }
    }

    /**
     * The "lifted" property is not a JsonObject so it should throw an exception.
     */
    public static class NotAnObject {
        protected String text;

        public String getNonMap() {
            return text;
        }
    }
}
