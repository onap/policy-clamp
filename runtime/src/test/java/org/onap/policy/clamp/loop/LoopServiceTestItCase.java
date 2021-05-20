/*-
 * ============LICENSE_START=======================================================
 * ONAP CLAMP
 * ================================================================================
 * Copyright (C) 2019 Nokia Intellectual Property. All rights
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

package org.onap.policy.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.policy.clamp.clds.Application;
import org.onap.policy.clamp.clds.util.JsonUtils;
import org.onap.policy.clamp.loop.log.LogType;
import org.onap.policy.clamp.loop.log.LoopLog;
import org.onap.policy.clamp.loop.log.LoopLogService;
import org.onap.policy.clamp.loop.template.LoopTemplate;
import org.onap.policy.clamp.loop.template.PolicyModel;
import org.onap.policy.clamp.loop.template.PolicyModelsService;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicy;
import org.onap.policy.clamp.policy.microservice.MicroServicePolicyService;
import org.onap.policy.clamp.policy.operational.OperationalPolicy;
import org.onap.policy.clamp.policy.operational.OperationalPolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopServiceTestItCase {

    private static final String EXAMPLE_LOOP_NAME = "ClosedLoopTest";
    private static final String EXAMPLE_JSON = "{\"testName\":\"testValue\"}";

    @Autowired
    LoopService loopService;

    @Autowired
    LoopsRepository loopsRepository;

    @Autowired
    MicroServicePolicyService microServicePolicyService;

    @Autowired
    OperationalPolicyService operationalPolicyService;

    @Autowired
    LoopLogService loopLogService;

    @Autowired
    PolicyModelsService policyModelsService;

    @Test
    @Transactional
    public void shouldCreateEmptyLoop() {
        // given
        String loopBlueprint = "blueprint";
        Loop testLoop = createTestLoop(EXAMPLE_LOOP_NAME, loopBlueprint);
        testLoop.setGlobalPropertiesJson(JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        testLoop.setLastComputedState(LoopState.DESIGN);

        // when
        Loop actualLoop = loopService.saveOrUpdateLoop(testLoop);

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop).isEqualTo(loopsRepository.findById(actualLoop.getName()).get());
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        assertThat(actualLoop.getGlobalPropertiesJson().getAsJsonPrimitive("testName").getAsString())
                .isEqualTo("testValue");
    }

    @Test
    @Transactional
    public void shouldAddOperationalPolicyToLoop() {
        // given
        saveTestLoopToDb();
        OperationalPolicy operationalPolicy = new OperationalPolicy("policyName", null,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null, null);

        // when
        Loop actualLoop = loopService.updateAndSaveOperationalPolicies(EXAMPLE_LOOP_NAME,
                Lists.newArrayList(operationalPolicy));

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> savedPolicies = actualLoop.getOperationalPolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies)
                .usingElementComparatorIgnoringFields("loop", "createdBy", "createdDate", "updatedBy", "updatedDate")
                .contains(operationalPolicy);
        OperationalPolicy savedPolicy = savedPolicies.iterator().next();
        Assertions.assertThat(savedPolicy.getLoop().getName()).isEqualTo(EXAMPLE_LOOP_NAME);

    }

    @Test
    @Transactional
    public void shouldAddMicroservicePolicyToLoop() {
        // given
        saveTestLoopToDb();
        PolicyModel policyModel = new PolicyModel("org.policies.policyModel1",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "policyModel1");
        policyModelsService.saveOrUpdatePolicyModel(policyModel);
        MicroServicePolicy microServicePolicy = new MicroServicePolicy("policyName", policyModel,
                false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);

        // when
        Loop actualLoop = loopService.updateAndSaveMicroservicePolicies(EXAMPLE_LOOP_NAME,
                Lists.newArrayList(microServicePolicy));

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<MicroServicePolicy> savedPolicies = actualLoop.getMicroServicePolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("usedByLoops", "createdDate", "updatedDate",
                "createdBy", "updatedBy").containsExactly(microServicePolicy);
        assertThat(savedPolicies).extracting("usedByLoops").hasSize(1);

    }

    @Test
    @Transactional
    //@Commit
    public void shouldCreateNewMicroservicePolicyAndUpdateJsonRepresentationOfOldOne() {
        // given
        saveTestLoopToDb();
        PolicyModel policyModel1 = new PolicyModel("org.policies.firstPolicyName",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "firstPolicyName");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = new PolicyModel("org.policies.secondPolicyName",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "secondPolicyName");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        MicroServicePolicy firstMicroServicePolicy = new MicroServicePolicy("firstPolicyName", policyModel1, false,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);

        loopService.updateAndSaveMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstMicroServicePolicy));
        MicroServicePolicy secondMicroServicePolicy = new MicroServicePolicy("secondPolicyName", policyModel2, false,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);

        // when
        firstMicroServicePolicy
                .setConfigurationsJson(JsonUtils.GSON.fromJson("{\"name1\":\"value1\"}", JsonObject.class));
        Loop actualLoop = loopService.updateAndSaveMicroservicePolicies(EXAMPLE_LOOP_NAME,
                Lists.newArrayList(firstMicroServicePolicy, secondMicroServicePolicy));

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<MicroServicePolicy> savedPolicies = actualLoop.getMicroServicePolicies();
        assertThat(savedPolicies).hasSize(2);
        assertThat(savedPolicies).contains(firstMicroServicePolicy);
        assertThat(savedPolicies).contains(secondMicroServicePolicy);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("usedByLoops", "createdDate", "updatedDate",
                "createdBy", "updatedBy").containsExactlyInAnyOrder(firstMicroServicePolicy, secondMicroServicePolicy);
    }

    private void saveTestLoopToDb() {
        Loop testLoop = createTestLoop(EXAMPLE_LOOP_NAME, "blueprint");
        testLoop.setGlobalPropertiesJson(JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        LoopTemplate template = new LoopTemplate();
        template.setName("testTemplate");
        testLoop.setLoopTemplate(template);
        loopService.saveOrUpdateLoop(testLoop);
    }

    @Test
    @Transactional
    public void shouldRemoveOldMicroservicePolicyIfNotInUpdatedList() {
        // given
        saveTestLoopToDb();
        PolicyModel policyModel1 = new PolicyModel("org.policies.firstPolicyName",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "firstPolicyName");
        policyModelsService.saveOrUpdatePolicyModel(policyModel1);
        PolicyModel policyModel2 = new PolicyModel("org.policies.secondPolicyName",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "secondPolicyName");
        policyModelsService.saveOrUpdatePolicyModel(policyModel2);
        MicroServicePolicy firstMicroServicePolicy = new MicroServicePolicy("firstPolicyName", policyModel1,
                false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);
        loopService.updateAndSaveMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstMicroServicePolicy));

        MicroServicePolicy secondMicroServicePolicy = new MicroServicePolicy("secondPolicyName", policyModel2,
                false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);

        // when
        Loop actualLoop = loopService.updateAndSaveMicroservicePolicies(EXAMPLE_LOOP_NAME,
                Lists.newArrayList(secondMicroServicePolicy));

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<MicroServicePolicy> savedPolicies = actualLoop.getMicroServicePolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("usedByLoops", "createdDate", "updatedDate",
                "createdBy", "updatedBy").containsExactly(secondMicroServicePolicy);

    }

    @Test
    @Transactional
    public void shouldCreateNewOperationalPolicyAndUpdateJsonRepresentationOfOldOne() {
        // given
        saveTestLoopToDb();

        JsonObject newJsonConfiguration = JsonUtils.GSON.fromJson("{}", JsonObject.class);

        OperationalPolicy firstOperationalPolicy = new OperationalPolicy("firstPolicyName", null,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null, null);
        loopService.updateAndSaveOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstOperationalPolicy));

        OperationalPolicy secondOperationalPolicy = new OperationalPolicy("secondPolicyName", null,
                newJsonConfiguration, null, null, null, null);

        // when
        firstOperationalPolicy.setConfigurationsJson(newJsonConfiguration);
        Loop actualLoop = loopService.updateAndSaveOperationalPolicies(EXAMPLE_LOOP_NAME,
                Lists.newArrayList(firstOperationalPolicy, secondOperationalPolicy));

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> savedPolicies = actualLoop.getOperationalPolicies();
        assertThat(savedPolicies).hasSize(2);
        assertThat(savedPolicies)
                .usingElementComparatorIgnoringFields("loop", "createdDate", "updatedDate", "createdBy", "updatedBy")
                .containsExactlyInAnyOrder(firstOperationalPolicy, secondOperationalPolicy);
        Set<String> policiesLoops = Lists.newArrayList(savedPolicies).stream().map(OperationalPolicy::getLoop)
                .map(Loop::getName).collect(Collectors.toSet());
        assertThat(policiesLoops).containsExactly(EXAMPLE_LOOP_NAME);
    }

    @Test
    @Transactional
    public void shouldRemoveOldOperationalPolicyIfNotInUpdatedList() {
        // given
        saveTestLoopToDb();

        OperationalPolicy firstOperationalPolicy = new OperationalPolicy("firstPolicyName", null,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null, null);
        loopService.updateAndSaveOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstOperationalPolicy));

        OperationalPolicy secondOperationalPolicy = new OperationalPolicy("policyName", null,
                JsonUtils.GSON.fromJson("{}", JsonObject.class), null, null, "pdpGroup1", "pdpSubgroup1");

        // when
        Loop actualLoop = loopService.updateAndSaveOperationalPolicies(EXAMPLE_LOOP_NAME,
                Lists.newArrayList(secondOperationalPolicy));

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> savedPolicies = actualLoop.getOperationalPolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies)
                .usingElementComparatorIgnoringFields("loop", "createdDate", "updatedDate", "createdBy", "updatedBy")
                .containsExactly(secondOperationalPolicy);
        OperationalPolicy savedPolicy = savedPolicies.iterator().next();
        Assertions.assertThat(savedPolicy.getLoop().getName()).isEqualTo(EXAMPLE_LOOP_NAME);

    }

    @Test
    @Transactional
    public void shouldCreateModelPropertiesAndUpdateJsonRepresentationOfOldOne() {
        // given
        saveTestLoopToDb();
        String expectedJson = "{\"test\":\"test\"}";
        JsonObject baseGlobalProperites = JsonUtils.GSON.fromJson("{}", JsonObject.class);
        JsonObject updatedGlobalProperites = JsonUtils.GSON.fromJson(expectedJson, JsonObject.class);
        loopService.updateAndSaveGlobalPropertiesJson(EXAMPLE_LOOP_NAME, baseGlobalProperites);

        // when
        Loop actualLoop = loopService.updateAndSaveGlobalPropertiesJson(EXAMPLE_LOOP_NAME, updatedGlobalProperites);

        // then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        JsonObject returnedGlobalProperties = actualLoop.getGlobalPropertiesJson();
        assertThat(returnedGlobalProperties.getAsJsonObject()).isEqualTo(updatedGlobalProperites);
    }

    @Test
    @Transactional
    public void deleteAttempt() {
        saveTestLoopToDb();
        // Add log
        Loop loop = loopsRepository.findById(EXAMPLE_LOOP_NAME).orElse(null);
        loop.addLog(new LoopLog("test", LogType.INFO, "CLAMP", loop));
        LoopTemplate template = new LoopTemplate();
        template.setName("testTemplate");
        loop.setLoopTemplate(template);
        loop = loopService.saveOrUpdateLoop(loop);
        // Add op policy
        OperationalPolicy operationalPolicy = new OperationalPolicy("opPolicy", null,
                JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null, null);
        loopService.updateAndSaveOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(operationalPolicy));

        PolicyModel policyModel = new PolicyModel("org.policies.microPolicy",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "microPolicy");
        policyModelsService.saveOrUpdatePolicyModel(policyModel);
        // Add Micro service policy
        MicroServicePolicy microServicePolicy = new MicroServicePolicy("microPolicy", policyModel,
                false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);
        loopService.updateAndSaveMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(microServicePolicy));

        // Verify it's there
        assertThat(loopsRepository.findById(EXAMPLE_LOOP_NAME).orElse(null)).isNotNull();
        loopService.deleteLoop(EXAMPLE_LOOP_NAME);
        // Verify it's well deleted and has been cascaded, except for Microservice
        assertThat(loopsRepository.findById(EXAMPLE_LOOP_NAME).orElse(null)).isNull();
        assertThat(microServicePolicyService.isExisting("microPolicy")).isTrue();
        assertThat(operationalPolicyService.isExisting("opPolicy")).isFalse();
        assertThat(loopLogService.isExisting(((LoopLog) loop.getLoopLogs().toArray()[0]).getId())).isFalse();
    }

    @Test
    @Transactional
    public void testUpdateLoopState() {
        saveTestLoopToDb();
        Loop loop = loopService.getLoop(EXAMPLE_LOOP_NAME);
        loopService.updateLoopState(loop, "SUBMITTED");
        Loop updatedLoop = loopService.getLoop(EXAMPLE_LOOP_NAME);
        assertThat(updatedLoop.getLastComputedState()).isEqualTo(LoopState.SUBMITTED);
    }

    @Test
    @Transactional
    public void testUpdateDcaeDeploymentFields() {
        saveTestLoopToDb();
        Loop loop = loopService.getLoop(EXAMPLE_LOOP_NAME);
        loopService.updateDcaeDeploymentFields(loop, "CLAMP_c5ce429a-f570-48c5-a7ea-53bed8f86f85",
                "https://deployment-handler.onap:8443");
        loop = loopService.getLoop(EXAMPLE_LOOP_NAME);
        assertThat(loop.getDcaeDeploymentId()).isEqualTo("CLAMP_c5ce429a-f570-48c5-a7ea-53bed8f86f85");
        assertThat(loop.getDcaeDeploymentStatusUrl()).isEqualTo("https://deployment-handler.onap:8443");
    }

    @Test
    @Transactional
    public void testUpdateMicroservicePolicy() {
        saveTestLoopToDb();
        assertThat(microServicePolicyService.isExisting("policyName")).isFalse();
        PolicyModel policyModel = new PolicyModel("org.policies.policyName",
                "tosca_definitions_version: tosca_simple_yaml_1_0_0", "1.0.0", "policyName");
        policyModelsService.saveOrUpdatePolicyModel(policyModel);
        MicroServicePolicy microServicePolicy = new MicroServicePolicy("policyName", policyModel,
                false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null, null, null);
        loopService.updateMicroservicePolicy(EXAMPLE_LOOP_NAME, microServicePolicy);
        assertThat(microServicePolicyService.isExisting("policyName")).isTrue();
    }

    private Loop createTestLoop(String loopName, String loopBlueprint) {
        return new Loop(loopName);
    }
}