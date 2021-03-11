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

package org.onap.policy.clamp.controlloop.runtime.commissioning.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.messages.rest.commissioning.CommissioningResponse;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.common.utils.coder.YamlJsonTranslator;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

public class CommissioningControllerTest extends CommonRestController {

    private static final String TOSCA_SERVICE_TEMPLATE_YAML =
            "src/test/resources/servicetemplates/pmsh_multiple_cl_tosca.yaml";
    private static final YamlJsonTranslator yamlTranslator = new YamlJsonTranslator();
    private static final String COMMISSIONING_ENDPOINT = "commission";
    private static ToscaServiceTemplate serviceTemplate = new ToscaServiceTemplate();

    /**
     * starts Main and inserts a commissioning template.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRestController.setUpBeforeClass("CommissioningApi");

        serviceTemplate = yamlTranslator.fromYaml(ResourceUtils.getResourceAsString(TOSCA_SERVICE_TEMPLATE_YAML),
                        ToscaServiceTemplate.class);
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testSwagger() throws Exception {
        super.testSwagger(COMMISSIONING_ENDPOINT);
    }

    @Test
    public void testUnauthorizedCreate() throws Exception {
        assertUnauthorizedPost(COMMISSIONING_ENDPOINT, Entity.json(serviceTemplate));
    }

    @Test
    public void testUnauthorizedQuery() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT);
    }

    @Test
    public void testUnauthorizedQueryElements() throws Exception {
        assertUnauthorizedGet(COMMISSIONING_ENDPOINT + "/elements");
    }

    @Test
    public void testUnauthorizedDelete() throws Exception {
        assertUnauthorizedDelete(COMMISSIONING_ENDPOINT);
    }

    @Test
    public void testCreateBadRequest() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json("NotToscaServiceTempalte"));

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        CommissioningResponse commissioningResponse = resp.readEntity(CommissioningResponse.class);
        assertNotNull(commissioningResponse.getErrorDetails());
        assertNull(commissioningResponse.getAffectedControlLoopDefinitions());
    }

    @Test
    public void testCreate() throws Exception {
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response resp = invocationBuilder.post(Entity.json(serviceTemplate));
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());
        CommissioningResponse commissioningResponse = resp.readEntity(CommissioningResponse.class);

        assertNotNull(commissioningResponse);
        assertNull(commissioningResponse.getErrorDetails());
        // Response should return the number of node templates present in the service template
        assertThat(commissioningResponse.getAffectedControlLoopDefinitions()).hasSize(13);
        for (String nodeTemplateName : serviceTemplate.getToscaTopologyTemplate().getNodeTemplates().keySet()) {
            assertTrue(commissioningResponse.getAffectedControlLoopDefinitions().stream()
                    .anyMatch(ac -> ac.getName().equals(nodeTemplateName)));
        }
    }

    @Test
    public void testQuery_NoResultWithThisName() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "?name=noResultWithThisName");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        List entityList = rawresp.readEntity(List.class);
        assertThat(entityList).isEmpty();
    }

    @Test
    public void testQuery() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        List entityList = rawresp.readEntity(List.class);
        assertNotNull(entityList);
        assertThat(entityList).hasSize(2);
    }

    @Test
    public void testQueryElementsBadRequest() throws Exception {
        createEntryInDB();

        //Call get elements with no info
        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/elements");
        Response resp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testQueryElements() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "/elements"
                + "?name=org.onap.domain.pmsh.PMSHControlLoopDefinition");
        Response rawresp = invocationBuilder.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), rawresp.getStatus());
        List entityList = rawresp.readEntity(List.class);
        assertNotNull(entityList);
        assertThat(entityList).hasSize(4);
    }

    @Test
    public void testDeleteBadRequest() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT);
        //Call delete with no info
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        createEntryInDB();

        Invocation.Builder invocationBuilder = super.sendRequest(COMMISSIONING_ENDPOINT + "?name="
                + serviceTemplate.getName() + "&version=" + serviceTemplate.getVersion());
        //Call delete with no info
        Response resp = invocationBuilder.delete();
        assertEquals(Response.Status.OK.getStatusCode(), resp.getStatus());

        try (PolicyModelsProvider modelsProvider = new PolicyModelsProviderFactory()
                .createPolicyModelsProvider(CommonRestController.getParameters())) {
            List<ToscaServiceTemplate> templatesInDB = modelsProvider.getServiceTemplateList(null, null);
            assertThat(templatesInDB).isEmpty();
        }
    }

    private synchronized void createEntryInDB() throws Exception {
        try (PolicyModelsProvider modelsProvider = new PolicyModelsProviderFactory()
                .createPolicyModelsProvider(CommonRestController.getParameters())) {
            modelsProvider.createServiceTemplate(serviceTemplate);
        }
    }
}
