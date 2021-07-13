/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ClElementStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.clamp.controlloop.runtime.main.parameters.ClRuntimeParameterGroup;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class TestMonitoringProvider {

    private static final String CL_PARTICIPANT_STATISTICS_JSON =
            "src/test/resources/rest/monitoring/TestParticipantStatistics.json";
    private static final String INVALID_PARTICIPANT_JSON_INPUT =
            "src/test/resources/rest/monitoring/TestParticipantStatistics_Invalid.json";
    private static final String CL_ELEMENT_STATISTICS_JSON =
            "src/test/resources/rest/monitoring/TestClElementStatistics.json";
    private static final String INVALID_CL_ELEMENT_JSON_INPUT =
            "src/test/resources/rest/monitoring/TestClElementStatistics_Invalid.json";
    private static final Coder CODER = new StandardCoder();

    private static final String LIST_IS_NULL = ".*StatisticsList is marked .*ull but is null";
    private static ParticipantStatisticsList inputParticipantStatistics;
    private static ParticipantStatisticsList invalidParticipantInput;
    private static ClElementStatisticsList inputClElementStatistics;
    private static ClElementStatisticsList invalidClElementInput;

    @BeforeAll
    public static void beforeSetupStatistics() throws CoderException {
        // Reading input json for statistics data
        inputParticipantStatistics =
                CODER.decode(new File(CL_PARTICIPANT_STATISTICS_JSON), ParticipantStatisticsList.class);
        invalidParticipantInput =
                CODER.decode(new File(INVALID_PARTICIPANT_JSON_INPUT), ParticipantStatisticsList.class);
        inputClElementStatistics = CODER.decode(new File(CL_ELEMENT_STATISTICS_JSON), ClElementStatisticsList.class);
        invalidClElementInput = CODER.decode(new File(INVALID_CL_ELEMENT_JSON_INPUT), ClElementStatisticsList.class);
    }

    @Test
    void testCreateParticipantStatistics() throws Exception {
        ParticipantStatisticsProvider participantStatisticsProvider = null;
        ClElementStatisticsProvider clElementStatisticsProvider = null;
        ControlLoopProvider clProvider = null;
        try {
            ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup(0, "createparStat");
            participantStatisticsProvider =
                    new ParticipantStatisticsProvider(parameters.getDatabaseProviderParameters());
            clElementStatisticsProvider = new ClElementStatisticsProvider(parameters.getDatabaseProviderParameters());
            clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());
            MonitoringProvider provider =
                    new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
            // Creating statistics data in db with null input
            assertThatThrownBy(() -> {
                provider.createParticipantStatistics(null);
            }).hasMessageMatching(LIST_IS_NULL);

            assertThatThrownBy(() -> {
                provider.createParticipantStatistics(invalidParticipantInput.getStatisticsList());
            }).hasMessageMatching("participantStatisticsList is marked .*null but is null");

            // Creating statistics data from input json
            ParticipantStatisticsList createResponse =
                    provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

            assertThat(createResponse.getStatisticsList()).hasSize(3);
            assertEquals(createResponse.getStatisticsList().toString().replaceAll("\\s+", ""),
                    inputParticipantStatistics.getStatisticsList().toString().replaceAll("\\s+", ""));
        } finally {
            if (participantStatisticsProvider != null) {
                participantStatisticsProvider.close();
            }
            if (clElementStatisticsProvider != null) {
                clElementStatisticsProvider.close();
            }
            if (clProvider != null) {
                clProvider.close();
            }
        }
    }

    @Test
    void testGetParticipantStatistics() throws Exception {
        ParticipantStatisticsProvider participantStatisticsProvider = null;
        ClElementStatisticsProvider clElementStatisticsProvider = null;
        ControlLoopProvider clProvider = null;
        try {
            ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup(0, "getparStat");
            participantStatisticsProvider =
                    new ParticipantStatisticsProvider(parameters.getDatabaseProviderParameters());
            clElementStatisticsProvider = new ClElementStatisticsProvider(parameters.getDatabaseProviderParameters());
            clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());
            MonitoringProvider provider =
                    new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);

            provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

            assertThatThrownBy(() -> {
                provider.fetchFilteredParticipantStatistics(null, null, 0, null, null);
            }).hasMessageMatching("name is marked .*null but is null");

            // Fetch specific statistics record with name, version and record count
            ParticipantStatisticsList getResponse =
                    provider.fetchFilteredParticipantStatistics("name2", "1.001", 1, null, null);
            assertThat(getResponse.getStatisticsList()).hasSize(1);
            assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
                    inputParticipantStatistics.getStatisticsList().get(2).toString().replaceAll("\\s+", ""));

            // Fetch statistics using timestamp
            getResponse = provider.fetchFilteredParticipantStatistics("name1", "1.001", 0, null,
                    Instant.parse("2021-01-10T15:00:00.000Z"));
            assertThat(getResponse.getStatisticsList()).hasSize(1);

            getResponse = provider.fetchFilteredParticipantStatistics("name1", "1.001", 0,
                    Instant.parse("2021-01-11T12:00:00.000Z"), Instant.parse("2021-01-11T16:00:00.000Z"));

            assertThat(getResponse.getStatisticsList()).isEmpty();
        } finally {
            if (participantStatisticsProvider != null) {
                participantStatisticsProvider.close();
            }
            if (clElementStatisticsProvider != null) {
                clElementStatisticsProvider.close();
            }
            if (clProvider != null) {
                clProvider.close();
            }
        }
    }

    @Test
    void testCreateClElementStatistics() throws Exception {
        ParticipantStatisticsProvider participantStatisticsProvider = null;
        ClElementStatisticsProvider clElementStatisticsProvider = null;
        ControlLoopProvider clProvider = null;
        try {
            ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup(0, "createelemstat");
            participantStatisticsProvider =
                    new ParticipantStatisticsProvider(parameters.getDatabaseProviderParameters());
            clElementStatisticsProvider = new ClElementStatisticsProvider(parameters.getDatabaseProviderParameters());
            clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());

            MonitoringProvider provider =
                    new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
            // Creating statistics data in db with null input
            assertThatThrownBy(() -> {
                provider.createClElementStatistics(null);
            }).hasMessageMatching(LIST_IS_NULL);

            assertThatThrownBy(() -> {
                provider.createClElementStatistics(invalidClElementInput.getClElementStatistics());
            }).hasMessageMatching("clElementStatisticsList is marked .*null but is null");

            // Creating clElement statistics data from input json
            ClElementStatisticsList createResponse =
                    provider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());

            assertThat(createResponse.getClElementStatistics()).hasSize(4);
            assertEquals(createResponse.getClElementStatistics().toString().replaceAll("\\s+", ""),
                    inputClElementStatistics.getClElementStatistics().toString().replaceAll("\\s+", ""));
        } finally {
            if (participantStatisticsProvider != null) {
                participantStatisticsProvider.close();
            }
            if (clElementStatisticsProvider != null) {
                clElementStatisticsProvider.close();
            }
            if (clProvider != null) {
                clProvider.close();
            }
        }
    }

    @Test
    void testGetClElementStatistics() throws Exception {
        ParticipantStatisticsProvider participantStatisticsProvider = null;
        ClElementStatisticsProvider clElementStatisticsProvider = null;
        ControlLoopProvider clProvider = null;
        try {
            ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup(0, "getelemstat");
            participantStatisticsProvider =
                    new ParticipantStatisticsProvider(parameters.getDatabaseProviderParameters());
            clElementStatisticsProvider = new ClElementStatisticsProvider(parameters.getDatabaseProviderParameters());
            clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());

            MonitoringProvider provider =
                    new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);

            assertThatThrownBy(() -> {
                provider.fetchFilteredClElementStatistics(null, null, null, null, null, 0);
            }).hasMessageMatching("name is marked .*null but is null");

            provider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());

            ClElementStatisticsList getResponse =
                    provider.fetchFilteredClElementStatistics("name1", null, null, null, null, 0);

            assertThat(getResponse.getClElementStatistics()).hasSize(2);
            assertEquals(getResponse.getClElementStatistics().get(0).toString().replaceAll("\\s+", ""),
                    inputClElementStatistics.getClElementStatistics().get(0).toString().replaceAll("\\s+", ""));

            // Fetch specific statistics record with name, id and record count
            getResponse = provider.fetchFilteredClElementStatistics("name1", "1.001",
                    "709c62b3-8918-41b9-a747-d21eb79c6c20", null, null, 0);
            assertThat(getResponse.getClElementStatistics()).hasSize(2);

            // Fetch statistics using timestamp
            getResponse = provider.fetchFilteredClElementStatistics("name1", "1.001", null,
                    Instant.parse("2021-01-10T13:45:00.000Z"), null, 0);
            assertThat(getResponse.getClElementStatistics()).hasSize(2);
        } finally {
            if (participantStatisticsProvider != null) {
                participantStatisticsProvider.close();
            }
            if (clElementStatisticsProvider != null) {
                clElementStatisticsProvider.close();
            }
            if (clProvider != null) {
                clProvider.close();
            }
        }
    }

    @Test
    void testGetParticipantStatsPerCL() throws Exception {
        ParticipantStatisticsProvider participantStatisticsProvider = null;
        ClElementStatisticsProvider clElementStatisticsProvider = null;

        try {
            ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup(0, "getparStatCL");
            participantStatisticsProvider =
                    new ParticipantStatisticsProvider(parameters.getDatabaseProviderParameters());
            clElementStatisticsProvider = new ClElementStatisticsProvider(parameters.getDatabaseProviderParameters());
            var mockClProvider = Mockito.mock(ControlLoopProvider.class);
            var provider =
                    new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, mockClProvider);

            provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

            var controlLoop = new ControlLoop();
            var element = new ControlLoopElement();
            element.setParticipantId(new ToscaConceptIdentifier("name1", "1.001"));
            controlLoop.setElements(Map.of(UUID.randomUUID(), element));
            when(mockClProvider.getControlLoop(eq(new ToscaConceptIdentifier("testName", "1.001"))))
                    .thenReturn(controlLoop);

            // when(provider.getAllParticipantIdsPerControlLoop("testName", "1.001")).thenReturn(conceptIdentifiers);
            ParticipantStatisticsList getResponse = provider.fetchParticipantStatsPerControlLoop("testName", "1.001");
            assertThat(getResponse.getStatisticsList()).hasSize(2);
            assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
                    inputParticipantStatistics.getStatisticsList().get(0).toString().replaceAll("\\s+", ""));
            assertThat(provider.fetchParticipantStatsPerControlLoop("invalidCLName", "1.002").getStatisticsList())
                    .isEmpty();
        } finally {
            if (participantStatisticsProvider != null) {
                participantStatisticsProvider.close();
            }
            if (clElementStatisticsProvider != null) {
                clElementStatisticsProvider.close();
            }
        }
    }

    @Test
    void testClElementStatsPerCL() throws Exception {
        ParticipantStatisticsProvider participantStatisticsProvider = null;
        ClElementStatisticsProvider clElementStatisticsProvider = null;

        // Setup a dummy Control loop data
        ControlLoopElement mockClElement = new ControlLoopElement();
        mockClElement.setId(inputClElementStatistics.getClElementStatistics().get(0).getId());
        mockClElement.setParticipantId(new ToscaConceptIdentifier(
                inputClElementStatistics.getClElementStatistics().get(0).getParticipantId().getName(),
                inputClElementStatistics.getClElementStatistics().get(0).getParticipantId().getVersion()));
        ControlLoop mockCL = new ControlLoop();
        mockCL.setElements(new LinkedHashMap<>());
        mockCL.getElements().put(mockClElement.getId(), mockClElement);

        try {
            ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup(0, "getelemstatPerCL");
            participantStatisticsProvider =
                    new ParticipantStatisticsProvider(parameters.getDatabaseProviderParameters());
            clElementStatisticsProvider = new ClElementStatisticsProvider(parameters.getDatabaseProviderParameters());
            ControlLoopProvider mockClProvider = Mockito.mock(ControlLoopProvider.class);
            var monitoringProvider =
                    new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, mockClProvider);

            // Mock controlloop data to be returned for the given CL Id
            when(mockClProvider.getControlLoop(new ToscaConceptIdentifier("testCLName", "1.001"))).thenReturn(mockCL);

            monitoringProvider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());

            ClElementStatisticsList getResponse;
            getResponse = monitoringProvider.fetchClElementStatsPerControlLoop("testCLName", "1.001");

            assertThat(getResponse.getClElementStatistics()).hasSize(2);
            assertEquals(getResponse.getClElementStatistics().get(1).toString().replaceAll("\\s+", ""),
                    inputClElementStatistics.getClElementStatistics().get(1).toString().replaceAll("\\s+", ""));

            assertThat(monitoringProvider.fetchClElementStatsPerControlLoop("invalidCLName", "1.002")
                    .getClElementStatistics()).isEmpty();

            Map<String, ToscaConceptIdentifier> clElementIds =
                    monitoringProvider.getAllClElementsIdPerControlLoop("testCLName", "1.001");
            assertThat(clElementIds)
                    .containsKey(inputClElementStatistics.getClElementStatistics().get(0).getId().toString());
        } finally {
            if (participantStatisticsProvider != null) {
                participantStatisticsProvider.close();
            }
            if (clElementStatisticsProvider != null) {
                clElementStatisticsProvider.close();
            }
        }
    }
}
