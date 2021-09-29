/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.common.exception.ControlLoopRuntimeException;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoops;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ClElementStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.controlloop.models.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.controlloop.runtime.commissioning.CommissioningProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.supervision.SupervisionHandler;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopStateChangePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ControlLoopUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantDeregisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantRegisterAckPublisher;
import org.onap.policy.clamp.controlloop.runtime.supervision.comm.ParticipantUpdatePublisher;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

/**
 * Class to perform unit test of {@link ControlLoopInstantiationProvider}}.
 *
 */
class ControlLoopInstantiationProviderTest {

    private static final String CL_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/controlloops/ControlLoops.json";
    private static final String CL_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsUpdate.json";
    private static final String CL_INSTANTIATION_CHANGE_STATE_JSON =
            "src/test/resources/rest/controlloops/PassiveCommand.json";
    private static final String CL_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON =
            "src/test/resources/rest/controlloops/ControlLoopElementsNotFound.json";
    private static final String CL_INSTANTIATION_CONTROLLOOP_DEFINITION_NOT_FOUND_JSON =
            "src/test/resources/rest/controlloops/ControlLoopsNotFound.json";
    private static final String TOSCA_TEMPLATE_YAML =
            "src/test/resources/rest/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final String CONTROL_LOOP_NOT_FOUND = "Control Loop not found";
    private static final String DELETE_BAD_REQUEST = "Control Loop State is still %s";
    private static final String ORDERED_STATE_INVALID = "ordered state invalid or not specified on command";
    private static final String CONTROLLOOP_ELEMENT_NAME_NOT_FOUND =
            "\"ControlLoops\" INVALID, item has status INVALID\n"
                    + "  \"entry org.onap.domain.pmsh.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
                    + "    \"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not FOUND\n"
                    + "  \"entry org.onap.domain.pmsh.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
                    + "    \"entry org.onap.domain.pmsh.DCAEMicroservice\" INVALID, Not FOUND\n";

    private static final String CONTROLLOOP_DEFINITION_NOT_FOUND = "\"ControlLoops\" INVALID, item has status INVALID\n"
            + "  \"entry org.onap.domain.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
            + "    item \"ControlLoop\" value \"org.onap.domain.PMSHControlLoopDefinition\" INVALID,"
            + " Commissioned control loop definition not FOUND\n"
            + "  \"entry org.onap.domain.PMSHControlLoopDefinition\" INVALID, item has status INVALID\n"
            + "    item \"ControlLoop\" value \"org.onap.domain.PMSHControlLoopDefinition\" INVALID,"
            + " Commissioned control loop definition not FOUND\n";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();

    private static SupervisionHandler supervisionHandler;
    private static CommissioningProvider commissioningProvider;
    private static ControlLoopProvider clProvider;
    private static PolicyModelsProvider modelsProvider;
    private static ParticipantProvider participantProvider;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        serviceTemplate = yamlTranslator.fromYaml(ResourceUtils.getResourceAsString(TOSCA_TEMPLATE_YAML),
                ToscaServiceTemplate.class);
    }

    /**
     * setup Db Provider Parameters.
     *
     * @throws PfModelException if an error occurs
     */
    @BeforeAll
    public static void setupDbProviderParameters() throws PfModelException {
        ClRuntimeParameterGroup controlLoopParameters = CommonTestData.geParameterGroup("instantproviderdb");

        modelsProvider = CommonTestData.getPolicyModelsProvider(controlLoopParameters.getDatabaseProviderParameters());
        clProvider = new ControlLoopProvider(controlLoopParameters.getDatabaseProviderParameters());
        participantProvider = new ParticipantProvider(controlLoopParameters.getDatabaseProviderParameters());

        var participantStatisticsProvider =
                new ParticipantStatisticsProvider(controlLoopParameters.getDatabaseProviderParameters());
        var clElementStatisticsProvider =
                new ClElementStatisticsProvider(controlLoopParameters.getDatabaseProviderParameters());
        commissioningProvider = new CommissioningProvider(new ServiceTemplateProvider(modelsProvider), clProvider, null,
                participantProvider);
        var monitoringProvider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        var participantProvider = new ParticipantProvider(controlLoopParameters.getDatabaseProviderParameters());
        var controlLoopUpdatePublisher = Mockito.mock(ControlLoopUpdatePublisher.class);
        var controlLoopStateChangePublisher = Mockito.mock(ControlLoopStateChangePublisher.class);
        var participantRegisterAckPublisher = Mockito.mock(ParticipantRegisterAckPublisher.class);
        var participantDeregisterAckPublisher = Mockito.mock(ParticipantDeregisterAckPublisher.class);
        var participantUpdatePublisher = Mockito.mock(ParticipantUpdatePublisher.class);
        supervisionHandler = new SupervisionHandler(clProvider, participantProvider, monitoringProvider,
                controlLoopUpdatePublisher, controlLoopStateChangePublisher, participantRegisterAckPublisher,
                participantDeregisterAckPublisher, participantUpdatePublisher);
    }

    @BeforeEach
    public void populateDb() throws Exception {
        createEntryInDB();
    }

    @AfterAll
    public static void closeDbProvider() throws PfModelException {
        clProvider.close();
        modelsProvider.close();
    }

    @AfterEach
    public void cleanDatabase() throws Exception {
        deleteEntryInDB(serviceTemplate.getName(), serviceTemplate.getVersion());
    }

    @Test
    void testInstantiationCrud() throws Exception {
        participantProvider.createParticipants(CommonTestData.createParticipants());

        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Crud");
        ControlLoops controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
        assertThat(controlLoopsDb.getControlLoopList()).isEmpty();
        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);
        InstantiationResponse instantiationResponse = instantiationProvider.createControlLoops(controlLoopsCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsCreate);

        controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
        assertThat(controlLoopsDb.getControlLoopList()).isNotEmpty();
        assertThat(controlLoopsCreate).isEqualTo(controlLoopsDb);

        for (ControlLoop controlLoop : controlLoopsCreate.getControlLoopList()) {
            ControlLoops controlLoopsGet =
                    instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
            assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
            assertThat(controlLoop).isEqualTo(controlLoopsGet.getControlLoopList().get(0));
        }

        ControlLoops controlLoopsUpdate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_UPDATE_JSON, "Crud");
        assertThat(controlLoopsUpdate).isNotEqualTo(controlLoopsDb);

        instantiationResponse = instantiationProvider.updateControlLoops(controlLoopsUpdate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsUpdate);

        controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
        assertThat(controlLoopsDb.getControlLoopList()).isNotEmpty();
        assertThat(controlLoopsUpdate).isEqualTo(controlLoopsDb);

        InstantiationCommand instantiationCommand =
                InstantiationUtils.getInstantiationCommandFromResource(CL_INSTANTIATION_CHANGE_STATE_JSON, "Crud");
        instantiationResponse = instantiationProvider.issueControlLoopCommand(instantiationCommand);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, instantiationCommand);

        for (ToscaConceptIdentifier toscaConceptIdentifier : instantiationCommand.getControlLoopIdentifierList()) {
            ControlLoops controlLoopsGet = instantiationProvider.getControlLoops(toscaConceptIdentifier.getName(),
                    toscaConceptIdentifier.getVersion());
            assertThat(controlLoopsGet.getControlLoopList()).hasSize(1);
            assertThat(instantiationCommand.getOrderedState())
                    .isEqualTo(controlLoopsGet.getControlLoopList().get(0).getOrderedState());
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

    private ControlLoops getControlLoopsFromDb(ControlLoops controlLoopsSource) throws Exception {

        ControlLoops controlLoopsDb = new ControlLoops();
        controlLoopsDb.setControlLoopList(new ArrayList<>());

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

        for (ControlLoop controlLoop : controlLoopsSource.getControlLoopList()) {
            ControlLoops controlLoopsFromDb =
                    instantiationProvider.getControlLoops(controlLoop.getName(), controlLoop.getVersion());
            controlLoopsDb.getControlLoopList().addAll(controlLoopsFromDb.getControlLoopList());
        }
        return controlLoopsDb;
    }

    @Test
    void testInstantiationDelete() throws Exception {

        ControlLoops controlLoops =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "Delete");
        assertThat(getControlLoopsFromDb(controlLoops).getControlLoopList()).isEmpty();

        ControlLoop controlLoop0 = controlLoops.getControlLoopList().get(0);

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

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

    private void assertThatDeleteThrownBy(ControlLoops controlLoops, ControlLoopState state) throws Exception {
        ControlLoop controlLoop = controlLoops.getControlLoopList().get(0);

        controlLoop.setState(state);

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

        instantiationProvider.updateControlLoops(controlLoops);
        assertThatThrownBy(
                () -> instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion()))
                        .hasMessageMatching(String.format(DELETE_BAD_REQUEST, state));
    }

    @Test
    void testCreateControlLoops_NoDuplicates() throws Exception {

        ControlLoops controlLoopsCreate =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "NoDuplicates");

        ControlLoops controlLoopsDb = getControlLoopsFromDb(controlLoopsCreate);
        assertThat(controlLoopsDb.getControlLoopList()).isEmpty();

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

        InstantiationResponse instantiationResponse = instantiationProvider.createControlLoops(controlLoopsCreate);
        InstantiationUtils.assertInstantiationResponse(instantiationResponse, controlLoopsCreate);

        assertThatThrownBy(() -> instantiationProvider.createControlLoops(controlLoopsCreate)).hasMessageMatching(
                controlLoopsCreate.getControlLoopList().get(0).getKey().asIdentifier() + " already defined");

        for (ControlLoop controlLoop : controlLoopsCreate.getControlLoopList()) {
            instantiationProvider.deleteControlLoop(controlLoop.getName(), controlLoop.getVersion());
        }
    }

    @Test
    void testCreateControlLoops_CommissionedClElementNotFound() throws Exception {

        ControlLoops controlLoops = InstantiationUtils
                .getControlLoopsFromResource(CL_INSTANTIATION_DEFINITION_NAME_NOT_FOUND_JSON, "ClElementNotFound");

        var provider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider, supervisionHandler,
                participantProvider);

        // to validate control Loop, it needs to define ToscaServiceTemplate
        // InstantiationUtils.storeToscaServiceTemplate(TOSCA_TEMPLATE_YAML, commissioningProvider);

        assertThat(getControlLoopsFromDb(controlLoops).getControlLoopList()).isEmpty();

        assertThatThrownBy(() -> provider.createControlLoops(controlLoops))
                .hasMessageMatching(CONTROLLOOP_ELEMENT_NAME_NOT_FOUND);
    }

    @Test
    void testCreateControlLoops_CommissionedClNotFound() throws Exception {
        ControlLoops controlLoops = InstantiationUtils
                .getControlLoopsFromResource(CL_INSTANTIATION_CONTROLLOOP_DEFINITION_NOT_FOUND_JSON, "ClNotFound");

        assertThat(getControlLoopsFromDb(controlLoops).getControlLoopList()).isEmpty();

        var provider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider, supervisionHandler,
                participantProvider);
        assertThatThrownBy(() -> provider.createControlLoops(controlLoops))
                .hasMessageMatching(CONTROLLOOP_DEFINITION_NOT_FOUND);
    }

    @Test
    void testIssueControlLoopCommand_OrderedStateInvalid() throws ControlLoopRuntimeException, IOException {
        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);
        assertThatThrownBy(() -> instantiationProvider.issueControlLoopCommand(new InstantiationCommand()))
                .hasMessageMatching(ORDERED_STATE_INVALID);
    }

    @Test
    void testInstantiationVersions() throws Exception {
        // create controlLoops V1
        ControlLoops controlLoopsV1 =
                InstantiationUtils.getControlLoopsFromResource(CL_INSTANTIATION_CREATE_JSON, "V1");
        assertThat(getControlLoopsFromDb(controlLoopsV1).getControlLoopList()).isEmpty();

        var instantiationProvider = new ControlLoopInstantiationProvider(clProvider, commissioningProvider,
                supervisionHandler, participantProvider);

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
            assertThat(controlLoop).isEqualTo(controlLoopsGet.getControlLoopList().get(0));
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
            assertThat(controlLoop).isEqualTo(controlLoopsGet.getControlLoopList().get(0));
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

    private synchronized void deleteEntryInDB(String name, String version) throws Exception {
        if (!modelsProvider.getServiceTemplateList(null, null).isEmpty()) {
            modelsProvider.deleteServiceTemplate(name, version);
        }
    }

    private synchronized void createEntryInDB() throws Exception {
        try {
            deleteEntryInDB(serviceTemplate.getName(), serviceTemplate.getVersion());
            modelsProvider.createServiceTemplate(serviceTemplate);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
