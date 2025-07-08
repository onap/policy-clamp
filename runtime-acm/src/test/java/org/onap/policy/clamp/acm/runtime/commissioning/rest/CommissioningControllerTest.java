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

package org.onap.policy.clamp.acm.runtime.commissioning.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_VERSIONING;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.util.CommonTestData;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionDefinition;
import org.onap.policy.clamp.models.acm.concepts.ParticipantSupportedElementType;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.AcTypeStateUpdate;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.PrimeOrder;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantProvider;
import org.onap.policy.clamp.models.acm.utils.AcmUtils;
import org.onap.policy.models.base.PfKey;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "default"})
class CommissioningControllerTest extends CommonRestController {

    private static final int NUMBER_COMPOSITIONS = 10;
    private static final String COMMISSIONING_ENDPOINT = "compositions";
    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    @Autowired
    private AcDefinitionProvider acDefinitionProvider;

    @Autowired
    private ParticipantProvider participantProvider;

    @LocalServerPort
    private int randomServerPort;

    /**
     * starts Main and inserts a commissioning template.
     */
    @BeforeAll
    static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
    }

    @BeforeEach
    void setUpPort() {
        super.setHttpPrefix(randomServerPort);
    }

    @AfterEach
    void after() {
        super.client.close();
    }

    @Test
    void testSwagger() {
        super.testSwagger(COMMISSIONING_ENDPOINT);
    }

    @Test
    void testUnauthorizedCreate() {
        assertUnauthorizedPost(COMMISSIONING_ENDPOINT, Entity.json(serviceTemplate));
    }

    @Test
    void testUnauthorizedQuery() {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT);
    }

    @Test
    void testUnauthorizedDelete() {
        assertUnauthorizedDelete(COMMISSIONING_ENDPOINT);
    }

    @Test
    void testCreateBadRequest() {
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        var resp = invocationBuilder.post(Entity.json("NotToscaServiceTemplate"));

        assertThat(Response.Status.BAD_REQUEST.getStatusCode()).isEqualTo(resp.getStatus());
        var commissioningResponse = resp.readEntity(CommissioningResponse.class);
        assertThat(commissioningResponse.getErrorDetails())
            .contains("org.springframework.http.converter.HttpMessageNotReadableException");
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).isNull();
        resp.close();
    }

    @Test
    void testCreateBadVersion() {
        var serviceTemplateCreate = new ToscaServiceTemplate(serviceTemplate);
        var x = new ToscaNodeTemplate(serviceTemplateCreate
            .getToscaTopologyTemplate().getNodeTemplates().values().iterator().next());
        x.setVersion("1.0.wrong");
        serviceTemplateCreate.getToscaTopologyTemplate().getNodeTemplates().put(x.getName(), x);

        var commissioningResponse = createServiceTemplate(serviceTemplateCreate, Response.Status.INTERNAL_SERVER_ERROR);
        assertThat(commissioningResponse.getErrorDetails())
            .isEqualTo("java.lang.IllegalArgumentException parameter "
                + "\"version\": value \"1.0.wrong\", does not match regular expression \""
                + PfKey.VERSION_REGEXP + "\"");
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).isNull();
    }

    @Test
    void testCreate() {
        var serviceTemplateCreate = new ToscaServiceTemplate(serviceTemplate);
        serviceTemplateCreate.setName("Create");
        var commissioningResponse = createServiceTemplate(serviceTemplateCreate, Response.Status.CREATED);
        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).hasSize(7);
        for (var nodeTemplateName : serviceTemplateCreate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedAutomationCompositionDefinitions().stream()
                .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }
    }

    @Test
    void testVersioning() {
        var serviceTemplateCreate = InstantiationUtils.getToscaServiceTemplate(TOSCA_VERSIONING);
        var commissioningResponse = createServiceTemplate(serviceTemplateCreate, Response.Status.CREATED);
        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).hasSize(11);
        assertNotNull(serviceTemplateCreate);
        for (var nodeTemplateName : serviceTemplateCreate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedAutomationCompositionDefinitions().stream()
                .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }
    }

    private CommissioningResponse createServiceTemplate(ToscaServiceTemplate serviceTemplateCreate,
                                                        Response.Status statusExpected) {
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        try (var resp = invocationBuilder.post(Entity.json(serviceTemplateCreate))) {
            assertEquals(statusExpected.getStatusCode(), resp.getStatus());
            return resp.readEntity(CommissioningResponse.class);
        }
    }

    @Test
    void testUpdate() {
        var toscaDataType = new ToscaDataType();
        toscaDataType.setName("org.onap.datatypes.policy.clamp.Configuration");
        toscaDataType.setDerivedFrom("tosca.datatypes.Root");
        toscaDataType.setProperties(new HashMap<>());
        var toscaProperty = new ToscaProperty();
        toscaProperty.setName("configurationEntityId");
        toscaProperty.setType("onap.datatypes.ToscaConceptIdentifier");
        toscaDataType.getProperties().put(toscaProperty.getName(), toscaProperty);

        var compositionId = createEntryInDB("forUpdate");
        var serviceTemplateUpdate = new ToscaServiceTemplate(serviceTemplate);
        serviceTemplateUpdate.getDataTypes().put(toscaDataType.getName(), toscaDataType);
        serviceTemplateUpdate.setMetadata(Map.of("compositionId", compositionId));

        var commissioningResponse = createServiceTemplate(serviceTemplateUpdate, Response.Status.OK);
        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).hasSize(7);
        for (var nodeTemplateName : serviceTemplateUpdate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedAutomationCompositionDefinitions().stream()
                .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }

        var entity = getServiceTemplate(COMMISSIONING_ENDPOINT + "/" + compositionId);
        assertThat(entity.getServiceTemplate().getDataTypes()).containsKey(toscaDataType.getName());
    }

    private AutomationCompositionDefinition getServiceTemplate(String url) {
        var invocationBuilder = super.sendRequest(url);
        try (var resp = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
            return resp.readEntity(AutomationCompositionDefinition.class);
        }
    }

    @Test
    void testQuery_NoResultWithThisName() {
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "?name=noResultWithThisName");
        try (var rawresp = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            var entityList = rawresp.readEntity(ToscaServiceTemplate.class);
            assertThat(entityList.getNodeTypes()).isNull();
        }
    }

    @Test
    void testQuery() {
        createEntryInDB("forQuery");

        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        try (var rawresp = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
            var entityList = rawresp.readEntity(ToscaServiceTemplates.class);
            assertNotNull(entityList);
            assertThat(entityList.getServiceTemplates()).isNotEmpty();
        }
    }

    @Test
    void testQueryPageable() {
        for (var i = 0; i < NUMBER_COMPOSITIONS; i++) {
            createEntryInDB("queryPageable" + i);
        }
        // filter by name with wrong name
        validateQueryPageable("?name=wrongName", 0);

        // filter by name
        validateQueryPageable("?name=queryPageable1", 1);

        // page=1 and size=5
        validateQueryPageable("?page=1&size=5", 5);

        // not pageable
        validateQueryNotPageable("?size=4");
        validateQueryNotPageable("?page=2");
        validateQueryNotPageable("");
    }

    private void validateQueryNotPageable(String parameters) {
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + parameters);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            var entityList = response.readEntity(ToscaServiceTemplates.class);
            assertThat(entityList.getServiceTemplates()).hasSizeGreaterThanOrEqualTo(NUMBER_COMPOSITIONS);
        }
    }

    private void validateQueryPageable(String parameters, int size) {
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + parameters);
        try (var response = invocationBuilder.buildGet().invoke()) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            var entityList = response.readEntity(ToscaServiceTemplates.class);
            assertThat(entityList.getServiceTemplates()).hasSize(size);
        }
    }

    @Test
    void testDeleteBadRequest() {
        createEntryInDB("DeleteBadRequest");
        deleteServiceTemplate(COMMISSIONING_ENDPOINT + "/" + UUID.randomUUID(), Response.Status.NOT_FOUND);
    }

    private void deleteServiceTemplate(String url, Response.Status statusExpected) {
        var invocationBuilder = super.sendRequest(url);
        try (var resp = invocationBuilder.delete()) {
            assertEquals(statusExpected.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testDelete() {
        var compositionId = createEntryInDB("forDelete");
        deleteServiceTemplate(COMMISSIONING_ENDPOINT + "/" + compositionId, Response.Status.OK);

        var templatesInDB = acDefinitionProvider.findAcDefinition(compositionId);
        assertThat(templatesInDB).isEmpty();
    }

    @Test
    void testPrime() {
        var compositionId = createEntryInDB("Prime");
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/" + compositionId);

        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate,
            "org.onap.policy.clamp.acm.AutomationCompositionElement");
        for (var elementEntry : acElements) {
            var participant = CommonTestData.createParticipant(UUID.randomUUID());
            var supportedElementType = new ParticipantSupportedElementType();
            supportedElementType.setTypeName(elementEntry.getValue().getType());
            supportedElementType.setTypeVersion(elementEntry.getValue().getTypeVersion());
            participant.getParticipantSupportedElementTypes().put(supportedElementType.getId(), supportedElementType);
            var replica = CommonTestData.createParticipantReplica(UUID.randomUUID());
            participant.getReplicas().put(replica.getReplicaId(), replica);
            participantProvider.saveParticipant(participant);
        }

        var body = new AcTypeStateUpdate();
        body.setPrimeOrder(PrimeOrder.PRIME);
        try (var resp = invocationBuilder.put(Entity.json(body))) {
            assertEquals(Response.Status.ACCEPTED.getStatusCode(), resp.getStatus());
        }
    }

    @Test
    void testPrimeBadRequest() {
        var compositionId = createEntryInDB("Prime");
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/" + compositionId);

        var acElements = AcmUtils.extractAcElementsFromServiceTemplate(serviceTemplate,
                "org.onap.policy.clamp.acm.AutomationCompositionElement");
        for (var elementEntry : acElements) {
            var participant = CommonTestData.createParticipant(UUID.randomUUID());
            var supportedElementType = new ParticipantSupportedElementType();
            supportedElementType.setTypeName(elementEntry.getValue().getType());
            supportedElementType.setTypeVersion(elementEntry.getValue().getTypeVersion());
            participant.getParticipantSupportedElementTypes().put(supportedElementType.getId(), supportedElementType);
            participantProvider.saveParticipant(participant);
        }

        var body = new AcTypeStateUpdate();
        body.setPrimeOrder(PrimeOrder.PRIME);
        try (var resp = invocationBuilder.put(Entity.json(body))) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), resp.getStatus());
        }
    }

    private UUID createEntryInDB(String name) {
        var serviceTemplateCreate = new ToscaServiceTemplate(serviceTemplate);
        serviceTemplateCreate.setName(name);
        var acmDefinition = acDefinitionProvider
            .createAutomationCompositionDefinition(serviceTemplateCreate, CommonTestData.TOSCA_ELEMENT_NAME,
                CommonTestData.TOSCA_COMP_NAME);

        return acmDefinition.getCompositionId();
    }

}
