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
import org.onap.clamp.clds.model.CldsEvent;
import org.onap.clamp.clds.model.prop.ModelProperties;
import org.openecomp.policy.api.AttributeType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;
import org.onap.clamp.clds.client.req.OperationalPolicyReq;
import org.onap.clamp.clds.client.req.StringMatchPolicyReq;
import org.onap.clamp.clds.model.prop.Policy;
import org.onap.clamp.clds.model.prop.StringMatch;
import org.onap.clamp.clds.transform.TransformUtil;

/**
 * Test Policy API in org.onap.clamp.ClampDesigner.client package - replicate
 * Policy Delegates in tests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class PolicyClientIT extends AbstractIT {
    String modelProp;
    String modelBpmnProp;
    String modelName;
    String controlName;

    @Before
    public void setUp() throws IOException {
        modelProp = TransformUtil.getResourceAsString("example/modelProp.json");
        modelBpmnProp = TransformUtil.getResourceAsString("example/modelBpmnProp.json");
        modelName = "example-model06";
        controlName = "ClosedLoop_FRWL_SIG_fad4dcae_e498_11e6_852e_0050568c4ccf";
    }

    private void createUpdateStringMatch(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, modelBpmnProp, modelProp);
        String stringMatchPolicyRequestUuid = UUID.randomUUID().toString();
        String policyJson = StringMatchPolicyReq.format(refProp, prop);
        System.out.println("String Match policyJson=" + policyJson);
        String responseMessage = policyClient.sendMicroService(policyJson, prop, stringMatchPolicyRequestUuid);
        System.out.println(responseMessage);
    }

    private void createUpdateOperationalPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, modelBpmnProp, modelProp);
        String operationalPolicyRequestUuid = UUID.randomUUID().toString();
        Map<AttributeType, Map<String, String>> attributes = OperationalPolicyReq.formatAttributes(refProp, prop);
        String responseMessage = policyClient.sendBrms(attributes, prop, operationalPolicyRequestUuid);
        System.out.println(responseMessage);
    }

    private void createUpdatePolicies(String actionCd) throws Exception {
        createUpdateStringMatch(actionCd);
        createUpdateOperationalPolicy(actionCd);
    }

    private void deleteStringMatchPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, modelBpmnProp, modelProp);
        StringMatch stringMatch = prop.getStringMatch();
        prop.setCurrentModelElementId(stringMatch.getId());
        String responseMessage = policyClient.deleteMicrosService(prop);
        System.out.println(responseMessage);
    }

    private void deleteOperationalPolicy(String actionCd) throws Exception {
        ModelProperties prop = new ModelProperties(modelName, controlName, actionCd, modelBpmnProp, modelProp);
        Policy policy = prop.getPolicy();
        prop.setCurrentModelElementId(policy.getId());
        String responseMessage = policyClient.deleteBrms(prop);
        System.out.println(responseMessage);
    }

    private void deletePolicies(String actionCd) throws Exception {
        deleteStringMatchPolicy(actionCd);
        deleteOperationalPolicy(actionCd);
    }

    /**
     * Delete policies so we can start with a clean state. But this is just a
     * precaution - the policies might not already exists. So ignore errors in
     * attempting to do this.
     * 
     * @param actionCd
     */
    private void cleanUpPolicies(String actionCd) {
        try {
            deleteStringMatchPolicy(actionCd);
        } catch (Exception e) {
            System.err.println(
                    "TestPolicyClient: The following error is ok - attempting delete in case the policy exists - the goal is to start with clean slate");
        }
        try {
            deleteOperationalPolicy(actionCd);
        } catch (Exception e) {
            System.err.println(
                    "TestPolicyClient: The following error is ok - attempting delete in case the policy exists - the goal is to start with clean slate");
        }
    }

    @Test
    public void testCreateUpdateDeletePolicy() throws Exception {

        cleanUpPolicies(CldsEvent.ACTION_DELETE);
        TimeUnit.SECONDS.sleep(5);
        System.out.println("entered into update");
        String actionCd;

        try {
            actionCd = CldsEvent.ACTION_SUBMIT;
            createUpdatePolicies(actionCd);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Policy send failed: PE500 "));
        }

        try {
            actionCd = CldsEvent.ACTION_RESUBMIT;
            createUpdatePolicies(actionCd);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Policy send failed: PE500 "));
        }

        try {
            actionCd = CldsEvent.ACTION_RESUBMIT;
            createUpdatePolicies(actionCd);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Policy send failed: PE500 "));
        }

        try {
            TimeUnit.SECONDS.sleep(20);
            deletePolicies(CldsEvent.ACTION_DELETE);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unable to get valid Response from  PDP"));
        }

    }
}
