/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.a1pms.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.StateChangeResult;
import org.onap.policy.common.utils.coder.MapperFactory;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class AcElementHandlerTest {

    private final AcA1PmsClient acA1PmsClient = mock(AcA1PmsClient.class);

    private static ToscaServiceTemplate serviceTemplate;
    private static final String A1_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.domain.database.A1PMSAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = CommonTestData.getToscaServiceTemplateFromYamlFile(CommonTestData.TOSCA_TEMPLATE_YAML);
    }

    @BeforeEach
    void startMocks() throws A1PolicyServiceException {
        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.TRUE);
        doNothing().when(acA1PmsClient).createService(any());
    }

    @Test
    void test_automationCompositionElementStateChange() throws A1PolicyServiceException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler = new AutomationCompositionElementHandler(participantIntermediaryApi,
                acA1PmsClient, MapperFactory.createJsonMapper());

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var compositionElement = CommonTestData.getCompositionElement(
                nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = CommonTestData.getAutomationCompositionElement();

        automationCompositionElementHandler.deploy(compositionElement, element);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(element.instanceId(),
                element.elementId(), DeployState.DEPLOYED, null, StateChangeResult.NO_ERROR, "Deployed");

        automationCompositionElementHandler.undeploy(compositionElement, element);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(element.instanceId(),
                element.elementId(), DeployState.UNDEPLOYED, null, StateChangeResult.NO_ERROR, "Undeployed");

        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.FALSE);
        assertThrows(A1PolicyServiceException.class,
                () -> automationCompositionElementHandler.undeploy(compositionElement, element));
    }

    @Test
    void test_AutomationCompositionElementUpdate() throws A1PolicyServiceException {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler = new AutomationCompositionElementHandler(
                participantIntermediaryApi, acA1PmsClient, MapperFactory.createJsonMapper());

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var compositionElement = CommonTestData.getCompositionElement(
                nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = CommonTestData.getAutomationCompositionElement();

        automationCompositionElementHandler.deploy(compositionElement, element);
        verify(participantIntermediaryApi).updateAutomationCompositionElementState(
                element.instanceId(), element.elementId(), DeployState.DEPLOYED, null,
                StateChangeResult.NO_ERROR, "Deployed");
    }

    @Test
    void test_AutomationCompositionElementUpdateWithUnhealthyA1pms() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient, new ObjectMapper());

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var compositionElement = CommonTestData.getCompositionElement(
                nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = CommonTestData.getAutomationCompositionElement();
        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.FALSE);

        assertThrows(A1PolicyServiceException.class,
                () -> automationCompositionElementHandler.deploy(compositionElement, element));
    }

    @Test
    void test_AutomationCompositionElementUpdateWithInvalidConfiguration() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient, new ObjectMapper());

        var compositionElement = CommonTestData.getCompositionElement(Map.of());
        var element = CommonTestData.getAutomationCompositionElement();
        assertThrows(A1PolicyServiceException.class,
                () -> automationCompositionElementHandler.deploy(compositionElement, element));
    }

    @Test
    void test_AutomationCompositionElementUpdateWithCoderException() {
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, acA1PmsClient, new ObjectMapper());

        Map<String, Object> invalidProperties = Map.of("policyServiceEntities", 1);
        var compositionElement = CommonTestData.getCompositionElement(invalidProperties);
        var element = CommonTestData.getAutomationCompositionElement();
        assertThrows(A1PolicyServiceException.class,
                () -> automationCompositionElementHandler.deploy(compositionElement, element));
    }
}
