/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.runtime.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
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
import org.onap.policy.models.base.PfModelRuntimeException;
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

    private static final String STAT_LIST_IS_NULL = ".*StatisticsList is marked .*ull but is null";
    private static final String PARTICIPANT_STAT_LIST_IS_NULL =
            "participantStatisticsList is marked .*null but is null";
    private static final String NAME_IS_NULL = "name is marked .*null but is null";
    private static final String CL_LIST_IS_NULL = "clElementStatisticsList is marked .*null but is null";
    private static final String ID_VERSION1 = "1.001";
    private static final String ID_VERSION2 = "1.002";
    private static final String ID_NAME1 = "name1";
    private static final String ID_NAME2 = "name2";
    private static final String SORT_DESC = "DESC";
    private static final String ID_NAME3 = "testCLName";
    private static final String ID_INVALID_NAME = "invalidCLName";
    private static ParticipantStatisticsList inputParticipantStatistics;
    private static ParticipantStatisticsList invalidParticipantInput;
    private static ClElementStatisticsList inputClElementStatistics;
    private static ClElementStatisticsList invalidClElementInput;

    private ControlLoopProvider clProvider = null;

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

    @AfterEach
    void close() throws Exception {
        if (clProvider != null) {
            clProvider.close();
        }
    }

    @Test
    void testCreateParticipantStatistics() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup("createparStat");
        clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());
        MonitoringProvider provider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);

        when(participantStatisticsProvider.createParticipantStatistics(any()))
                .thenReturn(inputParticipantStatistics.getStatisticsList());

        when(participantStatisticsProvider.createParticipantStatistics(eq(null)))
                .thenThrow(new PfModelRuntimeException(Response.Status.BAD_REQUEST, PARTICIPANT_STAT_LIST_IS_NULL));

        // Creating statistics data in db with null input

        assertThatThrownBy(() -> {
            provider.createParticipantStatistics(null);
        }).hasMessageMatching(STAT_LIST_IS_NULL);

        assertThatThrownBy(() -> {
            provider.createParticipantStatistics(invalidParticipantInput.getStatisticsList());
        }).hasMessageMatching(PARTICIPANT_STAT_LIST_IS_NULL);

        // Creating statistics data from input json
        ParticipantStatisticsList createResponse =
                provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

        assertThat(createResponse.getStatisticsList()).hasSize(3);
        assertEquals(createResponse.getStatisticsList().toString().replaceAll("\\s+", ""),
                inputParticipantStatistics.getStatisticsList().toString().replaceAll("\\s+", ""));
    }

    @Test
    void testGetParticipantStatistics() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        when(participantStatisticsProvider.getFilteredParticipantStatistics(eq(ID_NAME1), any(), any(), any(), eq(null),
                eq(SORT_DESC), eq(0))).thenReturn(List.of(inputParticipantStatistics.getStatisticsList().get(0)));

        when(participantStatisticsProvider.getFilteredParticipantStatistics(eq(ID_NAME1), any(),
                eq(Instant.parse("2021-01-11T12:00:00.000Z")), eq(Instant.parse("2021-01-11T16:00:00.000Z")), eq(null),
                eq(SORT_DESC), eq(0))).thenReturn(List.of());

        when(participantStatisticsProvider.getFilteredParticipantStatistics(eq(ID_NAME2), any(), any(), any(), eq(null),
                eq(SORT_DESC), eq(1))).thenReturn(List.of(inputParticipantStatistics.getStatisticsList().get(2)));

        ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup("getparStat");
        clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        MonitoringProvider provider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

        assertThatThrownBy(() -> {
            provider.fetchFilteredParticipantStatistics(null, null, 0, null, null);
        }).hasMessageMatching(NAME_IS_NULL);

        // Fetch specific statistics record with name, version and record count
        ParticipantStatisticsList getResponse =
                provider.fetchFilteredParticipantStatistics(ID_NAME2, ID_VERSION1, 1, null, null);
        assertThat(getResponse.getStatisticsList()).hasSize(1);
        assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
                inputParticipantStatistics.getStatisticsList().get(2).toString().replaceAll("\\s+", ""));

        // Fetch statistics using timestamp
        getResponse = provider.fetchFilteredParticipantStatistics(ID_NAME1, ID_VERSION1, 0, null,
                Instant.parse("2021-01-10T15:00:00.000Z"));
        assertThat(getResponse.getStatisticsList()).hasSize(1);

        getResponse = provider.fetchFilteredParticipantStatistics(ID_NAME1, ID_VERSION1, 0,
                Instant.parse("2021-01-11T12:00:00.000Z"), Instant.parse("2021-01-11T16:00:00.000Z"));

        assertThat(getResponse.getStatisticsList()).isEmpty();
    }

    @Test
    void testCreateClElementStatistics() throws Exception {
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        when(clElementStatisticsProvider.createClElementStatistics(any()))
                .thenReturn(inputClElementStatistics.getClElementStatistics());

        when(clElementStatisticsProvider.createClElementStatistics(eq(null)))
                .thenThrow(new PfModelRuntimeException(Response.Status.BAD_REQUEST, CL_LIST_IS_NULL));

        ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup("createelemstat");
        clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());

        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        MonitoringProvider provider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        // Creating statistics data in db with null input
        assertThatThrownBy(() -> {
            provider.createClElementStatistics(null);
        }).hasMessageMatching(STAT_LIST_IS_NULL);

        assertThatThrownBy(() -> {
            provider.createClElementStatistics(invalidClElementInput.getClElementStatistics());
        }).hasMessageMatching(CL_LIST_IS_NULL);

        // Creating clElement statistics data from input json
        ClElementStatisticsList createResponse =
                provider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());

        assertThat(createResponse.getClElementStatistics()).hasSize(4);
        assertEquals(createResponse.getClElementStatistics().toString().replaceAll("\\s+", ""),
                inputClElementStatistics.getClElementStatistics().toString().replaceAll("\\s+", ""));
    }

    @Test
    void testGetClElementStatistics() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        ClRuntimeParameterGroup parameters = CommonTestData.geParameterGroup("getelemstat");
        clProvider = new ControlLoopProvider(parameters.getDatabaseProviderParameters());

        when(clElementStatisticsProvider.getFilteredClElementStatistics(eq(ID_NAME1), any(), any(), any(), anyMap(),
                eq(SORT_DESC), eq(0)))
                        .thenReturn(List.of(inputClElementStatistics.getClElementStatistics().get(0),
                                inputClElementStatistics.getClElementStatistics().get(1)));

        MonitoringProvider provider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, clProvider);
        assertThatThrownBy(() -> {
            provider.fetchFilteredClElementStatistics(null, null, null, null, null, 0);
        }).hasMessageMatching(NAME_IS_NULL);

        provider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());

        ClElementStatisticsList getResponse =
                provider.fetchFilteredClElementStatistics(ID_NAME1, null, null, null, null, 0);

        assertThat(getResponse.getClElementStatistics()).hasSize(2);
        assertEquals(getResponse.getClElementStatistics().get(0).toString().replaceAll("\\s+", ""),
                inputClElementStatistics.getClElementStatistics().get(0).toString().replaceAll("\\s+", ""));

        // Fetch specific statistics record with name, id and record count
        getResponse = provider.fetchFilteredClElementStatistics(ID_NAME1, ID_VERSION1,
                "709c62b3-8918-41b9-a747-d21eb79c6c20", null, null, 0);
        assertThat(getResponse.getClElementStatistics()).hasSize(2);

        // Fetch statistics using timestamp
        getResponse = provider.fetchFilteredClElementStatistics(ID_NAME1, ID_VERSION1, null,
                Instant.parse("2021-01-10T13:45:00.000Z"), null, 0);
        assertThat(getResponse.getClElementStatistics()).hasSize(2);
    }

    @Test
    void testGetParticipantStatsPerCL() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        var mockClProvider = Mockito.mock(ControlLoopProvider.class);
        var provider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, mockClProvider);

        provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

        var controlLoop = new ControlLoop();
        var element = new ControlLoopElement();
        element.setParticipantId(new ToscaConceptIdentifier(ID_NAME1, ID_VERSION1));
        controlLoop.setElements(Map.of(UUID.randomUUID(), element));
        when(mockClProvider.getControlLoop(new ToscaConceptIdentifier(ID_NAME2, ID_VERSION1))).thenReturn(controlLoop);

        when(participantStatisticsProvider.getFilteredParticipantStatistics(eq(ID_NAME1), eq(ID_VERSION1), any(), any(),
                eq(null), eq(SORT_DESC), eq(0)))
                        .thenReturn(List.of(inputParticipantStatistics.getStatisticsList().get(0),
                                inputParticipantStatistics.getStatisticsList().get(1)));

        ParticipantStatisticsList getResponse = provider.fetchParticipantStatsPerControlLoop(ID_NAME2, ID_VERSION1);
        assertThat(getResponse.getStatisticsList()).hasSize(2);
        assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
                inputParticipantStatistics.getStatisticsList().get(0).toString().replaceAll("\\s+", ""));
        assertThat(provider.fetchParticipantStatsPerControlLoop(ID_INVALID_NAME, ID_VERSION2).getStatisticsList())
                .isEmpty();
    }

    @Test
    void testClElementStatsPerCL() throws Exception {
        // Setup a dummy Control loop data
        var mockClElement = new ControlLoopElement();
        mockClElement.setId(inputClElementStatistics.getClElementStatistics().get(0).getId());
        mockClElement.setParticipantId(new ToscaConceptIdentifier(
                inputClElementStatistics.getClElementStatistics().get(0).getParticipantId().getName(),
                inputClElementStatistics.getClElementStatistics().get(0).getParticipantId().getVersion()));
        var mockCL = new ControlLoop();
        mockCL.setElements(new LinkedHashMap<>());
        mockCL.getElements().put(mockClElement.getId(), mockClElement);

        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var clElementStatisticsProvider = mock(ClElementStatisticsProvider.class);
        var mockClProvider = Mockito.mock(ControlLoopProvider.class);
        var monitoringProvider =
                new MonitoringProvider(participantStatisticsProvider, clElementStatisticsProvider, mockClProvider);

        // Mock controlloop data to be returned for the given CL Id
        when(mockClProvider.getControlLoop(new ToscaConceptIdentifier(ID_NAME3, ID_VERSION1))).thenReturn(mockCL);

        when(clElementStatisticsProvider.getFilteredClElementStatistics(eq(ID_NAME1), eq(ID_VERSION1), any(), any(),
                anyMap(), eq(SORT_DESC), eq(0)))
                        .thenReturn(List.of(inputClElementStatistics.getClElementStatistics().get(0),
                                inputClElementStatistics.getClElementStatistics().get(1)));

        monitoringProvider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());

        ClElementStatisticsList getResponse =
                monitoringProvider.fetchClElementStatsPerControlLoop(ID_NAME3, ID_VERSION1);

        assertThat(getResponse.getClElementStatistics()).hasSize(2);
        assertEquals(getResponse.getClElementStatistics().get(1).toString().replaceAll("\\s+", ""),
                inputClElementStatistics.getClElementStatistics().get(1).toString().replaceAll("\\s+", ""));

        assertThat(monitoringProvider.fetchClElementStatsPerControlLoop(ID_INVALID_NAME, ID_VERSION2)
                .getClElementStatistics()).isEmpty();

        Map<String, ToscaConceptIdentifier> clElementIds =
                monitoringProvider.getAllClElementsIdPerControlLoop(ID_NAME3, ID_VERSION1);
        assertThat(clElementIds)
                .containsKey(inputClElementStatistics.getClElementStatistics().get(0).getId().toString());
    }
}
