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

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.client.req.sdc.SdcRequests;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.properties.ModelProperties;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SdcReqItCase {

    @Autowired
    private SdcRequests sdcReq;
    private String modelBpmnProp;
    private String modelBpmn;
    private String modelName;
    private String controlName;
    private ModelProperties modelProperties;
    private String jsonWithYamlInside;

    /**
     * Initialize Test.
     */
    @Before
    public void setUp() throws IOException {
        modelBpmnProp = ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmnProperties.json");
        modelBpmn = ResourceFileUtil.getResourceAsString("example/model-properties/tca/modelBpmn.json");
        modelName = "example-model01";
        controlName = "ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf";
        modelProperties = new ModelProperties(modelName, controlName, CldsEvent.ACTION_SUBMIT, false, modelBpmn,
            modelBpmnProp);
        jsonWithYamlInside = ResourceFileUtil.getResourceAsString("example/tca-policy-req/prop-text.json");
    }

    @Test
    public void formatBlueprintTest() throws IOException {
        String blueprintFormatted = sdcReq.formatBlueprint(modelProperties, jsonWithYamlInside);
        assertEquals(ResourceFileUtil.getResourceAsString("example/tca-policy-req/blueprint-expected.yaml"),
            blueprintFormatted);
    }

    @Test
    public void formatSdcLocationsReqTest() {
        String blueprintFormatted = sdcReq.formatSdcLocationsReq(modelProperties, "testos");
        assertEquals(
            "{\"artifactName\":\"testos\",\"locations\":[\"SNDGCA64\",\"ALPRGAED\",\"LSLEILAA\",\"MDTWNJC1\"]}",
            blueprintFormatted);
    }

    @Test
    public void formatSdcReqTest() throws JSONException {
        String jsonResult = sdcReq.formatSdcReq("payload", "artifactName", "artifactLabel", "artifactType");
        JSONAssert.assertEquals("{\"payloadData\" : \"cGF5bG9hZA==\",\"artifactLabel\" : \"artifactLabel\","
            + "\"artifactName\" :\"artifactName\",\"artifactType\" : \"artifactType\","
            + "\"artifactGroupType\" : \"DEPLOYMENT\",\"description\" : \"from CLAMP Cockpit\"}", jsonResult, true);
    }

    @Test
    public void getSdcReqUrlsListTest() throws GeneralSecurityException, DecoderException {
        List<String> listUrls = sdcReq.getSdcReqUrlsList(modelProperties);
        assertNotNull(listUrls);
        assertTrue(listUrls.size() == 1);
        assertTrue(listUrls.get(0).contains(
            "/sdc/v1/catalog/services/56441b4b-0467-41dc-9a0e-e68613838219/resourceInstances/vpacketgen0/artifacts"));
    }
}
