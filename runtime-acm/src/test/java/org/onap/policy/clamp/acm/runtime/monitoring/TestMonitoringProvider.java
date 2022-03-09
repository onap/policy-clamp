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

package org.onap.policy.clamp.acm.runtime.monitoring;

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
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatisticsList;
import org.onap.policy.clamp.models.acm.concepts.AutomationComposition;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.models.acm.persistence.provider.AcElementStatisticsProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class TestMonitoringProvider {

    private static final String AC_PARTICIPANT_STATISTICS_JSON =
        "src/test/resources/rest/monitoring/TestParticipantStatistics.json";
    private static final String INVALID_PARTICIPANT_JSON_INPUT =
        "src/test/resources/rest/monitoring/TestParticipantStatistics_Invalid.json";
    private static final String AC_ELEMENT_STATISTICS_JSON =
        "src/test/resources/rest/monitoring/TestAcElementStatistics.json";
    private static final String INVALID_AC_ELEMENT_JSON_INPUT =
        "src/test/resources/rest/monitoring/TestAcElementStatistics_Invalid.json";
    private static final Coder CODER = new StandardCoder();

    private static final String STAT_LIST_IS_NULL = ".*StatisticsList is marked .*ull but is null";
    private static final String PARTICIPANT_STAT_LIST_IS_NULL =
        "participantStatisticsList is marked .*null but is null";
    private static final String NAME_IS_NULL = "name is marked .*null but is null";
    private static final String AC_LIST_IS_NULL = "acElementStatisticsList is marked .*null but is null";
    private static final String ID_VERSION1 = "1.001";
    private static final String ID_VERSION2 = "1.002";
    private static final String ID_NAME1 = "name1";
    private static final String ID_NAME2 = "name2";
    private static final String SORT_DESC = "DESC";
    private static final String ID_NAME3 = "testACName";
    private static final String ID_INVALID_NAME = "invalidACName";
    private static ParticipantStatisticsList inputParticipantStatistics;
    private static ParticipantStatisticsList invalidParticipantInput;
    private static AcElementStatisticsList inputAcElementStatistics;
    private static AcElementStatisticsList invalidAcElementInput;

    @BeforeAll
    public static void beforeSetupStatistics() throws CoderException {
        // Reading input json for statistics data
        inputParticipantStatistics =
            CODER.decode(new File(AC_PARTICIPANT_STATISTICS_JSON), ParticipantStatisticsList.class);
        invalidParticipantInput =
            CODER.decode(new File(INVALID_PARTICIPANT_JSON_INPUT), ParticipantStatisticsList.class);
        inputAcElementStatistics = CODER.decode(new File(AC_ELEMENT_STATISTICS_JSON), AcElementStatisticsList.class);
        invalidAcElementInput = CODER.decode(new File(INVALID_AC_ELEMENT_JSON_INPUT), AcElementStatisticsList.class);
    }

    @Test
    void testCreateParticipantStatistics() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var acElementStatisticsProvider = mock(AcElementStatisticsProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);
        MonitoringProvider provider =
            new MonitoringProvider(participantStatisticsProvider, acElementStatisticsProvider, acProvider);

        when(participantStatisticsProvider.createParticipantStatistics(any()))
            .thenReturn(inputParticipantStatistics.getStatisticsList());

        when(participantStatisticsProvider.createParticipantStatistics(eq(null)))
            .thenThrow(new PfModelRuntimeException(Response.Status.BAD_REQUEST, PARTICIPANT_STAT_LIST_IS_NULL));

        // Creating statistics data in db with null input

        assertThatThrownBy(() -> provider.createParticipantStatistics(null)).hasMessageMatching(STAT_LIST_IS_NULL);

        assertThatThrownBy(() -> provider.createParticipantStatistics(invalidParticipantInput.getStatisticsList()))
            .hasMessageMatching(PARTICIPANT_STAT_LIST_IS_NULL);

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

        var acProvider = mock(AutomationCompositionProvider.class);
        var acElementStatisticsProvider = mock(AcElementStatisticsProvider.class);
        MonitoringProvider provider =
            new MonitoringProvider(participantStatisticsProvider, acElementStatisticsProvider, acProvider);
        provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

        assertThatThrownBy(() -> provider.fetchFilteredParticipantStatistics(null, null, 0, null, null))
            .hasMessageMatching(NAME_IS_NULL);

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
    void testCreateAcElementStatistics() throws Exception {
        var acElementStatisticsProvider = mock(AcElementStatisticsProvider.class);
        when(acElementStatisticsProvider.createAcElementStatistics(any()))
            .thenReturn(inputAcElementStatistics.getAcElementStatistics());

        when(acElementStatisticsProvider.createAcElementStatistics(eq(null)))
            .thenThrow(new PfModelRuntimeException(Response.Status.BAD_REQUEST, AC_LIST_IS_NULL));

        var acProvider = mock(AutomationCompositionProvider.class);

        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        MonitoringProvider provider =
            new MonitoringProvider(participantStatisticsProvider, acElementStatisticsProvider, acProvider);
        // Creating statistics data in db with null input
        assertThatThrownBy(() -> provider.createAcElementStatistics(null)).hasMessageMatching(STAT_LIST_IS_NULL);

        assertThatThrownBy(() -> provider.createAcElementStatistics(invalidAcElementInput.getAcElementStatistics()))
            .hasMessageMatching(AC_LIST_IS_NULL);

        // Creating acElement statistics data from input json
        AcElementStatisticsList createResponse =
            provider.createAcElementStatistics(inputAcElementStatistics.getAcElementStatistics());

        assertThat(createResponse.getAcElementStatistics()).hasSize(4);
        assertEquals(createResponse.getAcElementStatistics().toString().replaceAll("\\s+", ""),
            inputAcElementStatistics.getAcElementStatistics().toString().replaceAll("\\s+", ""));
    }

    @Test
    void testGetAcElementStatistics() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var acElementStatisticsProvider = mock(AcElementStatisticsProvider.class);
        var acProvider = mock(AutomationCompositionProvider.class);

        when(acElementStatisticsProvider.getFilteredAcElementStatistics(eq(ID_NAME1), any(), any(), any(), anyMap(),
            eq(SORT_DESC), eq(0)))
                .thenReturn(List.of(inputAcElementStatistics.getAcElementStatistics().get(0),
                    inputAcElementStatistics.getAcElementStatistics().get(1)));

        when(acElementStatisticsProvider.getFilteredAcElementStatistics(eq(ID_NAME1), any(), any(), any(), anyMap(),
            eq(SORT_DESC), eq(0)))
                .thenReturn(List.of(inputAcElementStatistics.getAcElementStatistics().get(0),
                    inputAcElementStatistics.getAcElementStatistics().get(1)));

        MonitoringProvider provider =
            new MonitoringProvider(participantStatisticsProvider, acElementStatisticsProvider, acProvider);
        assertThatThrownBy(() -> provider
            .fetchFilteredAcElementStatistics(null, null, null, null, null, 0))
            .hasMessageMatching(NAME_IS_NULL);

        provider.createAcElementStatistics(inputAcElementStatistics.getAcElementStatistics());

        AcElementStatisticsList getResponse =
            provider.fetchFilteredAcElementStatistics(ID_NAME1, null, null, null, null, 0);

        assertThat(getResponse.getAcElementStatistics()).hasSize(2);
        assertEquals(getResponse.getAcElementStatistics().get(0).toString().replaceAll("\\s+", ""),
            inputAcElementStatistics.getAcElementStatistics().get(0).toString().replaceAll("\\s+", ""));

        // Fetch specific statistics record with name, id and record count
        getResponse = provider.fetchFilteredAcElementStatistics(ID_NAME1, ID_VERSION1,
            "709c62b3-8918-41b9-a747-d21eb79c6c20", null, null, 0);
        assertThat(getResponse.getAcElementStatistics()).hasSize(2);

        // Fetch statistics using timestamp
        getResponse = provider.fetchFilteredAcElementStatistics(ID_NAME1, ID_VERSION1, null,
            Instant.parse("2021-01-10T13:45:00.000Z"), null, 0);
        assertThat(getResponse.getAcElementStatistics()).hasSize(2);
    }

    @Test
    void testGetParticipantStatsPerAc() throws Exception {
        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var acElementStatisticsProvider = mock(AcElementStatisticsProvider.class);
        var mockAcProvider = Mockito.mock(AutomationCompositionProvider.class);
        var provider =
            new MonitoringProvider(participantStatisticsProvider, acElementStatisticsProvider, mockAcProvider);

        provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

        var automationComposition = new AutomationComposition();
        var element = new AutomationCompositionElement();
        element.setParticipantId(new ToscaConceptIdentifier(ID_NAME1, ID_VERSION1));
        automationComposition.setElements(Map.of(UUID.randomUUID(), element));
        when(mockAcProvider.findAutomationComposition(new ToscaConceptIdentifier(ID_NAME2, ID_VERSION1)))
            .thenReturn(Optional.of(automationComposition));

        when(participantStatisticsProvider.getFilteredParticipantStatistics(eq(ID_NAME1), eq(ID_VERSION1), any(), any(),
            eq(null), eq(SORT_DESC), eq(0)))
                .thenReturn(List.of(inputParticipantStatistics.getStatisticsList().get(0),
                    inputParticipantStatistics.getStatisticsList().get(1)));

        ParticipantStatisticsList getResponse =
            provider.fetchParticipantStatsPerAutomationComposition(ID_NAME2, ID_VERSION1);
        assertThat(getResponse.getStatisticsList()).hasSize(2);
        assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
            inputParticipantStatistics.getStatisticsList().get(0).toString().replaceAll("\\s+", ""));
        assertThat(
            provider.fetchParticipantStatsPerAutomationComposition(ID_INVALID_NAME, ID_VERSION2).getStatisticsList())
                .isEmpty();
    }

    @Test
    void testAcElementStatsPerAc() throws Exception {
        // Setup a dummy automation composition data
        var mockAcElement = new AutomationCompositionElement();
        mockAcElement.setId(inputAcElementStatistics.getAcElementStatistics().get(0).getId());
        mockAcElement.setParticipantId(new ToscaConceptIdentifier(
            inputAcElementStatistics.getAcElementStatistics().get(0).getParticipantId().getName(),
            inputAcElementStatistics.getAcElementStatistics().get(0).getParticipantId().getVersion()));
        var mockAc = new AutomationComposition();
        mockAc.setElements(new LinkedHashMap<>());
        mockAc.getElements().put(mockAcElement.getId(), mockAcElement);

        var participantStatisticsProvider = mock(ParticipantStatisticsProvider.class);
        var acElementStatisticsProvider = mock(AcElementStatisticsProvider.class);
        var mockAcProvider = Mockito.mock(AutomationCompositionProvider.class);
        var monitoringProvider =
            new MonitoringProvider(participantStatisticsProvider, acElementStatisticsProvider, mockAcProvider);

        // Mock automation composition data to be returned for the given AC Id
        when(mockAcProvider.findAutomationComposition(new ToscaConceptIdentifier(ID_NAME3, ID_VERSION1)))
            .thenReturn(Optional.of(mockAc));

        when(acElementStatisticsProvider.getFilteredAcElementStatistics(eq(ID_NAME1), eq(ID_VERSION1), any(), any(),
            anyMap(), eq(SORT_DESC), eq(0)))
                .thenReturn(List.of(inputAcElementStatistics.getAcElementStatistics().get(0),
                    inputAcElementStatistics.getAcElementStatistics().get(1)));

        monitoringProvider.createAcElementStatistics(inputAcElementStatistics.getAcElementStatistics());

        AcElementStatisticsList getResponse =
            monitoringProvider.fetchAcElementStatsPerAutomationComposition(ID_NAME3, ID_VERSION1);

        assertThat(getResponse.getAcElementStatistics()).hasSize(2);
        assertEquals(getResponse.getAcElementStatistics().get(1).toString().replaceAll("\\s+", ""),
            inputAcElementStatistics.getAcElementStatistics().get(1).toString().replaceAll("\\s+", ""));

        assertThat(monitoringProvider.fetchAcElementStatsPerAutomationComposition(ID_INVALID_NAME, ID_VERSION2)
            .getAcElementStatistics()).isEmpty();

        Map<String, ToscaConceptIdentifier> acElementIds =
            monitoringProvider.getAllAcElementsIdPerAutomationComposition(ID_NAME3, ID_VERSION1);
        assertThat(acElementIds)
            .containsKey(inputAcElementStatistics.getAcElementStatistics().get(0).getId().toString());
    }
}
