/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019-2020 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2024-2025 Nordix Foundation
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

package org.onap.policy.common.utils.coder;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StandardCoderObjectTest {
    private static final Gson gson = new Gson();

    private static final String PROP1 = "abc";
    private static final String PROP2 = "ghi";
    private static final Integer PROP2_INDEX = 1;
    private static final String PROP_2_B = "jkl";
    private static final String VAL1 = "def";
    private static final String VAL2 = "mno";
    private static final String JSON = "{'abc':'def','ghi':[{},{'jkl':'mno'}]}".replace('\'', '"');

    private StandardCoderObject sco;

    /**
     * Creates a standard object, populated with some data.
     *
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        sco = new StandardCoderObject(gson.fromJson(JSON, JsonElement.class));
    }

    @Test
    void testStandardCoderObject() {
        assertNull(new StandardCoderObject().getData());
    }

    @Test
    void testStandardCoderObjectJsonElement() {
        assertNotNull(sco.getData());
        assertEquals(JSON, gson.toJson(sco.getData()));
    }

    @Test
    void testGetString() throws Exception {
        // one field
        assertEquals(VAL1, sco.getString(PROP1));

        // multiple fields
        assertEquals(VAL2, sco.getString(PROP2, PROP2_INDEX, PROP_2_B));

        // not found
        assertNull(sco.getString("xyz"));

        // read from null object
        assertNull(new StandardCoderObject().getString());
        assertNull(new StandardCoderObject().getString(PROP1));

        JsonElement obj = gson.fromJson("{'abc':[]}".replace('\'', '"'), JsonElement.class);
        sco = new StandardCoderObject(obj);

        // not a primitive
        assertNull(sco.getString(PROP1));

        // not a JSON object
        assertNull(sco.getString(PROP1, PROP2));

        // invalid subscript
        assertThatIllegalArgumentException().isThrownBy(() -> sco.getString(10.0));
    }

    @Test
    void testGetFieldFromObject() {
        // not an object
        assertNull(sco.getFieldFromObject(fromJson("[]"), PROP1));

        // field doesn't exist
        assertNull(sco.getFieldFromObject(fromJson("{}"), "non-existent"));

        // field exists
        assertEquals(4, sco.getFieldFromObject(fromJson("{\"world\":4}"), "world").getAsInt());
    }

    @Test
    void testGetItemFromArray() {
        // not an array
        assertNull(sco.getItemFromArray(fromJson("{}"), 0));

        // negative index
        assertThatIllegalArgumentException().isThrownBy(() -> sco.getItemFromArray(fromJson("[]"), -1));

        // index out of bounds
        assertNull(sco.getItemFromArray(fromJson("[5]"), 1));
        assertNull(sco.getItemFromArray(fromJson("[5]"), 2));

        // index exists
        assertEquals(6, sco.getItemFromArray(fromJson("[5,6,7]"), 1).getAsInt());

        // edge case: first and last item
        assertEquals(50, sco.getItemFromArray(fromJson("[50,60,70]"), 0).getAsInt());
        assertEquals(700, sco.getItemFromArray(fromJson("[500,600,700]"), 2).getAsInt());
    }

    private JsonElement fromJson(String json) {
        return gson.fromJson(json, JsonElement.class);
    }
}
