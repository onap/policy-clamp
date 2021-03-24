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
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoop;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.runtime.util.CommonTestData;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;


public class TestMonitoringProvider {

    private static final String CL_PARTICIPANT_STATISTICS_JSON =
        "src/test/resources/rest/monitoring/TestParticipantStatistics.json";
    private static final String INVALID_PARTICIPANT_JSON_INPUT =
        "src/test/resources/rest/monitoring/TestParticipantStatistics_Invalid.json";
    private static final String CL_ELEMENT_STATISTICS_JSON =
        "src/test/resources/rest/monitoring/TestClElementStatistics.json";
    private static final String INVALID_CL_ELEMENT_JSON_INPUT =
        "src/test/resources/rest/monitoring/TestClElementStatistics_Invalid.json";
    private static final Coder CODER = new StandardCoder();

    private static final String CL_PROVIDER_FIELD = "controlLoopProvider";

    private static final String LIST_IS_NULL = ".*StatisticsList is marked .*ull but is null";
    private static ParticipantStatisticsList inputParticipantStatistics;
    private static ParticipantStatisticsList invalidParticipantInput;
    private static ClElementStatisticsList inputClElementStatistics;
    private static ClElementStatisticsList invalidClElementInput;



    @BeforeClass
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
    public void testCreateParticipantStatistics() throws Exception {
        PolicyModelsProviderParameters parameters =
            CommonTestData.geParameterGroup(0, "createparStat").getDatabaseProviderParameters();

        try (MonitoringProvider provider = new MonitoringProvider(parameters)) {
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
        }
    }

    @Test
    public void testGetParticipantStatistics() throws Exception {
        PolicyModelsProviderParameters parameters =
            CommonTestData.geParameterGroup(0, "getparStat").getDatabaseProviderParameters();
        try (MonitoringProvider provider = new MonitoringProvider(parameters)) {
            ParticipantStatisticsList getResponse;

            provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());

            assertThatThrownBy(() -> {
                provider.fetchFilteredParticipantStatistics(null, null, 0, null, null);
            }).hasMessageMatching("name is marked .*null but is null");

            // Fetch specific statistics record with name, version and record count
            getResponse = provider.fetchFilteredParticipantStatistics("name2", "1.001", 1,
                null, null);
            assertThat(getResponse.getStatisticsList()).hasSize(1);
            assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
                inputParticipantStatistics.getStatisticsList().get(2).toString().replaceAll("\\s+", ""));

            // Fetch statistics using timestamp
            getResponse = provider.fetchFilteredParticipantStatistics("name1", "1.001", 0,
                null, Instant.parse("2021-01-10T15:00:00.000Z"));
            assertThat(getResponse.getStatisticsList()).hasSize(1);

            getResponse = provider.fetchFilteredParticipantStatistics("name1", "1.001", 0,
                Instant.parse("2021-01-11T12:00:00.000Z"), Instant.parse("2021-01-11T16:00:00.000Z"));

            assertThat(getResponse.getStatisticsList()).isEmpty();
        }
    }

    @Test
    public void testCreateClElementStatistics() throws Exception {
        PolicyModelsProviderParameters parameters =
            CommonTestData.geParameterGroup(0, "createelemstat").getDatabaseProviderParameters();
        try (MonitoringProvider provider = new MonitoringProvider(parameters)) {
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
        }
    }

    @Test
    public void testGetClElementStatistics() throws Exception {
        PolicyModelsProviderParameters parameters =
            CommonTestData.geParameterGroup(0, "getelemstat").getDatabaseProviderParameters();
        try (MonitoringProvider provider = new MonitoringProvider(parameters)) {
            ClElementStatisticsList getResponse;

            assertThatThrownBy(() -> {
                provider.fetchFilteredClElementStatistics(null, null, null,  null,
                    null, 0);
            }).hasMessageMatching("name is marked .*null but is null");

            ClElementStatisticsList lists = provider.createClElementStatistics(inputClElementStatistics
                .getClElementStatistics());

            getResponse = provider.fetchFilteredClElementStatistics("name1", null, null, null,
                null, 0);

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
        }
    }

    @Test
    public void testGetParticipantStatsPerCL() throws Exception {
        PolicyModelsProviderParameters parameters =
            CommonTestData.geParameterGroup(0, "getparStatCL").getDatabaseProviderParameters();
        try (MonitoringProvider provider = Mockito.spy(new MonitoringProvider(parameters))) {

            provider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());
            //Mock the response for fetching participant conceptIdentifiers per control loop
            List<ToscaConceptIdentifier> conceptIdentifiers = new ArrayList<>();
            conceptIdentifiers.add(new ToscaConceptIdentifier("name1", "1.001"));
            when(provider.getAllParticipantIdsPerControlLoop("testName", "1.001"))
                .thenReturn(conceptIdentifiers);
            ParticipantStatisticsList getResponse;
            getResponse = provider.fetchParticipantStatsPerControlLoop("testName", "1.001");
            assertThat(getResponse.getStatisticsList()).hasSize(2);
            assertEquals(getResponse.getStatisticsList().get(0).toString().replaceAll("\\s+", ""),
                inputParticipantStatistics.getStatisticsList().get(0).toString().replaceAll("\\s+", ""));
            assertThat(provider.fetchParticipantStatsPerControlLoop("invalidCLName", "1.002")
                .getStatisticsList()).isEmpty();
        }

    }

    @Test
    public void testClElementStatsPerCL() throws Exception {
        PolicyModelsProviderParameters parameters =
            CommonTestData.geParameterGroup(0, "getelemstatPerCL").getDatabaseProviderParameters();
        //Setup a dummy Control loop data
        ControlLoopElement mockClElement = new ControlLoopElement();
        mockClElement.setId(inputClElementStatistics.getClElementStatistics().get(0).getId());
        mockClElement.setParticipantId(new ToscaConceptIdentifier(inputClElementStatistics.getClElementStatistics()
            .get(0).getParticipantId().getName(), inputClElementStatistics.getClElementStatistics().get(0)
            .getParticipantId().getVersion()));
        ControlLoop mockCL = new ControlLoop();
        mockCL.setElements(Arrays.asList(mockClElement));

        //Mock controlloop data to be returned for the given CL Id
        ControlLoopProvider mockClProvider = Mockito.mock(ControlLoopProvider.class);
        when(mockClProvider.getControlLoop(new ToscaConceptIdentifier("testCLName", "1.001")))
            .thenReturn(mockCL);

        try (MonitoringProvider monitoringProvider = new MonitoringProvider(parameters)) {
            monitoringProvider.createClElementStatistics(inputClElementStatistics.getClElementStatistics());
            Field controlLoopProviderField = monitoringProvider.getClass().getDeclaredField(CL_PROVIDER_FIELD);
            controlLoopProviderField.setAccessible(true);
            controlLoopProviderField.set(monitoringProvider, mockClProvider);

            ClElementStatisticsList getResponse;
            getResponse = monitoringProvider.fetchClElementStatsPerControlLoop("testCLName", "1.001");

            assertThat(getResponse.getClElementStatistics()).hasSize(2);
            assertEquals(getResponse.getClElementStatistics().get(1).toString().replaceAll("\\s+", ""),
                inputClElementStatistics.getClElementStatistics().get(1).toString().replaceAll("\\s+", ""));

            assertThat(monitoringProvider.fetchClElementStatsPerControlLoop("invalidCLName", "1.002")
                .getClElementStatistics()).isEmpty();

            Map<String, ToscaConceptIdentifier> clElementIds = monitoringProvider
                .getAllClElementsIdPerControlLoop("testCLName", "1.001");
            assertThat(clElementIds).containsKey(inputClElementStatistics.getClElementStatistics().get(0).getId()
                .toString());
        }
    }
}
