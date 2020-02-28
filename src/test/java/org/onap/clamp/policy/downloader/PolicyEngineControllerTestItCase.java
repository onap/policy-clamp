package org.onap.clamp.policy.downloader;
/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2020 AT&T Intellectual Property. All rights
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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import javax.transaction.Transactional;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.loop.template.PolicyModel;
import org.onap.clamp.loop.template.PolicyModelId;
import org.onap.clamp.loop.template.PolicyModelsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@ActiveProfiles(profiles = "clamp-default,clamp-policy-controller")
public class PolicyEngineControllerTestItCase {

    @Autowired
    PolicyEngineController policyController;

    @Autowired
    PolicyModelsRepository policyModelsRepository;

    /**
     * This method tests a fake synchronization with the emulator.
     *
     * @throws JsonSyntaxException  In case of issues
     * @throws IOException          In case of issues
     * @throws InterruptedException In case of issues
     */
    @Test
    @Transactional
    public void synchronizeAllPoliciesTest() throws JsonSyntaxException, IOException, InterruptedException {
        policyController.synchronizeAllPolicies();
        Instant firstExecution = policyController.getLastInstantExecuted();
        assertThat(firstExecution).isNotNull();
        List<PolicyModel> policyModelsList = policyModelsRepository.findAll();
        assertThat(policyModelsList.size()).isGreaterThanOrEqualTo(5);
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.controlloop.operational.Drools", null, "1.0.0"));
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.controlloop.operational.Apex", null, "1.0.0"));
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.controlloop.guard.FrequencyLimiter", null, "1.0.0"));
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.controlloop.guard.Blacklist", null, "1.0.0"));
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.controlloop.guard.MinMax", null, "2.0.0"));

        // Re-do it to check that there is no issue with duplicate key
        policyController.synchronizeAllPolicies();
        Instant secondExecution = policyController.getLastInstantExecuted();
        assertThat(secondExecution).isNotNull();

        assertThat(firstExecution).isBefore(secondExecution);
    }

    @Test
    @Transactional
    public void downloadPdpGroupsTest() throws JsonSyntaxException, IOException, InterruptedException, ParseException {
        PolicyModel policyModel1 = new PolicyModel("onap.policies.monitoring.test", null, "1.0.0");
        policyModelsRepository.saveAndFlush(policyModel1);
        PolicyModel policyModel2 = new PolicyModel("onap.policies.controlloop.Operational", null, "1.0.0");
        policyModelsRepository.saveAndFlush(policyModel2);

        policyController.downloadPdpGroups();

        List<PolicyModel> policyModelsList = policyModelsRepository.findAll();
        assertThat(policyModelsList.size()).isGreaterThanOrEqualTo(2);

        PolicyModel policy1 = policyModelsRepository
                .getOne(new PolicyModelId("onap.policies.monitoring.test", "1.0.0"));
        PolicyModel policy2 = policyModelsRepository
                .getOne(new PolicyModelId("onap.policies.controlloop.Operational", "1.0.0"));

        String expectedRes1 = "{\"supportedPdpGroups\":[{\"monitoring\":[\"xacml\"]}]}";
        JsonObject expectedJson1 = JsonUtils.GSON.fromJson(expectedRes1, JsonObject.class);
        assertThat(policy1.getPolicyPdpGroup()).isEqualTo(expectedJson1);
        String expectedRes2 = "{\"supportedPdpGroups\":[{\"controlloop\":[\"apex\",\"drools\"]}]}";
        JsonObject expectedJson2 = JsonUtils.GSON.fromJson(expectedRes2, JsonObject.class);
        assertThat(policy2.getPolicyPdpGroup()).isEqualTo(expectedJson2);

    }
}
