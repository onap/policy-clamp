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
import static org.junit.Assert.assertNull;

import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopOrderedState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTypedEntityFilter;

public class ControlLoopProviderTest {

    private static final String LIST_IS_NULL = "controlLoops is marked .*ull but is null";
    private static final Coder CODER = new StandardCoder();
    private static final String CONTROL_LOOP_JSON = "src/test/resources/providers/TestControlLoops.json";
    private static final String UPDATE_CL_JSON = "src/test/resources/providers/UpdateControlLoops.json";

    private PolicyModelsProviderParameters parameters;
    private ControlLoopProvider controlLoopProvider;
    private ControlLoops inputControlLoops;
    private ControlLoops updateControlLoops;
    private String originalJson = ResourceUtils.getResourceAsString(CONTROL_LOOP_JSON);
    private String updateClJson = ResourceUtils.getResourceAsString(UPDATE_CL_JSON);


    /**
     * Set up test control loop provider.
     */
    @Before
    public void setupDao() throws Exception {

        parameters = new PolicyModelsProviderParameters();
        parameters.setDatabaseDriver("org.h2.Driver");
        parameters.setName("PolicyProviderParameterGroup");
        parameters.setImplementation("org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
        parameters.setDatabaseUrl("jdbc:h2:mem:testdb");
        parameters.setDatabaseUser("policy");
        parameters.setDatabasePassword("P01icY");
        parameters.setPersistenceUnit("ToscaConceptTest");

        controlLoopProvider = new ControlLoopProvider(parameters);

        inputControlLoops = CODER.decode(originalJson, ControlLoops.class);
        updateControlLoops = CODER.decode(updateClJson, ControlLoops.class);
    }

    @Test
    public void testControlLoopCreate() throws Exception {
        assertThatThrownBy(() -> {
            controlLoopProvider.createControlLoops(null);
        }).hasMessageMatching(LIST_IS_NULL);

        ControlLoops createdControlLoops = new ControlLoops();
        createdControlLoops
                .setControlLoops(controlLoopProvider.createControlLoops(inputControlLoops.getControlLoops()));
        String createdJson = CODER.encode(createdControlLoops, true);

        System.err.println(originalJson);
        System.out.println(createdJson);
        assertEquals(originalJson.replaceAll("\\s+", ""), createdJson.replaceAll("\\s+", ""));
    }

    @Test
    public void testGetControlLoops() throws Exception {

        List<ControlLoop> getResponse;

        // Return empty list when no data present in db
        getResponse = controlLoopProvider.getControlLoops(null, null);
        assertThat(getResponse).isEmpty();

        controlLoopProvider.createControlLoops(inputControlLoops.getControlLoops());
        String name = inputControlLoops.getControlLoops().get(0).getName();
        String version = inputControlLoops.getControlLoops().get(0).getVersion();
        assertEquals(1, controlLoopProvider.getControlLoops(name, version).size());

        ControlLoop cl = new ControlLoop();
        cl = controlLoopProvider.getControlLoop(new ToscaConceptIdentifier("PMSHInstance1", "1.0.1"));
        assertEquals(inputControlLoops.getControlLoops().get(1), cl);

        assertNull(controlLoopProvider.getControlLoop(new ToscaConceptIdentifier("invalid_name", "1.0.1")));

        assertThatThrownBy(() -> {
            controlLoopProvider.getFilteredControlLoops(null);
        }).hasMessageMatching("filter is marked .*ull but is null");

        final ToscaTypedEntityFilter<ControlLoop> filter = ToscaTypedEntityFilter.<ControlLoop>builder()
                .type("org.onap.domain.pmsh.PMSHControlLoopDefinition").build();
        assertEquals(2, controlLoopProvider.getFilteredControlLoops(filter).size());
    }


    @Test
    public void testUpdateControlLoops() throws Exception {
        assertThatThrownBy(() -> {
            controlLoopProvider.updateControlLoops(null);
        }).hasMessageMatching("controlLoops is marked .*ull but is null");

        ControlLoops existingControlLoops = new ControlLoops();
        existingControlLoops
                .setControlLoops(controlLoopProvider.createControlLoops(inputControlLoops.getControlLoops()));
        ControlLoop updateResponse = new ControlLoop();
        updateResponse = controlLoopProvider.updateControlLoop(updateControlLoops.getControlLoops().get(0));

        assertEquals(ControlLoopOrderedState.RUNNING, updateResponse.getOrderedState());
    }

    @Test
    public void testDeleteControlLoop() throws Exception {
        assertThatThrownBy(() -> {
            controlLoopProvider.deleteControlLoop("Invalid_name", "1.0.1");
        }).hasMessageMatching(".*.failed, control loop does not exist");

        ControlLoop deletedCl;
        List<ControlLoop> clList = controlLoopProvider.createControlLoops(inputControlLoops.getControlLoops());
        String name = inputControlLoops.getControlLoops().get(0).getName();
        String version = inputControlLoops.getControlLoops().get(0).getVersion();

        deletedCl = controlLoopProvider.deleteControlLoop(name, version);
        assertEquals(clList.get(0), deletedCl);

    }

    @After
    public void teardown() {
        controlLoopProvider.close();
    }
}


