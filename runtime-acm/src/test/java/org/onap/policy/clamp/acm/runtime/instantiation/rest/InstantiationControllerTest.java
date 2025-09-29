/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_VERSIONING;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
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
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.AcInstanceStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.InstantiationResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Class to perform unit test of {@link InstantiationController}}.
 *
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({ "test", "default" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InstantiationControllerTest extends CommonRestController {

    private static final int NUMBER_INSTANCES = 10;
    private static final String AC_INSTANTIATION_CREATE_JSON = "src/test/resources/rest/acm/AutomationComposition.json";
    private static final String AC_VERSIONING_YAML = "src/test/resources/rest/acm/AutomationCompositionVersioning.yaml";

    private static final String AC_INSTANTIATION_UPDATE_JSON =
            "src/test/resources/rest/acm/AutomationCompositionUpdate.json";

    private static final String INSTANTIATION_ENDPOINT = "compositions/%s/instances";

    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    private static final String NODE_TYPE = "org.onap.policy.clamp.acm.AutomationComposition";

    @Autowired
    private AcDefinitionProvider acDefinitionProvider;

    @Autowired
    private ParticipantProvider participantProvider;

    @Autowired
    private AutomationCompositionInstantiationProvider instantiationProvider;

    @LocalServerPort
    private int randomServerPort;

    @BeforeAll
    static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @BeforeEach
    void setUpPort() {
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
        assertNotNull(automationCompositionFromRsc);
        automationCompositionFromRsc.setCompositionId(compositionId);

        var instResponse = createAutomationComposition(compositionId, automationCompositionFromRsc,
            Response.Status.CREATED);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);
        automationCompositionFromRsc.setInstanceId(instResponse.getInstanceId());
        automationCompositionFromRsc.getElements().values()
                .forEach(element -> element.setParticipantId(CommonTestData.getParticipantId()));

        var automationCompositionFromDb =
                instantiationProvider.getAutomationComposition(compositionId, instResponse.getInstanceId());
        automationCompositionFromRsc.setLastMsg(automationCompositionFromDb.getLastMsg());

        assertNotNull(automationCompositionFromDb);
        assertEquals(automationCompositionFromRsc, automationCompositionFromDb);
    }

    private InstantiationResponse createAutomationComposition(UUID compositionId,
        AutomationComposition automationComposition, Response.Status statusExpected) {
        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId));
        try (var resp = invocationBuilder.post(Entity.json(automationComposition))) {
            assertEquals(statusExpected.getStatusCode(), resp.getStatus());
            return resp.readEntity(InstantiationResponse.class);
        }
    }

    @Test
    void testCreateBadRequest() {
        var compositionId = createAcDefinitionInDB("CreateBadRequest");
        var automationCompositionFromRsc = InstantiationUtils
                .getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "CreateBadRequest");
        assertNotNull(automationCompositionFromRsc);
        automationCompositionFromRsc.setCompositionId(compositionId);

        createAutomationComposition(compositionId, automationCompositionFromRsc, Response.Status.CREATED);

        // testing Bad Request: AC already defined
        var instResponse = createAutomationComposition(compositionId, automationCompositionFromRsc,
            Response.Status.BAD_REQUEST);
        assertNotNull(instResponse.getErrorDetails());
        assertNull(instResponse.getAffectedAutomationComposition());
    }

    @Test
    void testVersioning() {
        var serviceTemplateVer = InstantiationUtils.getToscaServiceTemplate(TOSCA_VERSIONING);
        var compositionId = createAcDefinitionInDB(serviceTemplateVer);
        var automationCompositionFromRsc = InstantiationUtils
            .getAutomationCompositionFromYaml(AC_VERSIONING_YAML, "Versioning");
        assertNotNull(automationCompositionFromRsc);
        automationCompositionFromRsc.setCompositionId(compositionId);
        var instResponse =
            createAutomationComposition(compositionId, automationCompositionFromRsc, Response.Status.CREATED);
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);
    }

    @Test
    void testQuery_NoResultWithThisName() {
        var invocationBuilder =
                super.sendRequest(getInstanceEndPoint(UUID.randomUUID()) + "?name=noResultWithThisName");
        try (var rawresp = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            var resp = rawresp.readEntity(AutomationCompositions.class);
            assertThat(resp.getAutomationCompositionList()).isEmpty();
        }
    }

    @Test
    void testQuery() {
        var compositionId = createAcDefinitionInDB("Query");
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var invocationBuilder = super.sendRequest(
                getInstanceEndPoint(compositionId) + "?name=" + automationComposition.getKey().getName());
        try (var rawresp = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            var automationCompositionsQuery = rawresp.readEntity(AutomationCompositions.class);
            assertNotNull(automationCompositionsQuery);
            assertThat(automationCompositionsQuery.getAutomationCompositionList()).hasSize(1);
            var automationCompositionRc = automationCompositionsQuery.getAutomationCompositionList().get(0);
            automationComposition.setLastMsg(automationCompositionRc.getLastMsg());
            assertEquals(automationComposition, automationCompositionRc);
        }
    }

    @Test
    void testQueryPageable() {
        var compositionId = createAcDefinitionInDB("Query");
        var automationComposition =
            InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);
        for (var i = 0; i < NUMBER_INSTANCES; i++) {
            automationComposition.setName("acm_" + i);
            instantiationProvider.createAutomationComposition(compositionId, automationComposition);
        }
        var endpoint = getInstanceEndPoint(compositionId);
        validateQueryPageable(endpoint + "?name=wrong_name", 0);
        validateQueryPageable(endpoint + "?name=acm_1", 1);
        validateQueryPageable(endpoint + "?page=1&size=4", 4);

        validateQueryNotPageable(endpoint + "?page=0");
        validateQueryNotPageable(endpoint + "?size=5");
        validateQueryNotPageable(endpoint);
    }

    private void validateQueryNotPageable(String link) {
        var invocationBuilder = super.sendRequest(link);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            var resultList = response.readEntity(AutomationCompositions.class);
            assertNotNull(resultList);
            assertThat(resultList.getAutomationCompositionList()).hasSizeGreaterThanOrEqualTo(NUMBER_INSTANCES);
        }
    }

    private void validateQueryPageable(String link, int size) {
        var invocationBuilder = super.sendRequest(link);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            var resultList = response.readEntity(AutomationCompositions.class);
            assertNotNull(resultList);
            assertThat(resultList.getAutomationCompositionList()).hasSize(size);
        }
    }

    @Test
    void testGet() {
        var compositionId = createAcDefinitionInDB("Get");
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Get");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var invocationBuilder = super.sendRequest(
                getInstanceEndPoint(compositionId, automationComposition.getInstanceId()));
        var rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var automationCompositionGet = rawresp.readEntity(AutomationComposition.class);
        rawresp.close();
        assertNotNull(automationCompositionGet);
        automationComposition.setLastMsg(automationCompositionGet.getLastMsg());
        assertEquals(automationComposition, automationCompositionGet);
    }

    @Test
    void testUpdate() {
        var compositionId = createAcDefinitionInDB("Update");
        var automationCompositionCreate =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Update");
        assertNotNull(automationCompositionCreate);
        automationCompositionCreate.setCompositionId(compositionId);

        var response = instantiationProvider.createAutomationComposition(compositionId, automationCompositionCreate);

        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_UPDATE_JSON, "Update");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);
        automationComposition.setInstanceId(response.getInstanceId());
        automationComposition.getElements().values()
                .forEach(element -> element.setParticipantId(CommonTestData.getParticipantId()));

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId));
        try (var resp = invocationBuilder.post(Entity.json(automationComposition))) {
            assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

            var instResponse = resp.readEntity(InstantiationResponse.class);
            InstantiationUtils.assertInstantiationResponse(instResponse, automationComposition);
        }
        var automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(
                compositionId, automationComposition.getKey().getName(),
                automationComposition.getKey().getVersion(), Pageable.unpaged());

        assertNotNull(automationCompositionsFromDb);
        assertThat(automationCompositionsFromDb.getAutomationCompositionList()).hasSize(1);
        var acFromDb = automationCompositionsFromDb.getAutomationCompositionList().get(0);
        automationComposition.setLastMsg(acFromDb.getLastMsg());
        automationComposition.setRevisionId(acFromDb.getRevisionId());
        assertEquals(automationComposition, acFromDb);
    }

    @Test
    void testDelete() {
        var compositionId = createAcDefinitionInDB("Delete");
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Delete");
        assertNotNull(automationCompositionFromRsc);
        automationCompositionFromRsc.setCompositionId(compositionId);

        var instResponse =
                instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, instResponse.getInstanceId()));
        try (var resp = invocationBuilder.delete()) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
            instResponse = resp.readEntity(InstantiationResponse.class);
        }
        InstantiationUtils.assertInstantiationResponse(instResponse, automationCompositionFromRsc);

        var automationCompositionsFromDb = instantiationProvider.getAutomationCompositions(compositionId,
                automationCompositionFromRsc.getKey().getName(), automationCompositionFromRsc.getKey().getVersion(),
                Pageable.unpaged());
        assertEquals(DeployState.DELETING,
                automationCompositionsFromDb.getAutomationCompositionList().get(0).getDeployState());
    }

    @Test
    void testDeleteNotFound() {
        var compositionId = createAcDefinitionInDB("DeleteNotFound");
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "DelNotFound");
        assertNotNull(automationCompositionFromRsc);
        automationCompositionFromRsc.setCompositionId(compositionId);

        instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, UUID.randomUUID()));
        try (var resp = invocationBuilder.delete()) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testRollbackNotValid() {
        var compositionId = createAcDefinitionInDB("RollbackNotFound");

        // instance not found
        var url = getInstanceEndPoint(compositionId, UUID.randomUUID()) + "/rollback";
        var invocationBuilder = super.sendRequest(url);
        try (var resp = invocationBuilder.post(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
        }

        // instance not valid state
        var automationCompositionFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "NotValid");
        assertNotNull(automationCompositionFromRsc);
        automationCompositionFromRsc.setCompositionId(compositionId);
        var instanceResponse =
                instantiationProvider.createAutomationComposition(compositionId, automationCompositionFromRsc);
        url = getInstanceEndPoint(compositionId, instanceResponse.getInstanceId()) + "/rollback";
        invocationBuilder = super.sendRequest(url);
        try (var resp = invocationBuilder.post(Entity.entity("", MediaType.APPLICATION_JSON))) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testDeploy_NotFound() {
        var compositionId = createAcDefinitionInDB("Deploy_NotFound");
        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, UUID.randomUUID()));
        try (var resp = invocationBuilder.put(Entity.json(new AcInstanceStateUpdate()))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testDeploy_BadRequest() {
        var compositionId = createAcDefinitionInDB("Deploy_BadRequest");
        var acFromRsc =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "BadRequest");
        assertNotNull(acFromRsc);
        acFromRsc.setCompositionId(compositionId);

        var instResponse = instantiationProvider.createAutomationComposition(compositionId, acFromRsc);

        var command = new AcInstanceStateUpdate();
        command.setDeployOrder(null);
        command.setLockOrder(null);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, instResponse.getInstanceId()));
        try (var resp = invocationBuilder.put(Entity.json(command))) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testDeploy() {
        var compositionId = createAcDefinitionInDB("Deploy");
        var automationComposition =
                InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Command");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);
        var instResponse = instantiationProvider.createAutomationComposition(compositionId, automationComposition);

        var instantiationUpdate = new AcInstanceStateUpdate();
        instantiationUpdate.setDeployOrder(DeployOrder.DEPLOY);
        instantiationUpdate.setLockOrder(null);

        var invocationBuilder = super.sendRequest(getInstanceEndPoint(compositionId, instResponse.getInstanceId()));
        try (var resp = invocationBuilder.put(Entity.json(instantiationUpdate))) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void test_queryCompositionInstancesByFilter_WithoutInstanceIds() {
        // test setup
        var compositionId = createAcDefinitionInDB("Query");
        var automationComposition =
            InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);
        for (var i = 0; i < NUMBER_INSTANCES; i++) {
            automationComposition.setName("acmr_" + i);
            instantiationProvider.createAutomationComposition(compositionId, automationComposition);
        }

        validateQueryPageable("instances", 10);
        validateQueryPageable("instances?page=1&size=4", 4);
        validateQueryPageable("instances?size=4", 10); // only works if page is also informed, so listAll
        validateQueryPageable("instances?stateChangeResult=FAILED,TIMEOUT", 0);
        validateQueryPageable("instances?deployState=UNDEPLOYED", 10);
        validateQueryPageable("instances?stateChangeResult=NO_ERROR&deployState=UNDEPLOYED", 10);
        validateQueryPageable("instances?sort=name&sortOrder=DESC", 10);
    }

    @Test
    void test_queryCompositionInstancesByFilter_WithInstanceIds() {
        // test setup
        var compositionId = createAcDefinitionInDB("Query");
        var automationComposition =
            InstantiationUtils.getAutomationCompositionFromResource(AC_INSTANTIATION_CREATE_JSON, "Query");
        assertNotNull(automationComposition);
        automationComposition.setCompositionId(compositionId);
        var instanceIds = new StringBuffer();
        for (var i = 0; i < NUMBER_INSTANCES; i++) {
            automationComposition.setName("acmr_" + i);
            var result = instantiationProvider.createAutomationComposition(compositionId, automationComposition);
            if (i > 0) {
                instanceIds.append(",");
            }
            instanceIds.append(result.getInstanceId());
        }

        validateQueryPageable("instances?instanceIds=" + instanceIds, 10);
        validateQueryPageable("instances?page=1&size=4&instanceIds=" + instanceIds, 4);
        validateQueryPageable("instances?size=4&instanceIds=" + instanceIds, 10);
        validateQueryPageable("instances?stateChangeResult=FAILED,TIMEOUT&instanceIds=" + instanceIds, 0);
        validateQueryPageable("instances?deployState=UNDEPLOYED&instanceIds=" + instanceIds, 10);
        validateQueryPageable("instances?sort=name&sortOrder=DESC&instanceIds=" + instanceIds, 10);
    }

    private UUID createAcDefinitionInDB(String name) {
        var serviceTemplateCreate = new ToscaServiceTemplate(serviceTemplate);
        serviceTemplateCreate.setName(name);
        return createAcDefinitionInDB(serviceTemplateCreate);
    }

    private UUID createAcDefinitionInDB(ToscaServiceTemplate serviceTemplateCreate) {
        var acmDefinition = CommonTestData.createAcDefinition(serviceTemplateCreate, AcTypeState.PRIMED);
        acDefinitionProvider.updateAcDefinition(acmDefinition, NODE_TYPE);
        saveDummyParticipantInDb();
        return acmDefinition.getCompositionId();
    }

    private void saveDummyParticipantInDb() {
        var participant = CommonTestData.createParticipant(CommonTestData.getParticipantId());
        var replica = CommonTestData.createParticipantReplica(CommonTestData.getReplicaId());
        participant.getReplicas().put(replica.getReplicaId(), replica);
        participantProvider.saveParticipant(participant);
    }
}
