/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
 *  Modifications Copyright (C) 2021 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.clamp.acm.participant.kubernetes.helm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.handler.AutomationCompositionElementHandler;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class PodStatusValidatorTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static int TIMEOUT = 2;
    private static int STATUS_CHECK_INTERVAL = 1;
    private static List<ChartInfo> charts;

    @InjectMocks
    private PodStatusValidator podStatusValidator = new PodStatusValidator(charts.get(0), TIMEOUT,
            STATUS_CHECK_INTERVAL);

    @InjectMocks
    private PodStatusValidator podValidatorWithPodName = new PodStatusValidator(charts.get(2), TIMEOUT,
            STATUS_CHECK_INTERVAL);


    @Mock
    private HelmClient client;

    @BeforeAll
    static void init() throws CoderException {
        charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
    }

    @AfterEach
    void clearPodStatusMap() {
        AutomationCompositionElementHandler.getPodStatusMap().clear();
    }


    @Test
    void test_RunningPodState() throws ServiceException {
        String runningPod = "NAME\tREADY\tSTATUS\tRESTARTS\tAGE\r\nHelloWorld-54777df9f8-qpzqr\t1/1\tRunning\t0\t9h";
        doReturn(runningPod).when(client).executeCommand(any());
        assertDoesNotThrow(() -> podStatusValidator.run());
        assertThat(AutomationCompositionElementHandler.getPodStatusMap()).hasSize(1);
        assertThat(AutomationCompositionElementHandler.getPodStatusMap()).containsKey(charts.get(0).getReleaseName());
        assertThat(AutomationCompositionElementHandler.getPodStatusMap())
            .containsValue(Map.of("HelloWorld-54777df9f8-qpzqr", "Running"));
    }

    @Test
    void test_InvalidPodState() throws ServiceException {
        String invalidPod = "NAME\tREADY\tSTATUS\tRESTARTS\tAGE\nhellofromdocker-54777df9f8-qpzqr\t1/1\tInit\t0\t9h";
        doReturn(invalidPod).when(client).executeCommand(any());
        assertThrows(ServiceException.class, () -> podStatusValidator.run());
        assertThat(AutomationCompositionElementHandler.getPodStatusMap()).isEmpty();
    }

    // Use case scenario: Hard coded pod name
    @Test
    void test_RunningPodStateWithPodName() throws ServiceException {
        String runningPod = "NAME\tREADY\tSTATUS\tRESTARTS\tAGE\r\nhelloallworld-54777df9f8-qpzqr\t1/1\tRunning\t0\t9h";
        doReturn(runningPod).when(client).executeCommand(any());
        assertDoesNotThrow(() -> podValidatorWithPodName.run());
        assertThat(AutomationCompositionElementHandler.getPodStatusMap()).hasSize(1);
        assertThat(AutomationCompositionElementHandler.getPodStatusMap()).containsKey(charts.get(2).getReleaseName());
        assertThat(AutomationCompositionElementHandler.getPodStatusMap())
            .containsValue(Map.of("helloallworld-54777df9f8-qpzqr", "Running"));
    }
}
