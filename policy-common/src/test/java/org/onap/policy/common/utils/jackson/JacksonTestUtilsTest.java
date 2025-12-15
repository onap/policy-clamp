/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved
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

package org.onap.policy.common.utils.jackson;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import java.io.File;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JacksonTestUtilsTest {
    private static final String HELLO = "hello";
    private JacksonTestUtils utils;

    @BeforeEach
    void setUp() {
        utils = new JacksonTestUtils();
    }

    @Test
    void testCompareJsonObjectClass_testCompareJsonObjectFile() throws JsonParseException {
        Data data = new Data();
        data.setId(500);
        data.setText(HELLO);
        // file not found
        var file = new File(JacksonTestUtilsTest.class.getSimpleName() + "-NotFound.json");
        assertThatThrownBy(() -> utils.compareJson(data, file))
                .isInstanceOf(JsonParseException.class);
        // force I/O error while reading file
        JacksonTestUtils utils2 = new JacksonTestUtils() {
            @Override
            protected String readFile(File file) throws IOException {
                throw new IOException("expected exception");
            }
        };
        var file2 = new File(JacksonTestUtilsTest.class.getSimpleName() + ".json");
        assertThatThrownBy(() -> utils2.compareJson(data, file2)).isInstanceOf(JsonParseException.class)
                .hasMessage("error reading: expected exception");
    }

    @Test
    void testCompareJsonObjectString() {
        Data data = new Data();
        data.setId(600);
        data.setText(HELLO);
        assertThatCode(() -> utils.compareJson(data, "{\"id\":600,\"text\":\"hello\"}"))
                .doesNotThrowAnyException();
    }

    @Test
    void testCompareJsonObjectJsonElement() throws JsonParseException {
        Data data = new Data();
        data.setId(650);
        data.setText(HELLO);
        var json = new ObjectMapper().createObjectNode();
        json.putPOJO("id", data.getId());
        json.putPOJO("text", data.getText());
        utils.compareJson(data, json);
        // mismatch
        data.setText("world");
        assertThatThrownBy(() -> utils.compareJson(data, json)).isInstanceOf(AssertionError.class);
    }

    @Test
    void testReorderJsonObject() {
        // insert properties in a non-alphabetical order
        var mapper = new ObjectMapper();
        var inner = mapper.createObjectNode();
        inner.put("objBint", 100);
        inner.put("objB", true);
        var arr = mapper.createArrayNode();
        arr.add(110);
        arr.add(inner);
        arr.add(false);
        var outer = mapper.createObjectNode();
        outer.putPOJO("objA", true);
        outer.putPOJO("objAStr", "obj-a-string");
        outer.putPOJO("nested-array", arr);
        outer = utils.reorder(outer);
        assertEquals("{'nested-array':[110,{'objBint':100,'objB':true},false],'objA':true,'objAStr':'obj-a-string'}"
                .replace('\'', '"'), outer.toString());
    }

    @Test
    void testReorderJsonArray() {
        // insert properties in a non-alphabetical order
        var mapper = new ObjectMapper();
        var inner = mapper.createObjectNode();
        inner.put("objCStr", "obj-c-string");
        inner.put("objC", true);
        var arr = mapper.createArrayNode();
        arr.add(200);
        arr.add(inner);
        arr.add(false);
        arr = utils.reorder(arr);
        assertEquals("[200,{'objC':true,'objCStr':'obj-c-string'},false]".replace('\'', '"'), arr.toString());
    }

    @Test
    void testReorderJsonElement() {
        // null element
        JsonNode jsonNode = null;
        assertTrue(utils.reorder(jsonNode).isNull());
        var mapper = new ObjectMapper();
        // object element
        var obj = mapper.createObjectNode();
        obj.set("objDNull", NullNode.instance);
        obj.put("objDStr", "obj-d-string");
        obj.put("objD", true);
        jsonNode = obj;
        jsonNode = utils.reorder(jsonNode);
        assertEquals("{'objD':true,'objDStr':'obj-d-string'}".replace('\'', '"'), jsonNode.toString());
        // boolean
        jsonNode = obj.get("objD");
        jsonNode = utils.reorder(jsonNode);
        assertEquals("true", jsonNode.toString());
        // JsonNull
        jsonNode = NullNode.instance;
        jsonNode = utils.reorder(jsonNode);
        assertEquals("null", jsonNode.toString());
        // array element
        var inner = mapper.createObjectNode();
        inner.put("objEStr", "obj-e-string");
        inner.put("objE", true);
        var arr = mapper.createArrayNode();
        arr.add(300);
        arr.add(inner);
        arr.add(false);
        jsonNode = arr;
        jsonNode = utils.reorder(jsonNode);
        assertEquals("[300,{'objE':true,'objEStr':'obj-e-string'},false]".replace('\'', '"'), jsonNode.toString());
    }

    @Setter
    @Getter
    @ToString
    public static class Data {
        private int id;
        private String text;
    }
}

