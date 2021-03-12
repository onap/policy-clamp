/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.instantiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to perform unit test of {@link ControlLoopInstantiationProvider}}.
 *
 */
public class ControlLoopInstantiationProviderTest {

    private static final String CL_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/controlloops/ControlLoops.json";
    private static final String CL_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsUpdate.json";
    private static final String CL_INSTANTIATION_CHANGE_STATE_JSON =
            "src/test/resources/rest/controlloops/PassiveCommand.json";
    private static final String CL_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsElementsNotFound.json";
    private static final String CL_INSTANTIATION_CONTROLLOOP_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsNotFound.json";
    private static final String TOSCA_TEMPLATE_YAML = "examples/controlloop/PMSubscriptionHandling.yaml";

    private static final String CONTROL_LOOP_NOT_FOUND = "Control Loop not found";
    private static final String DELETE_BAD_REQUEST = "Control Loop State is still %s";
    private static final String ORDERED_STATE_INVALID = "ordered state invalid or not specified on command";

    private static final String CONTROLLOOP_ELEMENT_NAME_NOT_FOUND =
            "\"ControlLoops\" INVALID, item has status INVALID\n"
                    + "  item \"Control Loop Element\" value \"org.onap.domain.pmsh.NotExistFirst 1.2.3\" INVALID,"
                    + " org.onap.domain.pmsh.NotExistFirst not FOUND\n"
                    + "  item \"Control Loop Element\" value \"org.onap.domain.pmsh.NotExistSecond 1.2.3\" INVALID,"
                    + " org.onap.domain.pmsh.NotExistSecond not FOUND\n";

    private static final String CONTROLLOOP_DEFINITION_NOT_FOUND =
            "\"ControlLoops\" INVALID, Commissioned control loop definition not FOUND\n";

    private static PolicyModelsProviderParameters databaseProviderParameters;

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeClass
    public static void setupDbProviderParameters() throws PfModelException {
        databaseProviderParameters =
                CommonTestData.geParameterGroup(0, "instantproviderdb").getDatabaseProviderParameters();
    }

    @Test
    public void testInstantiationCrud() throws Exception {
        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Crud");

        ControlLoops controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
        assertThat(controlLoopsDb.getControlLoopList()).isEmpty();

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            // to validate control Loop, it needs to define ToscaServiceTemplate
            InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, databaseProviderParameters);

            InstantiationResponse instantiationResponse = instantiationProvider.createControlLoops(controlLoopsCreate);
            InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsCreate);

            controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
            assertThat(controlLoopsDb.getControlLoopList()).isNotEmpty();
            Assert.assertEquals(controlLoopsCreate, controlLoopsDb);

            for (ControlLoop controlLoop : controlLoopsCreate.getControlLoopList()) {
                ControlLoops controlLoopsGet =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
                Assert.assertEquals(controlLoop, controlLoopsGet.getControlLoopList().get(0));
            }

            ControlLoops controlLoopsUpdate =
                    InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_UPDATE_JSON, "Crud");
            Assert.assertNotEquals(controlLoopsUpdate, controlLoopsDb);

            instantiationResponse = instantiationProvider.updateControlLoops(controlLoopsUpdate);
            InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsUpdate);

            controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
            assertThat(controlLoopsDb.getControlLoopList()).isNotEmpty();
            Assert.assertEquals(controlLoopsUpdate, controlLoopsDb);

            InstantiationCommand instantiationCommand =
                    InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Crud");
            instantiationResponse = instantiationProvider.issueControlLoopCommand(instantiationCommand);
            InstantiationUtils.assertInstantiationResponse(instantiationResponse, instantiationCommand);

            for (ToscaConceptIdentifier toscaConceptIdentifier : instantiationCommand.getControlLoopIdentifierList()) {
                ControlLoops controlLoopsGet = instantiationProvider.getControlLoops(toscaConceptIdentifier.getName(),
                        toscaConceptIdentifier.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
                Assert.assertEquals(instantiationCommand.getOrderedState(),
                        controlLoopsGet.getControlLoopList().get(0).getOrderedState());
            }

            // in order to delete a controlLoop the state must be UNINITIALISED
            controlLoopsCreate.getControlLoopList().forEach(cl -> cl.setState(ControlLoopState.UNINITIALISED));
            instantiationProvider.updateControlLoops(controlLoopsCreate);

            for (ControlLoop controlLoop : controlLoopsCreate.getControlLoopList()) {
                instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
            }

            controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
            assertThat(controlLoopsDb.getControlLoopList()).isEmpty();
        }
    }

    private ControlLoops getControlLoopsFromDb(ControlLoops controlLoopsSource) throws Exception {
        ControlLoops controlLoopsDb = new ControlLoops();
        controlLoopsDb.setControlLoopList(new ArrayList<>());

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            for (ControlLoop controlLoop : controlLoopsSource.getControlLoopList()) {
                ControlLoops controlLoopsFromDb =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                controlLoopsDb.getControlLoopList().addAll(controlLoopsFromDb.getControlLoopList());
            }
            return controlLoopsDb;
        }
    }

    @Test
    public void testInstantiationDelete() throws Exception {
        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Delete");
        assertThat(getControlLoopsFromDb(controlLoops).getControlLoopList()).isEmpty();

        ControlLoop controlLoop0 = controlLoops.getControlLoopList().get(0);

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            // to validate control Loop, it needs to define ToscaServiceTemplate
            InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, databaseProviderParameters);

            assertThatThrownBy(
                    () -> instantiationProvider.deleteControlLoop(controlLoop0.getName(), controlLoop0.getVersion()))
                            .hasMessageMatching(CONTROL_LOOP_NOT_FOUND);

            InstantiationUtils.assertInstantiationResponse(instantiationProvider.createControlLoops(controlLoops),
                    controlLoops);

            for (ControlLoopState state : ControlLoopState.values()) {
                if (!ControlLoopState.UNINITIALISED.equals(state)) {
                    assertThatDeleteThrownBy(controlLoops, state);
                }
            }

            controlLoop0.setState(ControlLoopState.UNINITIALISED);
            instantiationProvider.updateControlLoops(controlLoops);

            for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
                instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
            }

            for (ControlLoop controlLoop : controlLoops.getControlLoopList()) {
                ControlLoops controlLoopsGet =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).isEmpty();
            }
        }
    }

    private void assertThatDeleteThrownBy(ControlLoops controlLoops, ControlLoopState state) throws Exception {
        ControlLoop controlLoop = controlLoops.getControlLoopList().get(0);

        controlLoop.setState(state);

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            instantiationProvider.updateControlLoops(controlLoops);
            assertThatThrownBy(
                    () -> instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion()))
                            .hasMessageMatching(String.format(DELETE_BAD_REQUEST, state));
        }
    }

    @Test
    public void testCreateControlLoops_NoDuplicates() throws Exception {
        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "NoDuplicates");

        ControlLoops controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
        assertThat(controlLoopsDb.getControlLoopList()).isEmpty();

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            // to validate control Loop, it needs to define ToscaServiceTemplate
            InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, databaseProviderParameters);

            InstantiationResponse instantiationResponse = instantiationProvider.createControlLoops(controlLoopsCreate);
            InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsCreate);

            assertThatThrownBy(() -> instantiationProvider.createControlLoops(controlLoopsCreate)).hasMessageMatching(
                    controlLoopsCreate.getControlLoopList().get(0).getKey().asIdentifier() + " already defined");

            for (ControlLoop controlLoop : controlLoopsCreate.getControlLoopList()) {
                instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
            }
        }
    }

    @Test
    public void testCreateControlLoops_CommissionedClElementNotFound() throws Exception {
        ControlLoops controlLoops = InstantiationUtils
                .getControlLoopsFromResource(CL_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "ClElementNotFound");

        try (ControlLoopInstantiationProvider provider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            // to validate control Loop, it needs to define ToscaServiceTemplate
            InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, databaseProviderParameters);

            assertThat(getControlLoopsFromDb(controlLoops).getControlLoopList()).isEmpty();

            assertThatThrownBy(() -> provider.createControlLoops(controlLoops))
                    .hasMessageMatching(CONTROLLOOP_ELEMENT_NAME_NOT_FOUND);
        }
    }

    @Test
    public void testCreateControlLoops_CommissionedClNotFound() throws Exception {
        ControlLoops controlLoops = InstantiationUtils.getControlLoopsFromResource(
                CL_INSTANTIATION_CONTROLLOOP_DEFINITION_NOT_FOUND_JSON, "ClNotFound");

        assertThat(getControlLoopsFromDb(controlLoops).getControlLoopList()).isEmpty();

        try (ControlLoopInstantiationProvider provider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {
            assertThatThrownBy(() -> provider.createControlLoops(controlLoops))
                    .hasMessageMatching(CONTROLLOOP_DEFINITION_NOT_FOUND);
        }
    }

    @Test
    public void testIssueControlLoopCommand_OrderedStateInvalid() throws ControlLoopRuntimeException, IOException {
        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {
            assertThatThrownBy(() -> instantiationProvider.issueControlLoopCommand(new InstantiationCommand()))
                    .hasMessageMatching(ORDERED_STATE_INVALID);
        }
    }

    @Test
    public void testInstantiationVersions() throws Exception {

        // create controlLoops V1
        ControlLoops controlLoopsV1 =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "V1");
        assertThat(getControlLoopsFromDb(controlLoopsV1).getControlLoopList()).isEmpty();

        try (ControlLoopInstantiationProvider instantiationProvider =
                new ControlLoopInstantiationProvider(databaseProviderParameters)) {

            // to validate control Loop, it needs to define ToscaServiceTemplate
            InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, databaseProviderParameters);

            InstantiationUtils.assertInstantiationResponse(instantiationProvider.createControlLoops(controlLoopsV1),
                    controlLoopsV1);

            // create controlLoops V2
            ControlLoops controlLoopsV2 =
                    InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "V2");
            assertThat(getControlLoopsFromDb(controlLoopsV2).getControlLoopList()).isEmpty();
            InstantiationUtils.assertInstantiationResponse(instantiationProvider.createControlLoops(controlLoopsV2),
                    controlLoopsV2);

            // GET controlLoops V2
            for (ControlLoop controlLoop : controlLoopsV2.getControlLoopList()) {
                ControlLoops controlLoopsGet =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
                Assert.assertEquals(controlLoop, controlLoopsGet.getControlLoopList().get(0));
            }

            // DELETE controlLoops V1
            for (ControlLoop controlLoop : controlLoopsV1.getControlLoopList()) {
                instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
            }

            // GET controlLoops V1 is not available
            for (ControlLoop controlLoop : controlLoopsV1.getControlLoopList()) {
                ControlLoops controlLoopsGet =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).isEmpty();
            }

            // GET controlLoops V2 is still available
            for (ControlLoop controlLoop : controlLoopsV2.getControlLoopList()) {
                ControlLoops controlLoopsGet =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
                Assert.assertEquals(controlLoop, controlLoopsGet.getControlLoopList().get(0));
            }

            // DELETE controlLoops V2
            for (ControlLoop controlLoop : controlLoopsV2.getControlLoopList()) {
                instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
            }

            // GET controlLoops V2 is not available
            for (ControlLoop controlLoop : controlLoopsV2.getControlLoopList()) {
                ControlLoops controlLoopsGet =
                        instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
                assertThat(controlLoopsGet.getControlLoopList()).isEmpty();
            }
        }
    }
}
