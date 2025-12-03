/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2021 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GsonTestUtilsTest {
    private static final String HELLO = "hello";

    private GsonTestUtils utils;

    @BeforeEach
    public void setUp() {
        utils = new GsonTestUtils();
    }

    @Test
    void testGetGson() {
        assertNotNull(utils.getGson());
    }

    @Test
    void testGsonRoundTrip() {
        Data data = new Data();
        data.setId(500);

        // try with null text
        data.setText(null);
        assertEquals(data.toString(), utils.gsonRoundTrip(data, Data.class).toString());

        // try with non-null text
        data.setText(HELLO);
        assertEquals(data.toString(), utils.gsonRoundTrip(data, Data.class).toString());
    }

    @Test
    void testCompareGsonObjectClass_testCompareGsonObjectFile() {
        Data data = new Data();
        data.setId(500);
        data.setText(HELLO);

        utils.compareGson(data, GsonTestUtilsTest.class);

        // file not found
        File file = new File(GsonTestUtilsTest.class.getSimpleName() + "-NotFound.json");

        assertThatThrownBy(() -> utils.compareGson(data, file))
            .isInstanceOf(JsonParseException.class)
            .hasCauseInstanceOf(FileNotFoundException.class);

        // force I/O error while reading file
        GsonTestUtils utils2 = new GsonTestUtils() {
            @Override
            protected String readFile(File file) throws IOException {
                throw new IOException("expected exception");
            }
        };
        assertThatThrownBy(() -> utils2.compareGson(data, GsonTestUtilsTest.class))
                        .isInstanceOf(JsonParseException.class).hasCauseInstanceOf(IOException.class)
                        .hasMessage("error reading: GsonTestUtilsTest.json");
    }

    @Test
    void testCompareGsonObjectString() {
        Data data = new Data();
        data.setId(600);
        data.setText(HELLO);

        assertThatCode(() -> utils.compareGson(data, "{'id': ${obj.id}, 'text': '${obj.text}'}".replace('\'', '"')))
                        .doesNotThrowAnyException();
    }

    @Test
    void testCompareGsonObjectJsonElement() {
        Data data = new Data();
        data.setId(650);
        data.setText(HELLO);

        JsonObject json = new JsonObject();
        json.addProperty("id", data.getId());
        json.addProperty("text", data.getText());

        utils.compareGson(data, json);

        // mismatch
        data.setText("world");
        assertThatThrownBy(() -> utils.compareGson(data, json)).isInstanceOf(AssertionError.class);
    }

    @Test
    void testApplyScripts() {
        Data data = new Data();
        data.setId(700);
        data.setText(HELLO);

        String result = utils.applyScripts("no interpolation", data);
        assertEquals("no interpolation", result);

        result = utils.applyScripts("${obj.id} at start, ${obj.text} in middle, and end ${obj.id}", data);
        assertEquals("700 at start, hello in middle, and end 700", result);

        // try null value
        data.setText(null);
        result = utils.applyScripts("use ${obj.text} this", data);
        assertEquals("use null this", result);
        assertEquals("use null this", utils.applyScripts("use ${obj.text} this", null));
    }

    @Test
    void testReorderJsonObject() {
        // insert properties in a non-alphabetical order
        JsonObject inner = new JsonObject();
        inner.addProperty("objBint", 100);
        inner.add("objBNull", JsonNull.INSTANCE);
        inner.addProperty("objB", true);

        JsonArray arr = new JsonArray();
        arr.add(110);
        arr.add(inner);
        arr.add(false);

        JsonObject outer = new JsonObject();
        outer.add("objANull", JsonNull.INSTANCE);
        outer.addProperty("objA", true);
        outer.addProperty("objAStr", "obj-a-string");
        outer.add("nested-array", arr);

        outer = utils.reorder(outer);
        assertEquals("{'nested-array':[110,{'objB':true,'objBint':100},false],'objA':true,'objAStr':'obj-a-string'}"
                        .replace('\'', '"'), outer.toString());
    }

    @Test
    void testReorderJsonArray() {
        // insert properties in a non-alphabetical order
        JsonObject inner = new JsonObject();
        inner.add("objCNull", JsonNull.INSTANCE);
        inner.addProperty("objCStr", "obj-c-string");
        inner.addProperty("objC", true);

        JsonArray arr = new JsonArray();
        arr.add(200);
        arr.add(inner);
        arr.add(false);

        arr = utils.reorder(arr);
        assertEquals("[200,{'objC':true,'objCStr':'obj-c-string'},false]".replace('\'', '"'), arr.toString());
    }

    @Test
    void testReorderJsonElement() {
        // null element
        JsonElement jsonEl = null;
        assertNull(utils.reorder(jsonEl));

        // object element
        JsonObject obj = new JsonObject();
        obj.add("objDNull", JsonNull.INSTANCE);
        obj.addProperty("objDStr", "obj-d-string");
        obj.addProperty("objD", true);
        jsonEl = obj;
        jsonEl = utils.reorder(jsonEl);
        assertEquals("{'objD':true,'objDStr':'obj-d-string'}".replace('\'', '"'), jsonEl.toString());

        // boolean
        jsonEl = obj.get("objD");
        jsonEl = utils.reorder(jsonEl);
        assertEquals("true", jsonEl.toString());

        // JsonNull
        jsonEl = JsonNull.INSTANCE;
        jsonEl = utils.reorder(jsonEl);
        assertEquals("null", jsonEl.toString());

        // array element
        JsonObject inner = new JsonObject();
        inner.add("objENull", JsonNull.INSTANCE);
        inner.addProperty("objEStr", "obj-e-string");
        inner.addProperty("objE", true);

        JsonArray arr = new JsonArray();
        arr.add(300);
        arr.add(inner);
        arr.add(false);
        jsonEl = arr;
        jsonEl = utils.reorder(jsonEl);
        assertEquals("[300,{'objE':true,'objEStr':'obj-e-string'},false]".replace('\'', '"'), jsonEl.toString());
    }

    @Setter
    @Getter
    @ToString
    public static class Data {
        private int id;
        private String text;

    }
}
