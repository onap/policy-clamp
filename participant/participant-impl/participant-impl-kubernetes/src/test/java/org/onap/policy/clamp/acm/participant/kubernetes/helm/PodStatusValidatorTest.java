/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022,2024,2026 OpenInfra Foundation Europe. All rights reserved.
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.acm.participant.kubernetes.exception.ServiceException;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartInfo;
import org.onap.policy.clamp.acm.participant.kubernetes.models.ChartList;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;

class PodStatusValidatorTest {

    private static final Coder CODER = new StandardCoder();
    private static final String CHART_INFO_YAML = "src/test/resources/ChartList.json";
    private static final int TIMEOUT = 2;
    private static final int STATUS_CHECK_INTERVAL = 1;
    private static ChartInfo chart0;
    private static ChartInfo chart2;

    @BeforeAll
    static void init() throws CoderException {
        var charts = CODER.decode(new File(CHART_INFO_YAML), ChartList.class).getCharts();
        chart0 = charts.get(0);
        chart2 = charts.get(2);
    }

    @Test
    void test_RunningPodState() throws ServiceException {
        var client = mock(HelmClient.class);
        var podStatusValidator = new PodStatusValidator(client);
        String runningPod = "NAME\tREADY\tSTATUS\tRESTARTS\tAGE\r\nHelloWorld-54777df9f8-qpzqr\t1/1\tRunning\t0\t9h";
        doReturn(runningPod).when(client).executeCommand(any());
        assertDoesNotThrow(() -> podStatusValidator.run(TIMEOUT, STATUS_CHECK_INTERVAL, chart0));
    }

    @Test
    void test_InvalidPodState() throws ServiceException {
        var client = mock(HelmClient.class);
        var podStatusValidator = new PodStatusValidator(client);
        String invalidPod = "NAME\tREADY\tSTATUS\tRESTARTS\tAGE\nhellofromdocker-54777df9f8-qpzqr\t1/1\tInit\t0\t9h";
        doReturn(invalidPod).when(client).executeCommand(any());
        assertThrows(PfModelException.class,
                () -> podStatusValidator.run(TIMEOUT, STATUS_CHECK_INTERVAL, chart0));
    }

    // Use case scenario: Hard coded pod name
    @Test
    void test_RunningPodStateWithPodName() throws ServiceException {
        var client = mock(HelmClient.class);
        var podStatusValidator = new PodStatusValidator(client);
        String runningPod = "NAME\tREADY\tSTATUS\tRESTARTS\tAGE\r\nhelloallworld-54777df9f8-qpzqr\t1/1\tRunning\t0\t9h";
        doReturn(runningPod).when(client).executeCommand(any());
        assertDoesNotThrow(() -> podStatusValidator.run(TIMEOUT, STATUS_CHECK_INTERVAL, chart2));
    }
}
