/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyPapHttpClient;
import org.onap.policy.clamp.acm.participant.policy.main.parameters.CommonTestData;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
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

    private PolicyApiHttpClient api = Mockito.mock(PolicyApiHttpClient.class);
    private PolicyPapHttpClient pap = Mockito.mock(PolicyPapHttpClient.class);

    @Test
    void testHandlerDoesNotThrowExceptions() {
        var handler = getTestingHandler();

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(AC_ID, automationCompositionElementId,
                AutomationCompositionState.UNINITIALISED, AutomationCompositionOrderedState.PASSIVE));

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(AC_ID, automationCompositionElementId,
                AutomationCompositionState.RUNNING, AutomationCompositionOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(AC_ID, automationCompositionElementId,
                AutomationCompositionState.PASSIVE, AutomationCompositionOrderedState.RUNNING));
    }

    private AutomationCompositionElementHandler getTestingHandler() {
        var handler = new AutomationCompositionElementHandler(api, pap);
        var intermediaryApi = Mockito.mock(ParticipantIntermediaryApi.class);
        handler.setIntermediaryApi(intermediaryApi);
        return handler;
    }

    private AutomationCompositionElement getTestingAcElement() {
        var element = new AutomationCompositionElement();
        element.setDefinition(DEFINITION);
        element.setDescription("Description");
        element.setId(automationCompositionElementId);
        element.setOrderedState(AutomationCompositionOrderedState.UNINITIALISED);
        element.setParticipantId(CommonTestData.getParticipantId());
        element.setState(AutomationCompositionState.UNINITIALISED);
        var template = new ToscaServiceTemplate();
        template.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        template.getToscaTopologyTemplate().setPolicies(List.of(Map.of("DummyPolicy", new ToscaPolicy())));
        template.setPolicyTypes(Map.of("dummy policy type", new ToscaPolicyType()));
        element.setToscaServiceTemplateFragment(template);
        return element;
    }

    @Test
    void testAcElementUpdate() throws PfModelException {
        // Mock success scenario for policy creation and deployment
        doReturn(Response.ok().build()).when(api).createPolicyType(any());
        doReturn(Response.ok().build()).when(api).createPolicy(any());
        doReturn(Response.accepted().build()).when(pap).handlePolicyDeployOrUndeploy(any(), any(), any());

        var handler = getTestingHandler();
        var element = getTestingAcElement();

        assertDoesNotThrow(() -> handler.automationCompositionElementUpdate(AC_ID, element, Map.of()));

        assertDoesNotThrow(() -> handler.automationCompositionElementStateChange(AC_ID, automationCompositionElementId,
                AutomationCompositionState.PASSIVE, AutomationCompositionOrderedState.UNINITIALISED));

        // Mock failure in policy deployment
        doReturn(Response.serverError().build()).when(pap).handlePolicyDeployOrUndeploy(any(), any(), any());
        assertDoesNotThrow(() -> handler.automationCompositionElementUpdate(AC_ID, element, Map.of()));

        // Mock failure in policy type creation
        doReturn(Response.serverError().build()).when(api).createPolicyType(any());
        assertDoesNotThrow(() -> handler.automationCompositionElementUpdate(AC_ID, element, Map.of()));
    }
}
