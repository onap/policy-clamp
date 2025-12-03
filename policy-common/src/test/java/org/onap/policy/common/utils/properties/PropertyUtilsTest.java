/*
 * ============LICENSE_START=======================================================
 * ONAP
 * ================================================================================
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
 * Modifications Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.common.utils.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PropertyUtilsTest {
    private static final String DFLT_STRING = "my-default";
    private static final int DLFT_INT = 1000;

    private PropertyUtils utils;
    private String invalidName;
    private String invalidValue;
    private Exception invalidEx;

    /**
     * Initializes {@link #utils}.
     */
    @BeforeEach
    public void setUp() {
        Properties properties = new Properties();
        properties.put("myPrefix.my-string", "some text");
        properties.put("myPrefix.empty-string", "");

        properties.put("myPrefix.my-bool", "true");
        properties.put("myPrefix.my-bool2", "false");
        properties.put("myPrefix.empty-bool", "");
        properties.put("myPrefix.invalid-bool", "not a bool");

        properties.put("myPrefix.my-int", "100");
        properties.put("myPrefix.my-int2", "200");
        properties.put("myPrefix.empty-int", "");
        properties.put("myPrefix.invalid-int", "not an int");

        utils = new PropertyUtils(properties, "myPrefix", (name, value, ex) -> {
            invalidName = name;
            invalidValue = value;
            invalidEx = ex;
        });
    }

    @Test
    void testGetString() {
        assertEquals("some text", utils.getString(".my-string", DFLT_STRING));
        assertEquals(DFLT_STRING, utils.getString(".empty-string", DFLT_STRING));
        assertEquals(DFLT_STRING, utils.getString(".missing-string", DFLT_STRING));

        assertNull(invalidName);
        assertNull(invalidValue);
        assertNull(invalidEx);
    }

    @Test
    void testGetBoolean() {
        assertTrue(utils.getBoolean(".my-bool", false));
        assertFalse(utils.getBoolean(".my-bool2", true));
        assertTrue(utils.getBoolean(".empty-bool", true));
        assertFalse(utils.getBoolean(".invalid-bool", true));
        assertTrue(utils.getBoolean(".missing-bool", true));

        assertNull(invalidName);
        assertNull(invalidValue);
        assertNull(invalidEx);
    }

    @Test
    void testGetInteger() {
        assertEquals(100, utils.getInteger(".my-int", DLFT_INT));
        assertEquals(200, utils.getInteger(".my-int2", DLFT_INT));
        assertEquals(DLFT_INT, utils.getInteger(".empty-int", DLFT_INT));
        assertEquals(DLFT_INT, utils.getInteger(".missing-int", DLFT_INT));

        assertNull(invalidName);
        assertNull(invalidValue);
        assertNull(invalidEx);

        assertEquals(DLFT_INT, utils.getInteger(".invalid-int", DLFT_INT));

        assertEquals("myPrefix.invalid-int", invalidName);
        assertEquals("not an int", invalidValue);
        assertTrue(invalidEx instanceof NumberFormatException);
    }

}
