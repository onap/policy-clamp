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

package org.onap.policy.clamp.acm.participant.kserve.k8s;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.kserve.exception.KserveException;

class InferenceServiceValidatorTest {

    private static final int TIMEOUT = 2;
    private static final int STATUS_CHECK_INTERVAL = 1;

    private static final String inferenceSvcName = "inference-test";
    private static final String namespace = "test";

    @Test
    void test_runningPodState() throws IOException, ApiException {
        var kserveClient = mock(KserveClient.class);
        doReturn("True").when(kserveClient).getInferenceServiceStatus(any(), any());
        var inferenceServiceValidator =
                new InferenceServiceValidator(inferenceSvcName, namespace, TIMEOUT, STATUS_CHECK_INTERVAL,
                        kserveClient);
        assertDoesNotThrow(inferenceServiceValidator::run);
    }

    @Test
    void test_EmptyPodState() throws IOException, ApiException {
        var kserveClient = mock(KserveClient.class);
        doReturn("").when(kserveClient).getInferenceServiceStatus(any(), any());
        var inferenceServiceValidator =
                new InferenceServiceValidator("", namespace, TIMEOUT, STATUS_CHECK_INTERVAL,
                        kserveClient);
        assertThatThrownBy(inferenceServiceValidator::run).isInstanceOf(KserveException.class)
                .cause().hasMessage("Kserve setup is unavailable for inference service to be deployed");
    }

    @Test
    void test_PodFailureState() throws IOException, ApiException {
        var kserveClient = mock(KserveClient.class);
        doReturn("False").when(kserveClient).getInferenceServiceStatus(any(), any());
        var inferenceServiceValidator =
                new InferenceServiceValidator(inferenceSvcName, namespace, TIMEOUT, STATUS_CHECK_INTERVAL,
                        kserveClient);
        assertThatThrownBy(inferenceServiceValidator::run).isInstanceOf(KserveException.class)
                .hasMessage("Error verifying the status of the inference service. Exiting");
    }

}
