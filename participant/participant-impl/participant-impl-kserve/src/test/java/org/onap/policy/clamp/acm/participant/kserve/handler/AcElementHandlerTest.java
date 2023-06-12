/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.onap.policy.clamp.acm.participant.intermediary.api.ParticipantIntermediaryApi;
import org.onap.policy.clamp.acm.participant.kserve.exception.KserveException;
import org.onap.policy.clamp.acm.participant.kserve.k8s.KserveClient;
import org.onap.policy.clamp.acm.participant.kserve.utils.CommonTestData;
import org.onap.policy.clamp.acm.participant.kserve.utils.ToscaUtils;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class AcElementHandlerTest {

    private final KserveClient kserveClient = mock(KserveClient.class);

    private ParticipantIntermediaryApi participantIntermediaryApi = mock(ParticipantIntermediaryApi.class);

    @InjectMocks
    @Spy
    private AutomationCompositionElementHandler automationCompositionElementHandler =
            new AutomationCompositionElementHandler(participantIntermediaryApi, kserveClient);

    @Mock
    private ExecutorService executor;
    @Mock
    private Future<String> result;

    private final CommonTestData commonTestData = new CommonTestData();

    private static ToscaServiceTemplate serviceTemplate;
    private static final String KSERVE_AUTOMATION_COMPOSITION_ELEMENT =
            "onap.policy.clamp.ac.element.KserveAutomationCompositionElement";

    @BeforeAll
    static void init() {
        serviceTemplate = ToscaUtils.readAutomationCompositionFromTosca();
    }

    @BeforeEach
    void startMocks() throws ExecutionException, InterruptedException, IOException, ApiException {
        doReturn(true).when(kserveClient).deployInferenceService(any(), any());
        doReturn(true).when(automationCompositionElementHandler).checkInferenceServiceStatus(any(), any(), anyInt(),
                anyInt());
    }

    @Test
    void test_automationCompositionElementStateChange() throws PfModelException {
        var automationCompositionId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        var automationCompositionElementId = element.getId();

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties());

        assertDoesNotThrow(() -> automationCompositionElementHandler.undeploy(automationCompositionId,
                automationCompositionElementId));

    }

    @Test
    void test_AutomationCompositionElementUpdate() throws IOException, ApiException {
        var element = commonTestData.getAutomationCompositionElement();

        var nodeTemplatesMap = serviceTemplate.getToscaTopologyTemplate().getNodeTemplates();
        assertDoesNotThrow(() -> automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(),
                element, nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));
        assertThat(automationCompositionElementHandler.getConfigRequestMap()).hasSize(1).containsKey(element.getId());

        doThrow(new ApiException("Error installing the inference service")).when(kserveClient)
                .deployInferenceService(any(), any());

        var elementId2 = UUID.randomUUID();
        element.setId(elementId2);
        assertThrows(KserveException.class,
                () -> automationCompositionElementHandler.deploy(commonTestData.getAutomationCompositionId(), element,
                        nodeTemplatesMap.get(KSERVE_AUTOMATION_COMPOSITION_ELEMENT).getProperties()));

        assertThat(automationCompositionElementHandler.getConfigRequestMap().containsKey(elementId2)).isFalse();
    }

    @Test
    void test_checkInferenceServiceStatus() throws ExecutionException, InterruptedException {
        doReturn(result).when(executor).submit(any(Runnable.class), any());
        doReturn("Done").when(result).get();
        doReturn(true).when(result).isDone();
        assertDoesNotThrow(() -> automationCompositionElementHandler.checkInferenceServiceStatus("sklearn-iris",
                "kserve-test", 1, 1));
    }

    @Test
    void testUpdate() throws PfModelException {
        var automationCompositionId = commonTestData.getAutomationCompositionId();
        var element = commonTestData.getAutomationCompositionElement();
        assertDoesNotThrow(
                () -> automationCompositionElementHandler.update(automationCompositionId, element, Map.of()));
    }

    @Test
    void testLock() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.lock(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void testUnlock() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.unlock(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void testDelete() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.delete(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    void testPrime() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.prime(UUID.randomUUID(), List.of()));
    }

    @Test
    void testDeprime() throws PfModelException {
        assertDoesNotThrow(() -> automationCompositionElementHandler.deprime(UUID.randomUUID()));
    }
}
