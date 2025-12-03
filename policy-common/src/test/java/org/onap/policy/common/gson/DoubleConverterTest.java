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

package org.onap.policy.common.gson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DoubleConverterTest {

    @Test
    void testConvertFromDoubleObject() {
        // these should be unchanged
        assertNull(DoubleConverter.convertFromDouble((Object) null));
        assertEquals("hello", DoubleConverter.convertFromDouble("hello"));
        assertEquals("10.0", DoubleConverter.convertFromDouble("10.0"));
        assertEquals(12.5, DoubleConverter.convertFromDouble(12.5));
        assertEquals(12, DoubleConverter.convertFromDouble(12));
        assertEquals(12L, DoubleConverter.convertFromDouble(12L));

        // positive and negative int
        assertEquals(10, DoubleConverter.convertFromDouble(10.0));
        assertEquals(-10, DoubleConverter.convertFromDouble(-10.0));

        // positive and negative long
        assertEquals(100000000000L, DoubleConverter.convertFromDouble(100000000000.0));
        assertEquals(-100000000000L, DoubleConverter.convertFromDouble(-100000000000.0));

        // list
        List<Object> list = new ArrayList<>();
        list.add("list");
        list.add(null);
        list.add(21.0);
        list = (List<Object>) DoubleConverter.convertFromDouble((Object) list);
        assertEquals("[list, null, 21]", list.toString());

        // map
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("map-A", "map-value");
        map.put("map-B", null);
        map.put("map-C", 22.0);
        map = (Map<String, Object>) DoubleConverter.convertFromDouble((Object) map);
        assertEquals("{map-A=map-value, map-B=null, map-C=22}", map.toString());
    }

    @Test
    void testConvertFromDoubleList() {
        // null is ok
        DoubleConverter.convertFromDouble((List<Object>) null);

        List<Object> list = new ArrayList<>();
        list.add("world");
        list.add(20.0);

        List<Object> nested = new ArrayList<>();
        list.add(nested);
        nested.add(30.0);

        DoubleConverter.convertFromDouble(list);

        assertEquals("[world, 20, [30]]", list.toString());
    }

    @Test
    void testConvertFromDoubleMap() {
        // null is ok
        DoubleConverter.convertFromDouble((Map<String, Object>) null);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("keyA", "valueA");
        map.put("keyB", 200.0);

        Map<String, Object> nested = new LinkedHashMap<>();
        map.put("keyC", nested);
        nested.put("nested-key", 201.0);

        DoubleConverter.convertFromDouble(map);
        assertEquals("{keyA=valueA, keyB=200, keyC={nested-key=201}}", map.toString());
    }
}
