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


package org.onap.clamp.loop;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import java.util.Set;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onap.clamp.clds.Application;
import org.onap.clamp.clds.util.JsonUtils;
import org.onap.clamp.policy.microservice.MicroServicePolicy;
import org.onap.clamp.policy.operational.OperationalPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = Application.class)
public class LoopServiceTestItCase {

    private static final String EXAMPLE_LOOP_NAME = "ClosedLoopTest";
    private static final String EXAMPLE_JSON = "{\"testName\":\"testValue\"}";

    @Autowired
    LoopService loopService;

    @Autowired
    LoopsRepository loopsRepository;

    @After
    public void tearDown() {
        loopsRepository.deleteAll();
    }

    @Test
    public void shouldCreateEmptyLoop() {
        //given
        String loopBlueprint = "blueprint";
        String loopSvg = "representation";
        Loop testLoop = createTestLoop(EXAMPLE_LOOP_NAME, loopBlueprint, loopSvg);
        testLoop.setGlobalPropertiesJson(JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        testLoop.setLastComputedState(LoopState.DESIGN);

        //when
        Loop actualLoop = loopService.addNewLoop(testLoop);

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop).isEqualTo(loopsRepository.findById(actualLoop.getName()).get());
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        assertThat(actualLoop.getBlueprint()).isEqualTo(loopBlueprint);
        assertThat(actualLoop.getSvgRepresentation()).isEqualTo(loopSvg);
        assertThat(actualLoop.getGlobalPropertiesJson().getAsJsonPrimitive("testName").getAsString())
            .isEqualTo("testValue");
    }

    @Test
    public void shouldAddOperationalPolicyToLoop() {
        //given
        saveTestLoopToDb();
        JsonObject confJson = JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class);
        String policyName = "policyName";
        OperationalPolicy operationalPolicy = new OperationalPolicy(policyName, null, confJson);

        //when
        Loop actualLoop = loopService
            .updateOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(operationalPolicy));

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> savedPolicies = actualLoop.getOperationalPolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies)
            .usingElementComparatorIgnoringFields("loop")
            .contains(operationalPolicy);
        OperationalPolicy savedPolicy = savedPolicies.iterator().next();
        assertThat(savedPolicy.getLoop().getName()).isEqualTo(EXAMPLE_LOOP_NAME);

    }

    @Test
    public void shouldAddMicroservicePolicyToLoop() {
        //given
        saveTestLoopToDb();
        JsonObject confJson = JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class);
        String policyName = "policyName";
        String policyTosca = "policyTosca";
        MicroServicePolicy microServicePolicy = new MicroServicePolicy(policyName, policyTosca, false, confJson, null);

        //when
        Loop actualLoop = loopService
            .updateMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(microServicePolicy));

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<MicroServicePolicy> savedPolicies = actualLoop.getMicroServicePolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("usedByLoops")
            .containsExactly(microServicePolicy);
        assertThat(savedPolicies).extracting("usedByLoops")
            .hasSize(1);

    }

    @Test
    @Transactional
    public void shouldCreateNewMicroservicePolicyAndUpdateJsonRepresentationOfOldOne() {
        //given
        saveTestLoopToDb();
        String firstPolicyName = "firstPolicyName";
        JsonObject newJsonRepresentation = JsonUtils.GSON.fromJson("{}", JsonObject.class);
        String secondPolicyName = "secondPolicyName";
        String secondPolicyTosca = "secondPolicyTosca";
        MicroServicePolicy firstMicroServicePolicy = new MicroServicePolicy(firstPolicyName, "policyTosca",
            false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null);
        loopService.updateMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstMicroServicePolicy));

        MicroServicePolicy secondMicroServicePolicy = new MicroServicePolicy(secondPolicyName, secondPolicyTosca, true,
            newJsonRepresentation, null);

        //when
        firstMicroServicePolicy.setJsonRepresentation(newJsonRepresentation);
        Loop actualLoop = loopService.updateMicroservicePolicies(EXAMPLE_LOOP_NAME,
            Lists.newArrayList(firstMicroServicePolicy, secondMicroServicePolicy));

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<MicroServicePolicy> savedPolicies = actualLoop.getMicroServicePolicies();
        assertThat(savedPolicies).hasSize(2);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("usedByLoops")
            .containsExactlyInAnyOrder(firstMicroServicePolicy, secondMicroServicePolicy);

    }

    private void saveTestLoopToDb() {
        Loop testLoop = createTestLoop(EXAMPLE_LOOP_NAME, "blueprint", "representation");
        testLoop.setGlobalPropertiesJson(JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        loopService.addNewLoop(testLoop);
    }

    @Test
    public void shouldRemoveOldMicroservicePolicyIfNotInUpdatedList() {
        //given
        saveTestLoopToDb();

        JsonObject jsonRepresentation = JsonUtils.GSON.fromJson("{}", JsonObject.class);
        String firstPolicyName = "firstPolicyName";
        String secondPolicyName = "policyName";
        String secondPolicyTosca = "secondPolicyTosca";
        MicroServicePolicy firstMicroServicePolicy = new MicroServicePolicy(firstPolicyName, "policyTosca",
            false, JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class), null);
        loopService.updateMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstMicroServicePolicy));

        MicroServicePolicy secondMicroServicePolicy = new MicroServicePolicy(secondPolicyName, secondPolicyTosca, true,
            jsonRepresentation, null);

        //when
        Loop actualLoop = loopService
            .updateMicroservicePolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(secondMicroServicePolicy));

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<MicroServicePolicy> savedPolicies = actualLoop.getMicroServicePolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("usedByLoops")
            .containsExactly(secondMicroServicePolicy);

    }

    @Test
    @Transactional
    public void shouldCreateNewOperationalPolicyAndUpdateJsonRepresentationOfOldOne() {
        //given
        saveTestLoopToDb();

        String firstPolicyName = "firstPolicyName";
        JsonObject newJsonConfiguration = JsonUtils.GSON.fromJson("{}", JsonObject.class);
        String secondPolicyName = "secondPolicyName";
        OperationalPolicy firstOperationalPolicy = new OperationalPolicy(firstPolicyName, null,
            JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        loopService.updateOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstOperationalPolicy));

        OperationalPolicy secondOperationalPolicy = new OperationalPolicy(secondPolicyName, null, newJsonConfiguration);

        //when
        firstOperationalPolicy.setConfigurationsJson(newJsonConfiguration);
        Loop actualLoop = loopService.updateOperationalPolicies(EXAMPLE_LOOP_NAME,
            Lists.newArrayList(firstOperationalPolicy, secondOperationalPolicy));

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> savedPolicies = actualLoop.getOperationalPolicies();
        assertThat(savedPolicies).hasSize(2);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("loop")
            .containsExactlyInAnyOrder(firstOperationalPolicy, secondOperationalPolicy);
        Set<String> policiesLoops = Lists.newArrayList(savedPolicies).stream()
            .map(OperationalPolicy::getLoop)
            .map(Loop::getName)
            .collect(Collectors.toSet());
        assertThat(policiesLoops)
            .containsExactly(EXAMPLE_LOOP_NAME);
    }

    @Test
    public void shouldRemoveOldOperationalPolicyIfNotInUpdatedList() {
        //given
        saveTestLoopToDb();

        JsonObject jsonRepresentation = JsonUtils.GSON.fromJson("{}", JsonObject.class);
        String firstPolicyName = "firstPolicyName";
        String secondPolicyName = "policyName";
        OperationalPolicy firstOperationalPolicy = new OperationalPolicy(firstPolicyName, null,
            JsonUtils.GSON.fromJson(EXAMPLE_JSON, JsonObject.class));
        loopService.updateOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(firstOperationalPolicy));

        OperationalPolicy secondOperationalPolicy = new OperationalPolicy(secondPolicyName, null, jsonRepresentation);

        //when
        Loop actualLoop = loopService
            .updateOperationalPolicies(EXAMPLE_LOOP_NAME, Lists.newArrayList(secondOperationalPolicy));

        //then
        assertThat(actualLoop).isNotNull();
        assertThat(actualLoop.getName()).isEqualTo(EXAMPLE_LOOP_NAME);
        Set<OperationalPolicy> savedPolicies = actualLoop.getOperationalPolicies();
        assertThat(savedPolicies).hasSize(1);
        assertThat(savedPolicies).usingElementComparatorIgnoringFields("loop")
            .containsExactly(secondOperationalPolicy);
        OperationalPolicy savedPolicy = savedPolicies.iterator().next();
        assertThat(savedPolicy.getLoop().getName()).isEqualTo(EXAMPLE_LOOP_NAME);

    }

    private Loop createTestLoop(String loopName, String loopBlueprint, String loopSvg) {
        return new Loop(loopName, loopBlueprint, loopSvg);
    }
}