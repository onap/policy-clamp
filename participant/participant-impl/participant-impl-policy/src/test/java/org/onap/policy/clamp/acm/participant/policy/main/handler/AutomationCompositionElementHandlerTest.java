/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.CompositionElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.InstanceElementDto;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyApiHttpClient;
import org.onap.policy.clamp.acm.participant.policy.client.PolicyPapHttpClient;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyType;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaTopologyTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class AutomationCompositionElementHandlerTest {

    private static final Coder CODER = new StandardCoder();

    private static final ToscaConceptIdentifier DEFINITION =
            new ToscaConceptIdentifier("1.0.1", "org.onap.PM_CDS_Blueprint");

    @Test
    void testHandlerUndeployNoPolicy() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var handler = new AutomationCompositionElementHandler(mock(PolicyApiHttpClient.class),
                mock(PolicyPapHttpClient.class), intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = getInstanceElementWithNullTopology();

        handler.undeploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                "Undeployed");
    }

    private CompositionElementDto getCompositionElement() {
        return new CompositionElementDto(UUID.randomUUID(), DEFINITION, Map.of(), Map.of());
    }

    private InstanceElementDto getInstanceElement() {
        var template = new ToscaServiceTemplate();
        template.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        template.getToscaTopologyTemplate().setPolicies(List.of(Map.of("DummyPolicy", new ToscaPolicy())));
        template.setPolicyTypes(Map.of("dummy policy type", new ToscaPolicyType()));
        var inProperties = getProperties(template);
        return new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), inProperties, Map.of());
    }

    private Map<String, Object> getProperties(ToscaServiceTemplate template) {
        try {
            var json = CODER.encode(template);
            return CODER.decode(json, Map.class);
        } catch (CoderException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDeploy() throws PfModelException {
        // Mock success scenario for policy creation and deployment
        var api = mock(PolicyApiHttpClient.class);
        var pap = mock(PolicyPapHttpClient.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var handler = new AutomationCompositionElementHandler(api, pap, intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = getInstanceElement();

        handler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR,
                "Deployed");

        clearInvocations(intermediaryApi);
        handler.undeploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                "Undeployed");
    }

    @Test
    void testDeployError() {
        var api = mock(PolicyApiHttpClient.class);
        var pap = mock(PolicyPapHttpClient.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var handler = new AutomationCompositionElementHandler(api, pap, intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(),
                Map.of("data_types", 100), Map.of());
        assertThatThrownBy(() -> handler.deploy(compositionElement, instanceElement))
                .isInstanceOf(PfModelException.class);
    }

    @Test
    void testDeployNoPolicy() throws PfModelException {
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var handler = new AutomationCompositionElementHandler(mock(PolicyApiHttpClient.class),
                mock(PolicyPapHttpClient.class), intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = getInstanceElementWithNullTopology();
        handler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "ToscaTopologyTemplate not defined");

        clearInvocations(intermediaryApi);
        instanceElement = getInstanceElementWithNoPolicy();
        handler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR,
                "Deployed");

        clearInvocations(intermediaryApi);
        handler.undeploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR,
                "Undeployed");
    }

    private InstanceElementDto getInstanceElementWithNullTopology() {
        var template = new ToscaServiceTemplate();
        template.setToscaTopologyTemplate(null);
        var inProperties = getProperties(template);
        return new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), inProperties, Map.of());
    }

    private InstanceElementDto getInstanceElementWithNoPolicy() {
        var template = new ToscaServiceTemplate();
        template.setToscaTopologyTemplate(new ToscaTopologyTemplate());
        var inProperties = getProperties(template);
        return new InstanceElementDto(UUID.randomUUID(), UUID.randomUUID(), inProperties, Map.of());
    }

    @Test
    void testApiPolicyTypeException() throws PfModelException {
        var api = mock(PolicyApiHttpClient.class);
        when(api.createPolicyType(any()))
                .thenThrow(new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), "", null, null, null));

        var pap = mock(PolicyPapHttpClient.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var handler = new AutomationCompositionElementHandler(api, pap, intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = getInstanceElement();

        // Mock failure in policy type creation
        handler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "Creation of PolicyTypes/Policies failed. Policies will not be deployed.");
    }

    @Test
    void testApiPolicyException() throws PfModelException {
        var api = mock(PolicyApiHttpClient.class);
        when(api.createPolicy(any()))
                .thenThrow(new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), "", null, null, null));

        var pap = mock(PolicyPapHttpClient.class);
        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var handler = new AutomationCompositionElementHandler(api, pap, intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = getInstanceElement();

        // Mock failure in policy creation
        handler.deploy(compositionElement, instanceElement);
        verify(intermediaryApi).updateAutomationCompositionElementState(instanceElement.instanceId(),
                instanceElement.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.FAILED,
                "Creation of PolicyTypes/Policies failed. Policies will not be deployed.");
    }

    @Test
    void testDeployPapException() {
        var pap = mock(PolicyPapHttpClient.class);
        when(pap.handlePolicyDeployOrUndeploy(any(), any(), any()))
                .thenThrow(new WebClientResponseException(HttpStatus.BAD_REQUEST.value(), "", null, null, null));

        var intermediaryApi = mock(ParticipantIntermediaryApi.class);
        var api = mock(PolicyApiHttpClient.class);
        var handler = new AutomationCompositionElementHandler(api, pap, intermediaryApi);

        var compositionElement = getCompositionElement();
        var instanceElement = getInstanceElement();
        assertThatThrownBy(() -> handler.deploy(compositionElement, instanceElement))
                .hasMessageMatching("Deploy of Policy failed.");
    }
}
