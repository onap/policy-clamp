/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;

class ControlLoopProviderTest {

    private static final String LIST_IS_NULL = "controlLoops is marked .*ull but is null";
    private static final Coder CODER = new StandardCoder();
    private static final String CONTROL_LOOP_JSON = "src/test/resources/providers/TestControlLoops.json";
    private static final String UPDATE_CL_JSON = "src/test/resources/providers/UpdateControlLoops.json";
    private static final String TOSCA_TEMPLATE_YAML = "examples/controlloop/PMSubscriptionHandling.yaml";

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static AtomicInteger dbNameCounter = new AtomicInteger();

    private PolicyModelsProviderParameters parameters;
    private ControlLoopProvider controlLoopProvider;
    private ControlLoops inputControlLoops;
    private ControlLoops updateControlLoops;
    private String originalJson = ResourceUtils.getResourceAsString(CONTROL_LOOP_JSON);
    private String updateClJson = ResourceUtils.getResourceAsString(UPDATE_CL_JSON);

    @BeforeEach
    void beforeSetupDao() throws Exception {

        parameters = new PolicyModelsProviderParameters();
        parameters.setDatabaseDriver("org.h2.Driver");
        parameters.setName("PolicyProviderParameterGroup");
        parameters.setImplementation("org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
        parameters.setDatabaseUrl("jdbc:h2:mem:controlLoopProviderTestDb" + dbNameCounter.getAndDecrement());
        parameters.setDatabaseUser("policy");
        parameters.setDatabasePassword("P01icY");
        parameters.setPersistenceUnit("ToscaConceptTest");

        controlLoopProvider = new ControlLoopProvider(parameters);

        inputControlLoops = CODER.decode(originalJson, ControlLoops.class);
        updateControlLoops = CODER.decode(updateClJson, ControlLoops.class);
    }

    @AfterEach
    void teardown() {
        controlLoopProvider.close();
    }

    @Test
    void testControlLoopCreate() throws Exception {
        assertThatThrownBy(() -> {
            controlLoopProvider.createControlLoops(null);
        }).hasMessageMatching(LIST_IS_NULL);

        var createdControlLoops = new ControlLoops();
        createdControlLoops
            .setControlLoopList(controlLoopProvider.createControlLoops(inputControlLoops.getControlLoopList()));

        assertEquals(inputControlLoops, createdControlLoops);
    }

    @Test
    void testGetControlLoops() throws Exception {

        List<ControlLoop> getResponse;

        // Return empty list when no data present in db
        getResponse = controlLoopProvider.getControlLoops(null, null);
        assertThat(getResponse).isEmpty();

        controlLoopProvider.createControlLoops(inputControlLoops.getControlLoopList());
        var name = inputControlLoops.getControlLoopList().get(0).getName();
        var version = inputControlLoops.getControlLoopList().get(0).getVersion();
        assertEquals(1, controlLoopProvider.getControlLoops(name, version).size());

        var cl = new ControlLoop();
        cl = controlLoopProvider.getControlLoop(new ToscaConceptIdentifier("PMSHInstance1", "1.0.1"));
        assertEquals(inputControlLoops.getControlLoopList().get(1), cl);

        assertNull(controlLoopProvider.getControlLoop(new ToscaConceptIdentifier("invalid_name", "1.0.1")));

        assertThatThrownBy(() -> {
            controlLoopProvider.getFilteredControlLoops(null);
        }).hasMessageMatching("filter is marked .*ull but is null");

        final ToscaTypedEntityFilter<ControlLoop> filter = ToscaTypedEntityFilter.<ControlLoop>builder()
            .type("org.onap.domain.pmsh.PMSHControlLoopDefinition").build();
        assertEquals(2, controlLoopProvider.getFilteredControlLoops(filter).size());
    }

    @Test
    void testUpdateControlLoops() throws Exception {
        assertThatThrownBy(() -> {
            controlLoopProvider.updateControlLoops(null);
        }).hasMessageMatching("controlLoops is marked .*ull but is null");

        var existingControlLoops = new ControlLoops();
        existingControlLoops
            .setControlLoopList(controlLoopProvider.createControlLoops(inputControlLoops.getControlLoopList()));
        var updateResponse = new ControlLoop();
        updateResponse = controlLoopProvider.updateControlLoop(updateControlLoops.getControlLoopList().get(0));

        assertEquals(ControlLoopOrderedState.RUNNING, updateResponse.getOrderedState());
    }

    @Test
    void testDeleteControlLoop() throws Exception {
        assertThatThrownBy(() -> {
            controlLoopProvider.deleteControlLoop("Invalid_name", "1.0.1");
        }).hasMessageMatching(".*.failed, control loop does not exist");

        ControlLoop deletedCl;
        List<ControlLoop> clList = controlLoopProvider.createControlLoops(inputControlLoops.getControlLoopList());
        var name = inputControlLoops.getControlLoopList().get(0).getName();
        var version = inputControlLoops.getControlLoopList().get(0).getVersion();

        deletedCl = controlLoopProvider.deleteControlLoop(name, version);
        assertEquals(clList.get(0), deletedCl);
    }

    @Test
    void testDeleteAllInstanceProperties() throws Exception {
        var toscaServiceTemplate = testControlLoopRead();
        controlLoopProvider.deleteInstanceProperties(
                controlLoopProvider.saveInstanceProperties(toscaServiceTemplate),
                controlLoopProvider.getNodeTemplates(null, null));
        assertThat(controlLoopProvider.getControlLoops(null, null)).isEmpty();
    }

    @Test
    void testSaveAndDeleteInstanceProperties() throws Exception {
        var toscaServiceTest = testControlLoopRead();
        controlLoopProvider.createControlLoops(inputControlLoops.getControlLoopList());

        controlLoopProvider.saveInstanceProperties(toscaServiceTest);
        assertThat(controlLoopProvider.getNodeTemplates(
                "org.onap.policy.controlloop.PolicyControlLoopParticipant",
                "2.3.1")).isNotEmpty();

        controlLoopProvider.deleteInstanceProperties(
                controlLoopProvider.saveInstanceProperties(toscaServiceTest),
                controlLoopProvider.getNodeTemplates(
                        "org.onap.policy.controlloop.PolicyControlLoopParticipant",
                        "2.3.1"));

        assertThat(controlLoopProvider.getNodeTemplates(
                "org.onap.policy.controlloop.PolicyControlLoopParticipant",
                "2.3.1")).isEmpty();
    }

    @Test
    void testGetNodeTemplates() throws Exception {
        //Getting all nodes
        List<ToscaNodeTemplate> listNodes = controlLoopProvider.getNodeTemplates(null, null);
        assertNotNull(listNodes);

        assertThatThrownBy(() -> {
            controlLoopProvider.getFilteredNodeTemplates(null);
        }).hasMessageMatching("filter is marked non-null but is null");
    }

    private static ToscaServiceTemplate testControlLoopRead() {
        return testControlLoopYamlSerialization(TOSCA_TEMPLATE_YAML);
    }

    private static ToscaServiceTemplate testControlLoopYamlSerialization(String controlLoopFilePath) {
        String controlLoopString = ResourceUtils.getResourceAsString(controlLoopFilePath);
        ToscaServiceTemplate serviceTemplate = yamlTranslator.fromYaml(controlLoopString, ToscaServiceTemplate.class);
        return serviceTemplate;
    }
}
