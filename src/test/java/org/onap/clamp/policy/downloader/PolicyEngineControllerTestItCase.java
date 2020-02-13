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

import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import javax.transaction.Transactional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.loop.template.PolicyModel;
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

    @Test
    @Transactional
    public void synchronizeAllPoliciesTest() throws JsonSyntaxException, IOException, InterruptedException {
        policyController.synchronizeAllPolicies();
        Instant firstExecution = policyController.getLastInstantExecuted();
        assertThat (firstExecution).isNotNull();
        List<PolicyModel> policyModelsList = policyModelsRepository.findAll();
        assertThat(policyModelsList.size()).isGreaterThanOrEqualTo(8);
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.Monitoring", null, "1.0.0"));
        assertThat(policyModelsList).contains(new PolicyModel("onap.policies.controlloop.Operational", null, "1.0.0"));

        // Re-do it to check that there is no issue with duplicate key
        policyController.synchronizeAllPolicies();
        Instant secondExecution = policyController.getLastInstantExecuted();
        assertThat (secondExecution).isNotNull();

        assertThat(firstExecution).isBefore(secondExecution);
    }

}
