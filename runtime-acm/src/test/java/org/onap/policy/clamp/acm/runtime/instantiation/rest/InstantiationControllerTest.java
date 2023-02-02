/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.instantiation.AutomationCompositionInstantiationProvider;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.main.rest.InstantiationController;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AcTypeState;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositions;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
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

    private static final String INSTANTIATION_ENDPOINT = "compositions/%s/instances";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @Autowired
    private AcDefinitionProvider acDefinitionProvider;

    @Autowired
    private AutomationCompositionInstantiationProvider instantiationProvider;

    @LocalServerPort
    private int randomServerPort;

    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @BeforeEach
    public void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    private String getInstanceEndPoint(UUID compositionId) {
        return String.format(INSTANTIATION_ENDPOINT, compositionId.toString());
    }

    private String getInstanceEndPoint(UUID compositionId, UUID instanceId) {
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

        assertUnauthorizedPost(getInstanceEndPoint(UUID.randomUUID()), Entity.json(automationComposition));
    }

    @Test
    void testQuery_Unauthorized() {
        assertUnauthorizedGet(getInstanceEndPoint(UUID.randomUUID()));
    }

    @Test
    void testUpdate_Unauthorized() {
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Unauthorized");

        assertUnauthorizedPut(getInstanceEndPoint(UUID.randomUUID()), Entity.json(automationComposition));
    }

    @Test
    void testDelete_Unauthorized() {
        assertUnauthorizedDelete(getInstanceEndPoint(UUID.randomUUID()));
    }

    @Test
    void testCreate() {
        var compositionId = createAcDefinitionInDB("Create");
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Create");
        automationCompositionFromRsc.setCompositionId(compositionId);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId));
        var resp = invocationBuilder.post(Entity.json(automationCompositionFromRsc));
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        var instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);
        automationCompositionFromRsc.setInstanceId(instResponse.getInstanceId());
        automationCompositionFromRsc.getElements().values()
                .forEach(element -> element.setParticipantId(CommonTestData.getParticipantId()));

        var automationCompositionFromDb =
                instantiationProvider.getAutomationComposition(compositionId, instResponse.getInstanceId());

        assertNotNull(automationCompositionFromDb);
        assertEquals(automationCompositionFromRsc, automationCompositionFromDb);
    }

    @Test
    void testCreateBadRequest() {
        var compositionId = createAcDefinitionInDB("CreateBadRequest");
        var automationCompositionFromRsc = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "CreateBadRequest");
        automationCompositionFromRsc.setCompositionId(compositionId);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId));
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
        var invocationBuilder =
                super.sendRequest(getInstanceEndPoint(UUID.randomUUID()) + "?name=noResultWithThisName");
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var resp = rawresp.readEntity(AutomationCompositions.class);
        assertThat(resp.getAutomationCompositionList()).isEmpty();
    }

    @Test
    void testQuery() {
        var compositionId = createAcDefinitionInDB("Query");
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        automationComposition.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var invocationBuilder = super.sendRequest(
                getInstanceEndPoint(compositionId) + "?name=" + automationComposition.getKey().getName());
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var automationCompositionsQuery = rawresp.readEntity(AutomationCompositions.class);
        assertNotNull(automationCompositionsQuery);
        assertThat(automationCompositionsQuery.getAutomationCompositionList()).hasSize(1);
        assertEquals(automationComposition, automationCompositionsQuery.getAutomationCompositionList().get(0));
    }

    @Test
    void testGet() {
        var compositionId = createAcDefinitionInDB("Get");
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Get");
        automationComposition.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var invocationBuilder = super.sendRequest(
                getInstanceEndPoint(compositionId, automationComposition.getInstanceId()));
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var automationCompositionGet = rawresp.readEntity(AutomationComposition.class);
        assertNotNull(automationCompositionGet);
        assertEquals(automationComposition, automationCompositionGet);
    }

    @Test
    void testUpdate() {
        var compositionId = createAcDefinitionInDB("Update");
        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Update");
        automationCompositionCreate.setCompositionId(compositionId);

        var response = instantiationProvider.createAutomationComposition(compositionId, automationCompositionCreate);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Update");
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(response.getInstanceId());
        automationComposition.getElements().values()
                .forEach(element -> element.setParticipantId(CommonTestData.getParticipantId()));

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId));
        var resp = invocationBuilder.post(Entity.json(automationComposition));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        var instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationComposition);

        var automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(compositionId,
                automationComposition.getKey().getName(), automationComposition.getKey().getVersion());

        assertNotNull(automationCompositionsFromDb);
        assertThat(automationCompositionsFromDb.getAutomationCompositionList()).hasSize(1);
        assertEquals(automationComposition, automationCompositionsFromDb.getAutomationCompositionList().get(0));
    }

    @Test
    void testDelete() {
        var compositionId = createAcDefinitionInDB("Delete");
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");
        automationCompositionFromRsc.setCompositionId(compositionId);

        var instResponse =
                instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, instResponse.getInstanceId()));
        var resp = invocationBuilder.delete();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        instResponse = resp.readEntity(InstantiationResponse.class);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);

        var automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(compositionId,
                automationCompositionFromRsc.getKey().getName(), automationCompositionFromRsc.getKey().getVersion());
        assertThat(automationCompositionsFromDb.getAutomationCompositionList()).isEmpty();
    }

    @Test
    void testDeleteNotFound() {
        var compositionId = createAcDefinitionInDB("DeleteNotFound");
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "DelNotFound");
        automationCompositionFromRsc.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, UUID.randomUUID()));
        var resp = invocationBuilder.delete();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    void testDeploy_NotFound() {
        var compositionId = createAcDefinitionInDB("Deploy_NotFound");
        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, UUID.randomUUID()));
        var resp = invocationBuilder.put(Entity.json(new AcInstanceStateUpdate()));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    void testDeploy_BadRequest() {
        var compositionId = createAcDefinitionInDB("Deploy_BadRequest");
        var acFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "BadRequest");
        acFromRsc.setCompositionId(compositionId);

        var instResponse = instantiationProvider.createAutomationComposition(compositionId, acFromRsc);

        var command = new AcInstanceStateUpdate();
        command.setDeployOrder(null);
        command.setLockOrder(null);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, instResponse.getInstanceId()));
        var resp = invocationBuilder.put(Entity.json(command));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void testDeploy() {
        var compositionId = createAcDefinitionInDB("Deploy");
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Command");
        automationComposition.setCompositionId(compositionId);
        var instResponse = instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var instantiationUpdate = new AcInstanceStateUpdate();
        instantiationUpdate.setDeployOrder(DeployOrder.DEPLOY);
        instantiationUpdate.setLockOrder(null);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, instResponse.getInstanceId()));
        var resp = invocationBuilder.put(Entity.json(instantiationUpdate));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
    }

    private UUID createAcDefinitionInDB(String name) {
        var serviceTemplateCreate = new ToscaServiceTemplate(serviceTemplate);
        serviceTemplateCreate.setName(name);
        var acmDefinition = CommonTestData.createAcDefinition(serviceTemplate, AcTypeState.PRIMED);
        acDefinitionProvider.updateAcDefinition(acmDefinition);
        return acmDefinition.getCompositionId();
    }
}
