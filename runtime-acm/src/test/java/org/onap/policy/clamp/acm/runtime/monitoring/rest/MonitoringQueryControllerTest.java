/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.acm.runtime.monitoring.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.time.Instant;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.policy.clamp.acm.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.acm.runtime.util.rest.CommonRestController;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatisticsList;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatisticsList;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MonitoringQueryControllerTest extends CommonRestController {

    private static final String AC_PARTICIPANT_STATISTICS_JSON =
            "src/test/resources/rest/monitoring/TestParticipantStatistics.json";
    private static final String AC_ELEMENT_STATISTICS_JSON =
            "src/test/resources/rest/monitoring/TestAcElementStatistics.json";

    private static final Coder CODER = new StandardCoder();

    private static ParticipantStatisticsList inputParticipantStatistics;
    private static AcElementStatisticsList inputAcElementStatistics;

    private static ParticipantStatisticsList participantStatisticsList;
    private static AcElementStatisticsList acElementStatisticsList;

    private static final String AC_ELEMENT_STATS_ENDPOINT = "monitoring/acelement";
    private static final String PARTICIPANT_STATS_ENDPOINT = "monitoring/participant";
    private static final String PARTICIPANT_STATS_PER_AC_ENDPOINT = "monitoring/participants/automationcomposition";
    private static final String AC_ELEMENT_STATS_PER_AC_ENDPOINT = "monitoring/acelements/automationcomposition";

    @Autowired
    private MonitoringProvider monitoringProvider;

    @LocalServerPort
    private int randomServerPort;

    /**
     * starts Main.
     *
     * @throws Exception if an error occurs
     */
    @BeforeAll
    public static void setUpBeforeAll() throws Exception {

        inputParticipantStatistics =
                CODER.decode(new File(AC_PARTICIPANT_STATISTICS_JSON), ParticipantStatisticsList.class);
        inputAcElementStatistics = CODER.decode(new File(AC_ELEMENT_STATISTICS_JSON), AcElementStatisticsList.class);
    }

    @BeforeEach
    public void setUpBeforeEach() throws Exception {
        super.setHttpPrefix(randomServerPort);

        // Insert Participant statistics to DB
        participantStatisticsList =
                monitoringProvider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());
        // Insert AC Element statistics to DB
        acElementStatisticsList =
                monitoringProvider.createAcElementStatistics(inputAcElementStatistics.getAcElementStatistics());
    }

    @Test
    void testQuery_Unauthorized_for_AcElementStats() {
        assertUnauthorizedGet(AC_ELEMENT_STATS_ENDPOINT);
    }

    @Test
    void testQuery_Unauthorized_for_AcParticipantStats() {
        assertUnauthorizedGet(PARTICIPANT_STATS_ENDPOINT);
    }

    @Test
    void testQuery_Unauthorized_for_ParticipantStatsPerAc() {
        assertUnauthorizedGet(PARTICIPANT_STATS_PER_AC_ENDPOINT);
    }

    @Test
    void testQuery_Unauthorized_for_AcElementStatsPerAc() {
        assertUnauthorizedGet(AC_ELEMENT_STATS_PER_AC_ENDPOINT);
    }

    @Test
    void testSwagger_AcStats() {
        super.testSwagger(AC_ELEMENT_STATS_ENDPOINT);
        super.testSwagger(PARTICIPANT_STATS_ENDPOINT);
        super.testSwagger(AC_ELEMENT_STATS_PER_AC_ENDPOINT);
        super.testSwagger(PARTICIPANT_STATS_PER_AC_ENDPOINT);
    }

    @Test
    void testAcElementStatisticsEndpoint() {
        // Filter statistics only based on participant Id and UUID
        Invocation.Builder invokeRequest1 = super.sendRequest(AC_ELEMENT_STATS_ENDPOINT + "?name="
                + acElementStatisticsList.getAcElementStatistics().get(0).getParticipantId().getName() + "&version="
                + acElementStatisticsList.getAcElementStatistics().get(0).getParticipantId().getVersion() + "&id="
                + acElementStatisticsList.getAcElementStatistics().get(0).getId());
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        AcElementStatisticsList result1 = response1.readEntity(AcElementStatisticsList.class);

        assertNotNull(result1);
        assertThat(result1.getAcElementStatistics()).hasSize(2);

        var acElementStat0 = acElementStatisticsList.getAcElementStatistics().get(0);
        for (var acElement : result1.getAcElementStatistics()) {
            assertEquals(acElement.getParticipantId().asConceptKey(), acElementStat0.getParticipantId().asConceptKey());
            assertEquals(acElement.getId(), acElementStat0.getId());
        }

        // Filter statistics based on timestamp
        Invocation.Builder invokeRequest2 = super.sendRequest(AC_ELEMENT_STATS_ENDPOINT + "?name="
                + acElementStatisticsList.getAcElementStatistics().get(1).getParticipantId().getName() + "&version="
                + acElementStatisticsList.getAcElementStatistics().get(1).getParticipantId().getVersion()
                + "&startTime=" + Instant.parse("2021-01-10T13:00:00.000Z") + "&endTime="
                + Instant.parse("2021-01-10T14:00:00.000Z"));
        Response response2 = invokeRequest2.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
        AcElementStatisticsList result2 = response2.readEntity(AcElementStatisticsList.class);

        assertNotNull(result2);
        assertThat(result2.getAcElementStatistics()).hasSize(1);
        assertEquals(result2.getAcElementStatistics().get(0), acElementStat0);
    }

    @Test
    void testAcElementStats_BadRequest() {
        Invocation.Builder invokeRequest1 = super.sendRequest(AC_ELEMENT_STATS_ENDPOINT + "?version=1.0.0");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }

    @Test
    void testParticipantStatisticsEndpoint() {

        // Filter statistics only based on participant Id
        Invocation.Builder invokeRequest1 = super.sendRequest(PARTICIPANT_STATS_ENDPOINT + "?name="
                + participantStatisticsList.getStatisticsList().get(0).getParticipantId().getName() + "&version="
                + participantStatisticsList.getStatisticsList().get(0).getParticipantId().getVersion());
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        ParticipantStatisticsList result1 = response1.readEntity(ParticipantStatisticsList.class);

        assertNotNull(result1);
        assertThat(result1.getStatisticsList()).hasSize(2);
        assertThat(result1.getStatisticsList()).contains(participantStatisticsList.getStatisticsList().get(0));

        // Filter statistics based on timestamp
        Invocation.Builder invokeRequest2 = super.sendRequest(PARTICIPANT_STATS_ENDPOINT + "?name="
                + participantStatisticsList.getStatisticsList().get(1).getParticipantId().getName() + "&version="
                + participantStatisticsList.getStatisticsList().get(1).getParticipantId().getVersion() + "&startTime="
                + Instant.parse("2021-01-10T13:00:00.000Z") + "&endTime=" + Instant.parse("2021-01-10T14:00:00.000Z"));
        Response response2 = invokeRequest2.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
        ParticipantStatisticsList result2 = response2.readEntity(ParticipantStatisticsList.class);

        assertNotNull(result2);
        assertThat(result2.getStatisticsList()).hasSize(1);
        assertEquals(result2.getStatisticsList().get(0), participantStatisticsList.getStatisticsList().get(0));
    }

    @Test
    void testParticipantStats_BadRequest() {
        Invocation.Builder invokeRequest1 = super.sendRequest(PARTICIPANT_STATS_ENDPOINT + "?version=0.0");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }

    @Test
    void testParticipantStatsPerAcEndpoint() {
        Invocation.Builder invokeRequest1 =
                super.sendRequest(PARTICIPANT_STATS_PER_AC_ENDPOINT + "?name=dummyName&version=1.001");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        ParticipantStatisticsList result1 = response1.readEntity(ParticipantStatisticsList.class);
        assertThat(result1.getStatisticsList()).isEmpty();
    }

    @Test
    void testParticipantStatsPerAc_BadRequest() {
        Invocation.Builder invokeRequest1 = super.sendRequest(PARTICIPANT_STATS_PER_AC_ENDPOINT);
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }

    @Test
    void testAcElementStatisticsPerAcEndpoint() {
        Invocation.Builder invokeRequest1 =
                super.sendRequest(AC_ELEMENT_STATS_PER_AC_ENDPOINT + "?name=dummyName&version=1.001");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        AcElementStatisticsList result1 = response1.readEntity(AcElementStatisticsList.class);
        assertThat(result1.getAcElementStatistics()).isEmpty();
    }

    @Test
    void testAcElementStatsPerAc_BadRequest() {
        Invocation.Builder invokeRequest1 = super.sendRequest(AC_ELEMENT_STATS_PER_AC_ENDPOINT);
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }
}
