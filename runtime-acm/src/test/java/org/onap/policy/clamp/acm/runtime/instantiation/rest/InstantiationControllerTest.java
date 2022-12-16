/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.instantiation.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.rest.InstantiationController;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationUpdate;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class to perform unit test of {@link InstantiationController}}.
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "default" })
class InstantiationControllerTest extends CommonRestController {

    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";

    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json";

    private static final String AC_INSTANTIATION_CHANGE_STATE_JSON = "src/test/resources/rest/acm/PassiveCommand.json";

    private static final String INSTANTIATION_ENDPOINT = "compositions/%s/instances";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
    private UUID compositionId = UUID.randomUUID();

    @Autowired
    private AutomationCompositionRepository automationCompositionRepository;

    @Autowired
    private AcDefinitionProvider acDefinitionProvider;

    @Autowired
    private AutomationCompositionInstantiationProvider instantiationProvider;

    @Autowired
    private ParticipantProvider participantProvider;

    @LocalServerPort
    private int randomServerPort;

    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @BeforeEach
    public void populateDb() {
        createEntryInDB();
    }

    @BeforeEach
    public void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @AfterEach
    public void cleanDatabase() {
        deleteEntryInDB();
    }

    private String getInstanceEndPoint() {
        return String.format(INSTANTIATION_ENDPOINT, compositionId.toString());
    }

    private String getInstanceEndPoint(UUID instanceId) {
        return String.format(INSTANTIATION_ENDPOINT, compositionId.toString()) + "/" + instanceId;
    }

    @Test
    void testSwagger() {
        super.testSwagger(String.format(INSTANTIATION_ENDPOINT, "{compositionId}"));
    }

    @Test
    void testCreate_Unauthorized() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Unauthorized");

        assertUnauthorizedPost(getInstanceEndPoint(), Entity.json(automationComposition));
    }

    @Test
    void testQuery_Unauthorized() {
        assertUnauthorizedGet(getInstanceEndPoint());
    }

    @Test
    void testUpdate_Unauthorized() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Unauthorized");

        assertUnauthorizedPut(getInstanceEndPoint(), Entity.json(automationComposition));
    }

    @Test
    void testDelete_Unauthorized() {
        assertUnauthorizedDelete(getInstanceEndPoint());
    }

    @Test
    void testCreate() {
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Create");
        automationCompositionFromRsc.setCompositionId(compositionId);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint());
        var resp = invocationBuilder.post(Entity.json(automationCompositionFromRsc));
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        var instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);
        automationCompositionFromRsc.setInstanceId(instResponse.getInstanceId());

        var automationCompositionFromDb =
                instantiationProvider.getAutomationComposition(compositionId, instResponse.getInstanceId());

        assertNotNull(automationCompositionFromDb);
        assertEquals(automationCompositionFromRsc, automationCompositionFromDb);

    }

    @Test
    void testCreateBadRequest() {
        var automationCompositionFromRsc = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "CreateBadRequest");
        automationCompositionFromRsc.setCompositionId(compositionId);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint());
        var resp = invocationBuilder.post(Entity.json(automationCompositionFromRsc));
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());

        // testing Bad Request: AC already defined
        resp = invocationBuilder.post(Entity.json(automationCompositionFromRsc));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        var instResponse = resp.readEntity(InstantiationResponse.class);
        assertNotNull(instResponse.getErrorDetails());
        assertNull(instResponse.getAffectedAutomationComposition());
    }

    @Test
    void testQuery_NoResultWithThisName() {
        var invocationBuilder = super.sendRequest(getInstanceEndPoint() + "?name=noResultWithThisName");
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var resp = rawresp.readEntity(AutomationCompositions.class);
        assertThat(resp.getAutomationCompositionList()).isEmpty();
    }

    @Test
    void testQuery() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        automationComposition.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var invocationBuilder =
                super.sendRequest(getInstanceEndPoint() + "?name=" + automationComposition.getKey().getName());
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var automationCompositionsQuery = rawresp.readEntity(AutomationCompositions.class);
        assertNotNull(automationCompositionsQuery);
        assertThat(automationCompositionsQuery.getAutomationCompositionList()).hasSize(1);
        assertEquals(automationComposition, automationCompositionsQuery.getAutomationCompositionList().get(0));
    }

    @Disabled
    @Test
    void testUpdate() {
        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Update");
        automationCompositionCreate.setCompositionId(compositionId);

        var response = instantiationProvider.createAutomationComposition(compositionId, automationCompositionCreate);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Update");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(response.getInstanceId());
        var instantiationUpdate = new InstantiationUpdate();
        instantiationUpdate.setElements(automationComposition.getElements());
        var invocationBuilder = super.sendRequest(getInstanceEndPoint(response.getInstanceId()));
        var resp = invocationBuilder.put(Entity.json(automationComposition));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        var instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationComposition);

        var automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(
                automationComposition.getKey().getName(), automationComposition.getKey().getVersion());

        assertNotNull(automationCompositionsFromDb);
        assertThat(automationCompositionsFromDb.getAutomationCompositionList()).hasSize(1);
        assertEquals(automationComposition, automationCompositionsFromDb.getAutomationCompositionList().get(0));
    }

    @Test
    void testDelete() {
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");
        automationCompositionFromRsc.setCompositionId(compositionId);

        var instResponse =
                instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(instResponse.getInstanceId()));
        var resp = invocationBuilder.delete();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);

        var automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(
                automationCompositionFromRsc.getKey().getName(), automationCompositionFromRsc.getKey().getVersion());
        assertThat(automationCompositionsFromDb.getAutomationCompositionList()).isEmpty();
    }

    @Test
    void testDeleteNotFound() {
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "DelNotFound");
        automationCompositionFromRsc.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(UUID.randomUUID()));
        var resp = invocationBuilder.delete();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Disabled
    @Test
    void testCommand_NotFound1() {
        var invocationBuilder = super.sendRequest(getInstanceEndPoint(UUID.randomUUID()));
        var resp = invocationBuilder.put(Entity.json(new InstantiationUpdate()));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Disabled
    @Test
    void testCommand_NotFound2() {
        var acFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "DelNotFound");
        acFromRsc.setCompositionId(compositionId);

        var instResponse = instantiationProvider.createAutomationComposition(compositionId, acFromRsc);

        var command = new InstantiationUpdate();
        command.setInstantiationCommand(new InstantiationCommand());
        command.getInstantiationCommand().setOrderedState(null);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(instResponse.getInstanceId()));
        var resp = invocationBuilder.put(Entity.json(command));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Disabled
    @Test
    void testCommand() throws PfModelException {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Command");
        automationComposition.setCompositionId(compositionId);
        var instResponse = instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var participants = CommonTestData.createParticipants();
        for (var participant : participants) {
            participantProvider.saveParticipant(participant);
        }

        var instantiationUpdate = new InstantiationUpdate();
        var command = InstantiationUtils.getInstantiationCommandFromResource(AC_INSTANTIATION_CHANGE_STATE_JSON);
        instantiationUpdate.setInstantiationCommand(command);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(instResponse.getInstanceId()));
        var resp = invocationBuilder.put(Entity.json(instantiationUpdate));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationComposition);

        // check passive state on DB
        var toscaConceptIdentifier = instResponse.getAffectedAutomationComposition();
        var automationCompositionsGet = instantiationProvider
                .getAutomationCompositions(toscaConceptIdentifier.getName(), toscaConceptIdentifier.getVersion());
        assertThat(automationCompositionsGet.getAutomationCompositionList()).hasSize(1);
        assertEquals(command.getOrderedState(),
                automationCompositionsGet.getAutomationCompositionList().get(0).getOrderedState());
    }

    private synchronized void deleteEntryInDB() {
        automationCompositionRepository.deleteAll();
        var list = acDefinitionProvider.findAcDefinition(compositionId);
        if (!list.isEmpty()) {
            acDefinitionProvider.deleteAcDefintion(compositionId);
        }
    }

    private synchronized void createEntryInDB() {
        deleteEntryInDB();
        var acmDefinition = acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate);
        compositionId = acmDefinition.getCompositionId();
    }
}
