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

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.rest.InstantiationController;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationCommand;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.clamp.models.acm.persistence.repository.AutomationCompositionRepository;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
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
@ActiveProfiles("test")
class InstantiationControllerTest extends CommonRestController {

    private static final String ID_NAME = "PMSH_Test_Instance";
    private static final String ID_VERSION = "1.2.3";

    private static final String AC_INSTANTIATION_CREATE_JSON =
        "src/test/resources/rest/acm/AutomationCompositions.json";

    private static final String AC_INSTANTIATION_UPDATE_JSON =
        "src/test/resources/rest/acm/AutomationCompositionsUpdate.json";

    private static final String AC_INSTANTIATION_CHANGE_STATE_JSON = "src/test/resources/rest/acm/PassiveCommand.json";

    private static final String INSTANTIATION_ENDPOINT = "instantiation";
    private static final String INSTANTIATION_COMMAND_ENDPOINT = "instantiation/command";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @Autowired
    private AutomationCompositionRepository automationCompositionRepository;

    @Autowired
    private ServiceTemplateProvider serviceTemplateProvider;

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
    public void populateDb() throws Exception {
        createEntryInDB();
    }

    @BeforeEach
    public void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @AfterEach
    public void cleanDatabase() throws Exception {
        deleteEntryInDB();
    }

    @Test
    void testSwagger() {
        super.testSwagger(INSTANTIATION_ENDPOINT);
    }

    @Test
    void testCreate_Unauthorized() throws Exception {
        AutomationCompositions automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Unauthorized");

        assertUnauthorizedPost(INSTANTIATION_ENDPOINT, Entity.json(automationCompositions));
    }

    @Test
    void testQuery_Unauthorized() {
        assertUnauthorizedGet(INSTANTIATION_ENDPOINT);
    }

    @Test
    void testUpdate_Unauthorized() throws Exception {
        AutomationCompositions automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_UPDATE_JSON, "Unauthorized");

        assertUnauthorizedPut(INSTANTIATION_ENDPOINT, Entity.json(automationCompositions));
    }

    @Test
    void testDelete_Unauthorized() {
        assertUnauthorizedDelete(INSTANTIATION_ENDPOINT);
    }

    @Test
    void testCommand_Unauthorized() throws Exception {
        InstantiationCommand instantiationCommand =
            InstantiationUtils.getInstantiationCommandFromResource(AC_INSTANTIATION_CHANGE_STATE_JSON, "Unauthorized");

        assertUnauthorizedPut(INSTANTIATION_COMMAND_ENDPOINT, Entity.json(instantiationCommand));
    }

    @Test
    void testCreate() throws Exception {

        AutomationCompositions automationCompositionsFromRsc =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Create");

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(automationCompositionsFromRsc));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionsFromRsc);

        for (AutomationComposition automationCompositionFromRsc : automationCompositionsFromRsc
            .getAutomationCompositionList()) {
            AutomationCompositions automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(
                automationCompositionFromRsc.getKey().getName(), automationCompositionFromRsc.getKey().getVersion());

            assertNotNull(automationCompositionsFromDb);
            assertThat(automationCompositionsFromDb.getAutomationCompositionList()).hasSize(1);
            assertEquals(automationCompositionFromRsc,
                automationCompositionsFromDb.getAutomationCompositionList().get(0));
        }
    }

    @Test
    void testCreateBadRequest() throws Exception {

        AutomationCompositions automationCompositionsFromRsc =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "CreateBadRequest");

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(automationCompositionsFromRsc));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        // testing Bad Request: AC already defined
        resp = invocationBuilder.post(Entity.json(automationCompositionsFromRsc));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        assertNotNull(instResponse.getErrorDetails());
        assertNull(instResponse.getAffectedAutomationCompositions());
    }

    @Test
    void testQuery_NoResultWithThisName() {
        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT + "?name=noResultWithThisName");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        AutomationCompositions resp = rawresp.readEntity(AutomationCompositions.class);
        assertThat(resp.getAutomationCompositionList()).isEmpty();
    }

    @Test
    void testQuery() throws Exception {

        var automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        instantiationProvider.createAutomationCompositions(automationCompositions);

        for (AutomationComposition automationCompositionFromRsc : automationCompositions
            .getAutomationCompositionList()) {
            Invocation.Builder invocationBuilder =
                super.sendRequest(INSTANTIATION_ENDPOINT + "?name=" + automationCompositionFromRsc.getKey().getName());
            Response rawresp = invocationBuilder.buildGet().invoke();
            assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            AutomationCompositions automationCompositionsQuery = rawresp.readEntity(AutomationCompositions.class);
            assertNotNull(automationCompositionsQuery);
            assertThat(automationCompositionsQuery.getAutomationCompositionList()).hasSize(1);
            assertEquals(automationCompositionFromRsc,
                automationCompositionsQuery.getAutomationCompositionList().get(0));
        }
    }

    @Test
    void testUpdate() throws Exception {

        AutomationCompositions automationCompositionsCreate =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Update");

        var automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_UPDATE_JSON, "Update");
        instantiationProvider.createAutomationCompositions(automationCompositionsCreate);

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(automationCompositions));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositions);

        for (AutomationComposition automationCompositionUpdate : automationCompositions
            .getAutomationCompositionList()) {
            AutomationCompositions automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(
                automationCompositionUpdate.getKey().getName(), automationCompositionUpdate.getKey().getVersion());

            assertNotNull(automationCompositionsFromDb);
            assertThat(automationCompositionsFromDb.getAutomationCompositionList()).hasSize(1);
            assertEquals(automationCompositionUpdate,
                automationCompositionsFromDb.getAutomationCompositionList().get(0));
        }
    }

    @Test
    void testDelete() throws Exception {

        AutomationCompositions automationCompositionsFromRsc =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");

        instantiationProvider.createAutomationCompositions(automationCompositionsFromRsc);

        for (AutomationComposition automationCompositionFromRsc : automationCompositionsFromRsc
            .getAutomationCompositionList()) {
            Invocation.Builder invocationBuilder =
                super.sendRequest(INSTANTIATION_ENDPOINT + "?name=" + automationCompositionFromRsc.getKey().getName()
                    + "&version=" + automationCompositionFromRsc.getKey().getVersion());
            Response resp = invocationBuilder.delete();
            assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
            InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
            InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);

            AutomationCompositions automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(
                automationCompositionFromRsc.getKey().getName(), automationCompositionFromRsc.getKey().getVersion());
            assertThat(automationCompositionsFromDb.getAutomationCompositionList()).isEmpty();
        }
    }

    @Test
    void testDeleteBadRequest() throws Exception {

        AutomationCompositions automationCompositionsFromRsc =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "DelBadRequest");

        instantiationProvider.createAutomationCompositions(automationCompositionsFromRsc);

        for (AutomationComposition automationCompositionFromRsc : automationCompositionsFromRsc
            .getAutomationCompositionList()) {
            Invocation.Builder invocationBuilder =
                super.sendRequest(INSTANTIATION_ENDPOINT + "?name=" + automationCompositionFromRsc.getKey().getName());
            Response resp = invocationBuilder.delete();
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testCommand_NotFound1() {
        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_COMMAND_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(new InstantiationCommand()));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void testCommand_NotFound2() throws Exception {
        InstantiationCommand command =
            InstantiationUtils.getInstantiationCommandFromResource(AC_INSTANTIATION_CHANGE_STATE_JSON, "Command");
        command.setOrderedState(null);

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_COMMAND_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(command));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void testCommand() throws Exception {
        var automationCompositions =
            InstantiationUtils.getAutomationCompositionsFromResource(AC_INSTANTIATION_CREATE_JSON, "Command");
        instantiationProvider.createAutomationCompositions(automationCompositions);

        var participants = CommonTestData.createParticipants();
        for (var participant : participants) {
            participantProvider.saveParticipant(participant);
        }

        InstantiationCommand command =
            InstantiationUtils.getInstantiationCommandFromResource(AC_INSTANTIATION_CHANGE_STATE_JSON, "Command");

        Invocation.Builder invocationBuilder = super.sendRequest(INSTANTIATION_COMMAND_ENDPOINT);
        Response resp = invocationBuilder.put(Entity.json(command));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
        InstantiationResponse instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, command);

        // check passive state on DB
        for (ToscaConceptIdentifier toscaConceptIdentifier : command.getAutomationCompositionIdentifierList()) {
            AutomationCompositions automationCompositionsGet = instantiationProvider
                .getAutomationCompositions(toscaConceptIdentifier.getName(), toscaConceptIdentifier.getVersion());
            assertThat(automationCompositionsGet.getAutomationCompositionList()).hasSize(1);
            assertEquals(command.getOrderedState(),
                automationCompositionsGet.getAutomationCompositionList().get(0).getOrderedState());
        }
    }

    private synchronized void deleteEntryInDB() throws Exception {
        automationCompositionRepository.deleteAll();
        var list = serviceTemplateProvider.getAllServiceTemplates();
        if (!list.isEmpty()) {
            serviceTemplateProvider.deleteServiceTemplate(list.get(0).getName(), list.get(0).getVersion());
        }
    }

    private synchronized void createEntryInDB() throws Exception {
        deleteEntryInDB();
        serviceTemplateProvider.createServiceTemplate(serviceTemplate);
    }
}
