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

import com.fasterxml.jackson.databind.node.ObjectNode;

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

    /**
     * Test getting prop value as a JSON Node / template.
     *
     * @throws IOException
     *         when JSON parsing fails
     */
    @Test
    public void testGetJsonTemplate() throws IOException {
        // ui.location.default={"DC1":"Data Center 1","DC2":"Data Center
        // 2","DC3":"Data Center 3"}
        ObjectNode root = (ObjectNode) refProp.getJsonTemplate("ui.location.default");
        assertNotNull(root);
        assertEquals(root.get("DC1").asText(), "Data Center 1");
        // Test composite key
        root = (ObjectNode) refProp.getJsonTemplate("ui.location", "default");
        assertNotNull(root);
        assertEquals(root.get("DC1").asText(), "Data Center 1");
        root = (ObjectNode) refProp.getJsonTemplate("ui.location", "");
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
        String content = refProp.getFileContent("sdc.decode.service_ids");
        assertEquals("{}", content);
        // Test composite key
        content = refProp.getFileContent("sdc.decode", "service_ids");
        assertEquals("{}", content);
    }

    @Test
    public void testGetStringList() {
        List<String> profileList = refProp.getStringList("policy.pdpUrl1", ",");
        assertTrue(profileList.size() == 3);
        assertTrue(profileList.get(0).trim().equals("http://localhost:8085/pdp/"));
        assertTrue(profileList.get(1).trim().equals("testpdp"));
        assertTrue(profileList.get(2).trim().equals("alpha123"));
    }
}
