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
import static org.onap.policy.clamp.acm.runtime.util.CommonTestData.TOSCA_ST_TEMPLATE_YAML;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import java.util.List;
import java.util.Map;
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
import org.onap.policy.clamp.models.acm.persistence.provider.ServiceTemplateProvider;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
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

    private static final String COMMISSIONING_ENDPOINT = "commission";
    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();
    private static ToscaServiceTemplate commonPropertiesServiceTemplate = new ToscaServiceTemplate();

    @Autowired
    private ServiceTemplateProvider serviceTemplateProvider;

    @LocalServerPort
    private int randomServerPort;

    /**
     * starts Main and inserts a commissioning template.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        serviceTemplate = InstantiationUtils.getToscaServiceTemplate(TOSCA_SERVICE_TEMPLATE_YAML);
        commonPropertiesServiceTemplate =
            InstantiationUtils.getToscaServiceTemplate(TOSCA_ST_TEMPLATE_YAML);
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
    void testSwagger() throws Exception {
        super.testSwagger(COMMISSIONING_ENDPOINT);
    }

    @Test
    void testUnauthorizedCreate() throws Exception {
        assertUnauthorizedPost(COMMISSIONING_ENDPOINT, Entity.json(serviceTemplate));
    }

    @Test
    void testUnauthorizedQuery() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT);
    }

    @Test
    void testUnauthorizedQueryElements() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT + "/elements");
    }

    @Test
    void testUnauthorizedDelete() throws Exception {
        assertUnauthorizedDelete(COMMISSIONING_ENDPOINT);
    }

    @Test
    void testUnauthorizedQueryToscaServiceTemplate() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT + "/toscaservicetemplate");
    }

    @Test
    void testUnauthorizedQueryToscaServiceTemplateSchema() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT + "/toscaServiceTemplateSchema");
    }

    @Test
    void testUnauthorizedQueryToscaServiceCommonOrInstanceProperties() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT + "/getCommonOrInstanceProperties");
    }

    @Test
    void testQueryToscaServiceTemplate() throws Exception {
        createFullEntryInDbWithCommonProps();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/toscaservicetemplate");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        String template = rawresp.readEntity(String.class);
        final JsonObject jsonObject = new JsonParser().parse(template).getAsJsonObject();

        Gson gson = new Gson();
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ToscaNodeTemplate.class,
                (JsonSerializer<ToscaNodeTemplate>) (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()));
        builder.setPrettyPrinting();
        gson = builder.create();

        ToscaNodeTemplate toscaNodeTemplate = gson.fromJson(jsonObject, ToscaNodeTemplate.class);
        assertNotNull(toscaNodeTemplate);
    }

    @Test
    void testQueryToscaServiceTemplateSchema() throws Exception {
        createFullEntryInDbWithCommonProps();

        Invocation.Builder invocationBuilder =
            super.sendRequest(COMMISSIONING_ENDPOINT + "/toscaServiceTemplateSchema");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        String schema = rawresp.readEntity(String.class);
        assertNotNull(schema);
    }

    @Test
    void testQueryCommonOrInstanceProperties() throws Exception {
        createFullEntryInDbWithCommonProps();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT
            + "/getCommonOrInstanceProperties" + "?common=true&name=ToscaServiceTemplateSimple&version=1.0.0");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, ToscaNodeTemplate> commonProperties = rawresp.readEntity(Map.class);

        assertNotNull(commonProperties);
        assertThat(commonProperties).hasSize(5);
    }

    @Test
    void testCreateBadRequest() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json("NotToscaServiceTempalte"));

        assertThat(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).isEqualTo(resp.getStatus());
        CommissioningResponse commissioningResponse = resp.readEntity(CommissioningResponse.class);
        assertThat(commissioningResponse.getErrorDetails()).isNotNull();
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).isNull();
    }

    @Test
    void testCreate() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(serviceTemplate));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        CommissioningResponse commissioningResponse = resp.readEntity(CommissioningResponse.class);

        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedAutomationCompositionDefinitions()).hasSize(13);
        for (String nodeTemplateName : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedAutomationCompositionDefinitions().stream()
                .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }
    }

    @Test
    void testQuery_NoResultWithThisName() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "?name=noResultWithThisName");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        List<?> entityList = rawresp.readEntity(List.class);
        assertThat(entityList).isEmpty();
    }

    @Test
    void testQuery() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        List<?> entityList = rawresp.readEntity(List.class);
        assertNotNull(entityList);
        assertThat(entityList).hasSize(2);
    }

    @Test
    void testQueryElementsBadRequest() throws Exception {
        createEntryInDB();

        // Call get elements with no info
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/elements");
        Response resp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.NOT_ACCEPTABLE.getStatusCode(), resp.getStatus());
    }

    @Test
    void testQueryElements() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(
            COMMISSIONING_ENDPOINT + "/elements" + "?name=org.onap.domain.pmsh.PMSHAutomationCompositionDefinition");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        List<?> entityList = rawresp.readEntity(List.class);
        assertNotNull(entityList);
        assertThat(entityList).hasSize(4);
    }

    @Test
    void testDeleteBadRequest() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        // Call delete with no info
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
    }

    @Test
    void testDelete() throws Exception {
        var serviceTemplateCreated = createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "?name="
            + serviceTemplateCreated.getName() + "&version=" + serviceTemplateCreated.getVersion());
        // Call delete with no info
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        List<ToscaServiceTemplate> templatesInDB = serviceTemplateProvider.getAllServiceTemplates();
        assertThat(templatesInDB).isEmpty();
    }

    private synchronized ToscaServiceTemplate createEntryInDB() throws Exception {
        deleteEntryInDB();
        return serviceTemplateProvider.createServiceTemplate(serviceTemplate);
    }

    // Delete entries from the DB after relevant tests
    private synchronized void deleteEntryInDB() throws Exception {
        var list = serviceTemplateProvider.getAllServiceTemplates();
        if (!list.isEmpty()) {
            serviceTemplateProvider.deleteServiceTemplate(list.get(0).getName(), list.get(0).getVersion());
        }
    }

    private synchronized void createFullEntryInDbWithCommonProps() throws Exception {
        deleteEntryInDB();
        serviceTemplateProvider.createServiceTemplate(commonPropertiesServiceTemplate);
    }
}
