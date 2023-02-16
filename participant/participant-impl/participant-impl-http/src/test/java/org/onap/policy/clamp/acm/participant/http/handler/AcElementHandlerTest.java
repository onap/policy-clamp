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

package org.onap.policy.clamp.acm.participant.http.handler;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.http.main.handler.AutomationCompositionElementHandler;
import org.onap.policy.clamp.acm.participant.http.main.models.ConfigRequest;
import org.onap.policy.clamp.acm.participant.http.main.webclient.AcHttpClient;
import org.onap.policy.clamp.acm.participant.http.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.http.utils.ToscaUtils;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.models.acm.concepts.DeployState;
import org.onap.policy.clamp.models.acm.concepts.LockState;

class AcElementHandlerTest {

    private final CommonTestData commonTestData = new CommonTestData();
    private static final String HTTP_AUTOMATION_COMPOSITION_ELEMENT =
            "org.onap.domain.database.Http_PMSHMicroserviceAutomationCompositionElement";

    @Test
    void testUndeploy() throws IOException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var acElementId = element.getId();

        try (var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(mock(AcHttpClient.class))) {
            var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
            automationCompositionElementHandler.setIntermediaryApi(participantIntermediaryApi);
            automationCompositionElementHandler.undeploy(instanceId, acElementId);
            verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, acElementId,
                    DeployState.UNDEPLOYED, LockState.NONE);
        }
    }

    @Test
    void testDeployError() throws IOException {
        var instanceId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();

        try (var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(mock(AcHttpClient.class))) {
            automationCompositionElementHandler.setIntermediaryApi(mock(ParticipantIntermediaryApi.class));
            Map<String, Object> map = new HashMap<>();
            assertThatThrownBy(() -> automationCompositionElementHandler.deploy(instanceId, element, map))
                    .hasMessage("Constraint violations in the config request");
        }
    }

    @Test
    void testDeploy() throws Exception {
        var serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var map = new HashMap<>(nodeTemplatesMap.get(HTTP_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = commonTestData.getAutomationCompositionElement();
        map.putAll(element.getProperties());
        var instanceId = commonTestData.getAutomationCompositionId();
        var acHttpClient = mock(AcHttpClient.class);
        try (var automationCompositionElementHandler = new AutomationCompositionElementHandler(acHttpClient)) {
            var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
            automationCompositionElementHandler.setIntermediaryApi(participantIntermediaryApi);
            automationCompositionElementHandler.deploy(instanceId, element, map);
            verify(acHttpClient).run(any(ConfigRequest.class), anyMap());
            verify(participantIntermediaryApi).updateAutomationCompositionElementState(instanceId, element.getId(),
                    DeployState.DEPLOYED, LockState.LOCKED);
        }

    }
}
