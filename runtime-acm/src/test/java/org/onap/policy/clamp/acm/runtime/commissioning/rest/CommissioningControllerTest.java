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

package org.onap.policy.clamp.acm.runtime.commissioning.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_SERVICE_TEMPLATE_YAML;

import java.util.HashMap;
import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.onap.policy.clamp.acm.runtime.instantiation.InstantiationUtils;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.models.acm.persistence.provider.AcDefinitionProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaDataType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaProperty;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class CommissioningControllerTest extends CommonRestController {

    private static final String COMMISSIONING_ENDPOINT = "compositions";
    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
    private UUID compositionId;

    @Autowired
    private AcDefinitionProvider acDefinitionProvider;

    @LocalServerPort
    private int randomServerPort;

    /**
     * starts Main and inserts a commissioning template.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
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
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json("NotToscaServiceTempalte"));

        assertThat(Response.Status.BAD_REQUEST.getStatusCode()).isEqualTo(resp.getStatus());
        CommissioningResponse commissioningResponse = resp.readEntity(CommissioningResponse.class);
        assertThat(commissioningResponse.getErrorDetails()).isNotNull();
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).isNull();
    }

    @Test
    void testCreate() {
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(serviceTemplate));
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        CommissioningResponse commissioningResponse = resp.readEntity(CommissioningResponse.class);
        compositionId = commissioningResponse.getCompositionId();
        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).hasSize(7);
        for (String nodeTemplateName : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedAutomationCompositionDefinitions().stream()
                    .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }
    }

    @Test
    void testUpdate() {
        var invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        var resp = invocationBuilder.post(Entity.json(serviceTemplate));
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        var commissioningResponse = resp.readEntity(CommissioningResponse.class);
        compositionId = commissioningResponse.getCompositionId();

        var toscaDataType = new ToscaDataType();
        toscaDataType.setName("org.onap.datatypes.policy.clamp.Configuration");
        toscaDataType.setDerivedFrom("tosca.datatypes.Root");
        toscaDataType.setProperties(new HashMap<>());
        var toscaProperty = new ToscaProperty();
        toscaProperty.setName("configurationEntityId");
        toscaProperty.setType("onap.datatypes.ToscaConceptIdentifier");
        toscaDataType.getProperties().put(toscaProperty.getName(), toscaProperty);

        serviceTemplate.getDataTypes().put(toscaDataType.getName(), toscaDataType);
        invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/" + compositionId);
        resp = invocationBuilder.put(Entity.json(serviceTemplate));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        commissioningResponse = resp.readEntity(CommissioningResponse.class);
        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).hasSize(7);
        for (String nodeTemplateName : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedAutomationCompositionDefinitions().stream()
                    .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }

        invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/" + compositionId);
        resp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        var entity = resp.readEntity(ToscaServiceTemplate.class);
        assertThat(entity.getDataTypes()).containsKey(toscaDataType.getName());
    }

    @Test
    void testQuery_NoResultWithThisName() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "?name=noResultWithThisName");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var entityList = rawresp.readEntity(ToscaServiceTemplate.class);
        assertThat(entityList.getNodeTypes()).isNull();
    }

    @Test
    void testQuery() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        var entityList = rawresp.readEntity(ToscaServiceTemplate.class);
        assertNotNull(entityList);
    }

    @Test
    void testDeleteBadRequest() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/" + UUID.randomUUID());
        // Call delete with no info
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resp.getStatus());
    }

    @Test
    void testDelete() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/" + compositionId);
        // Call delete with no info
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        var templatesInDB = acDefinitionProvider.getAllAcDefinitions();
        assertThat(templatesInDB).isEmpty();
    }

    private synchronized void createEntryInDB() throws Exception {
        deleteEntryInDB();
        var acmDefinition = acDefinitionProvider.createAutomationCompositionDefinition(serviceTemplate);
        compositionId = acmDefinition.getCompositionId();
    }

    // Delete entries from the DB after relevant tests
    private synchronized void deleteEntryInDB() throws Exception {
        var list = acDefinitionProvider.getAllAcDefinitions();
        if (!list.isEmpty()) {
            acDefinitionProvider.deleteAcDefintion(compositionId);
        }
    }
}
