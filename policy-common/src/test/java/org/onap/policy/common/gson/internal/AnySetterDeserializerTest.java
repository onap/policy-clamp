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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.common.gson.JacksonExclusionStrategy;
import org.onap.policy.common.gson.internal.DataAdapterFactory.Data;

class AnySetterDeserializerTest {

    private static DataAdapterFactory dataAdapter = new DataAdapterFactory();

    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(dataAdapter)
                    .setExclusionStrategies(new JacksonExclusionStrategy()).create();

    private Set<String> set;
    private AnySetterDeserializer deser;

    /**
     * Set up.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    void setUp() throws Exception {
        set = new HashSet<>(Arrays.asList("id", "value"));
        deser = new AnySetterDeserializer(gson, set,
                        MapData.class.getDeclaredMethod("setItem", String.class, List.class));
    }

    @Test
    void testAnySetterDeserializer() {
        JsonObject json = new JsonObject();

        // these should not be copied
        json.addProperty("id", 10);
        json.addProperty("value", "the-value");

        // these should be copied
        DataAdapterFactory.addToObject(json);

        MapData data = new MapData();
        data.map = new TreeMap<>();

        dataAdapter.reset();
        deser.getFromTree(json, data);

        assertTrue(dataAdapter.isDataRead());
        assertNotNull(data.map);
        assertEquals(DataAdapterFactory.makeMap().toString(), data.map.toString());
    }

    public static class MapData {
        protected Map<String, List<Data>> map;

        protected void setItem(String key, List<Data> value) {
            map.put(key, value);
        }
    }

}
