/*
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

package org.onap.policy.common.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MapDoubleAdapterFactoryTest {
    private static Gson gson = new GsonBuilder().registerTypeAdapterFactory(new MapDoubleAdapterFactory()).create();

    @Test
    @SuppressWarnings("unchecked")
    void testMap() {
        MyMap map = new MyMap();
        map.data = new HashMap<>();
        map.data.put("plainString", "def");
        map.data.put("posInt", 10);
        map.data.put("negInt", -10);
        map.data.put("doubleVal", 12.5);
        map.data.put("posLong", 100000000000L);
        map.data.put("negLong", -100000000000L);

        Map<String, Object> nested = new LinkedHashMap<>();
        map.data.put("nestedMap", nested);
        nested.put("nestedString", "world");
        nested.put("nestedInt", 50);

        String json = gson.toJson(map);

        map.data.clear();
        map = gson.fromJson(json, MyMap.class);

        assertEquals(json, gson.toJson(map));

        assertEquals(10, map.data.get("posInt"));
        assertEquals(-10, map.data.get("negInt"));
        assertEquals(100000000000L, map.data.get("posLong"));
        assertEquals(-100000000000L, map.data.get("negLong"));
        assertEquals(12.5, map.data.get("doubleVal"));
        assertEquals(nested, map.data.get("nestedMap"));

        nested = (Map<String, Object>) map.data.get("nestedMap");
        assertEquals(50, nested.get("nestedInt"));
    }

    @Test
    void testList() {
        MyList list = new MyList();
        list.data = new ArrayList<>();
        list.data.add("ghi");
        list.data.add(100);

        List<Object> nested = new ArrayList<>();
        list.data.add(nested);
        nested.add("world2");
        nested.add(500);

        String json = gson.toJson(list);

        list.data.clear();
        list = gson.fromJson(json, MyList.class);

        assertEquals(json, gson.toJson(list));

        assertEquals("[ghi, 100, [world2, 500]]", list.data.toString());
    }

    @Test
    void test_ValueIsNotObject() {
        MyDoubleMap map = new MyDoubleMap();
        map.data = new LinkedHashMap<>();
        map.data.put("plainDouble", 13.5);
        map.data.put("doubleAsInt", 100.0);

        String json = gson.toJson(map);

        map.data.clear();
        map = gson.fromJson(json, MyDoubleMap.class);

        // everything should still be Double - check by simply accessing
        assertNotNull(map.data.get("plainDouble"));
        assertNotNull(map.data.get("doubleAsInt"));
    }

    @Test
    void test_KeyIsNotString() {
        MyObjectMap map = new MyObjectMap();

        map.data = new LinkedHashMap<>();
        map.data.put("plainDouble2", 14.5);
        map.data.put("doubleAsInt2", 200.0);

        String json = gson.toJson(map);

        map.data.clear();
        map = gson.fromJson(json, MyObjectMap.class);

        // everything should still be Double
        assertEquals(14.5, map.data.get("plainDouble2"));
        assertEquals(200.0, map.data.get("doubleAsInt2"));
    }

    @Test
    void test_ListValueIsNotObject() {
        MyDoubleList list = new MyDoubleList();
        list.data = new ArrayList<>();
        list.data.add(13.5);
        list.data.add(100.0);

        String json = gson.toJson(list);

        list.data.clear();
        list = gson.fromJson(json, MyDoubleList.class);

        // everything should still be Double - check by simply accessing
        assertEquals("[13.5, 100.0]", list.data.toString());
    }

    private static class MyMap {
        private Map<String, Object> data;
    }

    private static class MyDoubleMap {
        private Map<String, Double> data;
    }

    private static class MyObjectMap {
        private Map<Object, Object> data;
    }

    private static class MyList {
        private List<Object> data;
    }

    private static class MyDoubleList {
        private List<Double> data;
    }

}
