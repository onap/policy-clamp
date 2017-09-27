/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
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
 * ECOMP is a trademark and service mark of AT&T Intellectual Property.
 */

package org.onap.clamp.clds.it;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.AbstractIT;
import org.onap.clamp.clds.client.req.OperationalPolicyReq;
import org.onap.clamp.clds.client.req.TcaMPolicyReq;
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.onap.clamp.clds.model.prop.Policy;
import org.onap.clamp.clds.model.prop.PolicyChain;
import org.onap.clamp.clds.model.prop.Tca;
import org.onap.clamp.clds.util.ResourceFileUtil;
import org.onap.policy.api.AttributeType;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Test Policy API in org.onap.clamp.ClampDesigner.client package - replicate
 * Policy Delegates in tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:application-no-camunda.properties")
public class PolicyClientIT extends AbstractIT {
    String modelProp;
    String modelBpmnProp;
    String modelName;
    String controlName;

    /**
     * Initialize Test.
     */
    @Before
    public void setUp() throws IOException {
        modelProp = ResourceFileUtil.getResourceAsString("example/modelProp.json");
        modelBpmnProp = ResourceFileUtil.getResourceAsString("example/modelBpmnProp.json");
        modelName = "example-model06";
        controlName = "ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf";
    }

    private void createUpdateOperationalPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, false, modelBpmnProp, modelProp);
        Policy policy = prop.getType(Policy.class);
        if (policy.isFound()) {
            for (PolicyChain policyChain : policy.getPolicyChains()) {
                String operationalPolicyRequestUuid = UUID.randomUUID().toString();

                Map<AttributeType, Map<String, String>> attributes = OperationalPolicyReq.formatAttributes(refProp,
                        prop, policy.getId(), policyChain);
                String responseMessage = policyClient.sendBrmsPolicy(attributes, prop, operationalPolicyRequestUuid);
            }
        }
    }

    private void createUpdateTcaPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, false, modelBpmnProp, modelProp);
        Tca tca = prop.getType(Tca.class);
        if (tca.isFound()) {
            String tcaPolicyRequestUuid = UUID.randomUUID().toString();
            String policyJson = TcaMPolicyReq.formatTca(refProp, prop);
            String correctValue = ResourceFileUtil.getResourceAsString("expected/tca.json");
            JSONAssert.assertEquals(policyJson, correctValue, true);
            String responseMessage = "";
            try {
                responseMessage = policyClient.sendMicroServiceInJson(policyJson, prop, tcaPolicyRequestUuid);
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Exception while communicating with Policy"));
            }
        }
    }

    private void deleteOperationalPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, false, modelBpmnProp, modelProp);

        Policy policy = prop.getType(Policy.class);
        if (policy.isFound()) {
            prop.setCurrentModelElementId(policy.getId());
            for (PolicyChain policyChain : policy.getPolicyChains()) {
                prop.setPolicyUniqueId(policyChain.getPolicyId());
                String responseMessage = policyClient.deleteBrms(prop);
            }
        }
    }

    private void deleteTcaPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, false, modelBpmnProp, modelProp);

        Tca tca = prop.getType(Tca.class);
        if (tca.isFound()) {
            prop.setCurrentModelElementId(tca.getId());
            String responseMessage = "";
            try {
                responseMessage = policyClient.deleteMicrosService(prop);
            } catch (Exception e) {
                assertTrue(e.getMessage().contains("Policy delete failed: PE500 "));
            }
        }
    }

    // @Test
    /**
     * Temporarily disabled Test.
     */
    public void testCreateUpdateDeleteOperationalPolicy() throws Exception {

        createUpdateOperationalPolicy(CldsEvent.ACTION_SUBMIT);

        TimeUnit.SECONDS.sleep(20);

        deleteOperationalPolicy(CldsEvent.ACTION_DELETE);
    }

    @Test
    public void testCreateUpdateDeleteTcaPolicy() throws Exception {

        createUpdateTcaPolicy(CldsEvent.ACTION_SUBMIT);

        TimeUnit.SECONDS.sleep(20);

        deleteTcaPolicy(CldsEvent.ACTION_DELETE);
    }
}
