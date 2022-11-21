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

package org.onap.policy.clamp.acm.participant.a1.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.onap.policy.clamp.acm.participant.a1.exception.PolicyServiceException;
import org.onap.policy.clamp.acm.participant.a1.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.a1.utils.ToscaUtils;
import org.onap.policy.clamp.acm.participant.a1.webclient.AcPmsClient;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionOrderedState;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionState;
import org.onap.policy.models.tosca.authorative.concepts.ToscaNodeTemplate;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AcElementHandlerTest {

    @InjectMocks
    @Spy
    private AutomationCompositionElementHandler automationCompositionElementHandler =
        new AutomationCompositionElementHandler();

    private final AcPmsClient acPmsClient = mock(AcPmsClient.class);

    private final CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String A1_AUTOMATION_COMPOSITION_ELEMENT =
        "org.onap.domain.database.A1PMSAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
    }

    @BeforeEach
    void startMocks() {
        automationCompositionElementHandler.setIntermediaryApi(mock(ParticipantIntermediaryApi.class));
    }

    @Test
    void test_automationCompositionElementStateChange() {
        var automationCompositionId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
            automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
            AutomationCompositionOrderedState.PASSIVE));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
            automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
            AutomationCompositionOrderedState.UNINITIALISED));

        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementStateChange(
            automationCompositionId, automationCompositionElementId, AutomationCompositionState.PASSIVE,
            AutomationCompositionOrderedState.RUNNING));
    }

    @Test
    void test_AutomationCompositionElementUpdate() throws PolicyServiceException {
        when(acPmsClient.isPmsHealthy()).thenReturn(Boolean.TRUE);
        doNothing().when(acPmsClient).createService(any());
        AutomationCompositionElement element = commonTestData.getAutomationCompositionElement();

        Map<String, ToscaNodeTemplate> nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        assertDoesNotThrow(() -> automationCompositionElementHandler.automationCompositionElementUpdate(
            commonTestData.getAutomationCompositionId(), element,
            nodeTemplatesMap.get(A1_AUTOMATION_COMPOSITION_ELEMENT)));
    }
}
