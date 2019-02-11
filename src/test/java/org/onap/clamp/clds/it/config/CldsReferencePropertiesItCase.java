/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017-2018 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END============================================
 * ===================================================================
 *
 */

package org.onap.clamp.clds.it.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.config.ClampProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test corg.onap.clamp.ClampDesigner.model.refprop package using RefProp.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class CldsReferencePropertiesItCase {

    @Autowired
    private ClampProperties refProp;

    /**
     * Test getting a value the properties in string.
     */
    @Test
    public void testGetStringValue() {
        assertEquals(refProp.getStringValue("policy.onap.name"), "DCAE");
        assertEquals(refProp.getStringValue("policy.ms.policyNamePrefix", ""), "Config_MS_");
        assertEquals(refProp.getStringValue("policy.ms.policyNamePrefix", "testos"), "Config_MS_");
        assertEquals(refProp.getStringValue("policy.ms", "policyNamePrefix"), "Config_MS_");
        assertNull(refProp.getStringValue("does.not.exist"));
    }

    @Test
    public void shouldReturnJsonFromTemplate() throws IOException {
        //when
        JsonElement root = refProp.getJsonTemplate("ui.location.default");

        //then
        assertNotNull(root);
        assertTrue(root.isJsonObject());
        assertEquals(root.getAsJsonObject().get("DC1").getAsString(), "Data Center 1");
    }

    @Test
    public void shouldReturnJsonFromTemplate_2() throws IOException {
        //when
        JsonElement root = refProp.getJsonTemplate("ui.location", "default");

        //then
        assertNotNull(root);
        assertTrue(root.isJsonObject());
        assertEquals(root.getAsJsonObject().get("DC1").getAsString(), "Data Center 1");
    }

    @Test
    public void shouldReturnNullIfPropertyNotFound() throws IOException {
        //when
        JsonElement root = refProp.getJsonTemplate("ui.location", "");

        //then
        assertNull(root);
    }

    /**
     * Test getting prop value as a JSON Node / template.
     *
     * @throws IOException
     *         when JSON parsing fails
     */
    @Test
    public void testGetFileContent() throws IOException {
        String location = "{\n\t\"DC1\": \"Data Center 1\",\n\t\"DC2\": \"Data Center 2\",\n\t\"DC3\": \"Data Center 3\"\n}\n";
        String content = refProp.getFileContent("ui.location.default");
        assertEquals(location, content);
        // Test composite key
        content = refProp.getFileContent("ui.location", "default");
        assertEquals(location, content);
    }

    @Test
    public void testGetStringList() {
        List<String> profileList = refProp.getStringList("policy.pdpUrl1", ",");
        assertTrue(profileList.size() == 3);
        assertTrue(profileList.get(0).trim().startsWith("http://localhost:"));
        assertTrue(profileList.get(1).trim().equals("testpdp"));
        assertTrue(profileList.get(2).trim().equals("alpha123"));
    }
}
