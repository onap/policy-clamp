/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.onap.policy.clamp.acm.participant.a1pms.exception.A1PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1pms.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.a1pms.utils.ToscaUtils;
import org.onap.policy.clamp.acm.participant.a1pms.webclient.AcA1PmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AcElementHandlerTest {

    private final AcA1PmsClient acA1PmsClient = mock(AcA1PmsClient.class);

    @InjectMocks
    @Spy
    private AutomationCompositionElementHandler automationCompositionElementHandler =
            new AutomationCompositionElementHandler(acA1PmsClient);

    private final CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String A1_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.domain.database.A1PMSAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
    }

    @BeforeEach
    void startMocks() throws A1PolicyServiceException {
        automationCompositionElementHandler.setIntermediaryApi(mock(ParticipantIntermediaryApi.class));
        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.TRUE);
        doNothing().when(acA1PmsClient).createService(any());
    }

    @Test
    void test_automationCompositionElementStateChange() throws A1PolicyServiceException {
        var automationCompositionId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler
                .automationCompositionElementUpdate(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties());

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
                automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
                AutomationCompositionOrderedState.PASSIVE));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
                automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
                AutomationCompositionOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
                automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
                AutomationCompositionOrderedState.RUNNING));

        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.FALSE);
        assertThrows(A1PolicyServiceException.class,
                () -> automationCompositionElementHandler.automationCompositionElementStateChange(
                        automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
                        AutomationCompositionOrderedState.UNINITIALISED));
    }

    @Test
    void test_AutomationCompositionElementUpdate() {
        var element = commonTestData.getAutomationCompositionElement();

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        assertDoesNotThrow(() -> automationCompositionElementHandler
                .automationCompositionElementUpdate(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));
    }

    @Test
    void test_AutomationCompositionElementUpdateWithUnhealthyA1pms() {
        var element = commonTestData.getAutomationCompositionElement();
        when(acA1PmsClient.isPmsHealthy()).thenReturn(Boolean.FALSE);

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        assertThrows(A1PolicyServiceException.class, () -> automationCompositionElementHandler
                .automationCompositionElementUpdate(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));
    }

    @Test
    void test_AutomationCompositionElementUpdateWithInvalidConfiguration() {
        var element = commonTestData.getAutomationCompositionElement();
        assertThrows(A1PolicyServiceException.class, () -> automationCompositionElementHandler
                .automationCompositionElementUpdate(commonTestData.getAutomationCompositionId(), element,
                        Map.of()));
    }
}
