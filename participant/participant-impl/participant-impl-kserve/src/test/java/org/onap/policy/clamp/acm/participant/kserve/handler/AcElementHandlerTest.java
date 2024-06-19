/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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

package org.onap.policy.clamp.acm.participant.kserve.handler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.kubernetes.client.openapi.ApiException;
import jakarta.validation.ValidationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kserve.exception.KserveException;
import org.onap.policy.clamp.acm.participant.kserve.k8s.KserveClient;
import org.onap.policy.clamp.acm.participant.kserve.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.kserve.utils.ToscaUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;

class AcElementHandlerTest {

    private final CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String KSERVE_AUTOMATION_COMPOSITION_ELEMENT =
            "onap.policy.clamp.ac.element.KserveAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
    }

    @Test
    void test_automationCompositionElementStateChange()
            throws ExecutionException, InterruptedException, IOException, ApiException {
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var compositionElement = commonTestData.getCompositionElement(
                nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = commonTestData.getAutomationCompositionElement();

        var kserveClient = mock(KserveClient.class);
        doReturn(true).when(kserveClient).deployInferenceService(any(), any());
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                spy(new AutomationCompositionElementHandler(participantIntermediaryApi, kserveClient));
        doReturn(true).when(automationCompositionElementHandler)
                .checkInferenceServiceStatus(any(), any(), anyInt(), anyInt());

        assertDoesNotThrow(() -> automationCompositionElementHandler.deploy(compositionElement, element));
        assertDoesNotThrow(() -> automationCompositionElementHandler.undeploy(compositionElement, element));
    }

    @Test
    void test_automationCompositionElementFailed()
            throws ExecutionException, InterruptedException, IOException, ApiException {
        var kserveClient = mock(KserveClient.class);
        doReturn(false).when(kserveClient).deployInferenceService(any(), any());
        doReturn(false).when(kserveClient).undeployInferenceService(any(), any());
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                spy(new AutomationCompositionElementHandler(participantIntermediaryApi, kserveClient));
        doReturn(false).when(automationCompositionElementHandler)
                .checkInferenceServiceStatus(any(), any(), anyInt(), anyInt());

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var compositionElement = commonTestData.getCompositionElement(
                nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = commonTestData.getAutomationCompositionElement();
        assertDoesNotThrow(() -> automationCompositionElementHandler.deploy(compositionElement, element));
        assertDoesNotThrow(() -> automationCompositionElementHandler.undeploy(compositionElement, element));
    }

    @Test
    void test_automationCompositionElementWrongData() {
        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var element = commonTestData.getAutomationCompositionElement();

        var kserveClient = mock(KserveClient.class);
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, kserveClient);

        var compositionElementEmpty = commonTestData.getCompositionElement(Map.of());
        assertThrows(ValidationException.class,
                () -> automationCompositionElementHandler.deploy(compositionElementEmpty, element));

        var compositionElementWrong = commonTestData.getCompositionElement(Map.of("kserveInferenceEntities", "1"));
        assertThrows(KserveException.class,
                () -> automationCompositionElementHandler.deploy(compositionElementWrong, element));

        var map = new HashMap<>(nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        map.put("uninitializedToPassiveTimeout", " ");
        var compositionElementWrong2 = commonTestData.getCompositionElement(map);
        assertThrows(KserveException.class,
                () -> automationCompositionElementHandler.deploy(compositionElementWrong2, element));
    }

    @Test
    void test_AutomationCompositionElementUpdate()
            throws IOException, ApiException, ExecutionException, InterruptedException {
        var kserveClient = mock(KserveClient.class);
        doReturn(true).when(kserveClient).deployInferenceService(any(), any());

        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                spy(new AutomationCompositionElementHandler(participantIntermediaryApi, kserveClient));
        doReturn(true).when(automationCompositionElementHandler)
                .checkInferenceServiceStatus(any(), any(), anyInt(), anyInt());
        doThrow(new ApiException("Error installing the inference service")).when(kserveClient)
                .deployInferenceService(any(), any());

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        var compositionElement = commonTestData.getCompositionElement(
                nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties());
        var element = commonTestData.getAutomationCompositionElement();
        assertThrows(KserveException.class,
                () -> automationCompositionElementHandler.deploy(compositionElement, element));

    }

    @Test
    void test_checkInferenceServiceStatus() throws IOException, ApiException {
        var kserveClient = mock(KserveClient.class);
        doReturn("True").when(kserveClient).getInferenceServiceStatus(any(), any());
        doReturn(true).when(kserveClient).deployInferenceService(any(), any());
        var participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);
        var automationCompositionElementHandler =
                new AutomationCompositionElementHandler(participantIntermediaryApi, kserveClient);

        assertDoesNotThrow(() -> automationCompositionElementHandler.checkInferenceServiceStatus("sklearn-iris",
                "kserve-test", 1, 1));
    }
}
