/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.policy.main.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyPapHttpClient;
import org.onap.policy.clamp.models.acm.concepts.AcElementDeploy;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;
import org.onap.policy.clamp.models.acm.messages.rest.instantiation.DeployOrder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;

class AutomationCompositionElementHandlerTest {

    private static final String ID_NAME = "org.onap.PM_CDS_Blueprint";
    private static final String ID_VERSION = "1.0.1";
    private static final UUID automationCompositionElementId = UUID.randomUUID();
    public static final UUID AC_ID = UUID.randomUUID();
    private static final ToscaConceptIdentifier DEFINITION = new ToscaConceptIdentifier(ID_NAME, ID_VERSION);

    @Test
    void testHandlerUndeploy() throws PfModelException {
        var handler = new AutomationCompositionElementHandler(mock(PolicyApiHttpClient.class),
                mock(PolicyPapHttpClient.class));
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        handler.setIntermediaryApi(intermediaryApi);

        handler.undeploy(AC_ID, automationCompositionElementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(AC_ID, automationCompositionElementId,
                DeployState.UNDEPLOYED, LockState.NONE);
    }

    private AcElementDeploy getTestingAcElement() {
        var element = new AcElementDeploy();
        element.setDefinition(DEFINITION);
        element.setId(automationCompositionElementId);
        element.setOrderedState(DeployOrder.DEPLOY);
        var template = new ToscaServiceTemplate();
        template.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        template.getToscaTopologyTemplate().setPolicies(List.of(Map.of("DummyPolicy", new ToscaPolicy())));
        template.setPolicyTypes(Map.of("dummy policy type", new ToscaPolicyType()));
        element.setToscaServiceTemplateFragment(template);
        return element;
    }

    @Test
    void testDeploy() throws PfModelException {
        // Mock success scenario for policy creation and deployment
        var api = mock(PolicyApiHttpClient.class);
        doReturn(Response.ok().build()).when(api).createPolicyType(any());
        doReturn(Response.ok().build()).when(api).createPolicy(any());

        var pap = mock(PolicyPapHttpClient.class);
        doReturn(Response.accepted().build()).when(pap).handlePolicyDeployOrUndeploy(any(), any(), any());

        var handler = new AutomationCompositionElementHandler(api, pap);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        handler.setIntermediaryApi(intermediaryApi);

        handler.deploy(AC_ID, getTestingAcElement(), Map.of());
        handler.undeploy(AC_ID, automationCompositionElementId);
        verify(intermediaryApi).updateAutomationCompositionElementState(AC_ID, automationCompositionElementId,
                DeployState.UNDEPLOYED, LockState.NONE);
    }

    @Test
    void testApiException() throws PfModelException {
        var api = mock(PolicyApiHttpClient.class);
        doReturn(Response.serverError().build()).when(api).createPolicyType(any());
        doReturn(Response.ok().build()).when(api).createPolicy(any());

        var pap = mock(PolicyPapHttpClient.class);
        doReturn(Response.accepted().build()).when(pap).handlePolicyDeployOrUndeploy(any(), any(), any());

        var handler = new AutomationCompositionElementHandler(api, pap);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        handler.setIntermediaryApi(intermediaryApi);
        var element = getTestingAcElement();

        // Mock failure in policy type creation
        assertThatThrownBy(() -> handler.deploy(AC_ID, element, Map.of()))
                .hasMessageMatching("Creation of PolicyTypes/Policies failed. Policies will not be deployed.");
    }

    @Test
    void testDeployPapException() throws PfModelException {
        var api = mock(PolicyApiHttpClient.class);
        doReturn(Response.ok().build()).when(api).createPolicyType(any());
        doReturn(Response.ok().build()).when(api).createPolicy(any());

        var pap = mock(PolicyPapHttpClient.class);
        doReturn(Response.serverError().build()).when(pap).handlePolicyDeployOrUndeploy(any(), any(), any());

        var handler = new AutomationCompositionElementHandler(api, pap);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        handler.setIntermediaryApi(intermediaryApi);
        var element = getTestingAcElement();
        assertThatThrownBy(() -> handler.deploy(AC_ID, element, Map.of()))
                .hasMessageMatching("Deploy of Policy failed.");
    }
}
