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

package org.onap.policy.clamp.controlloop.runtime.monitoring.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.time.Instant;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.controlloop.runtime.monitoring.MonitoringProvider;
import org.onap.policy.clamp.controlloop.runtime.util.rest.CommonRestController;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;

public class MonitoringQueryControllerTest extends CommonRestController {

    private static final String CL_PARTICIPANT_STATISTICS_JSON =
        "src/test/resources/rest/monitoring/TestParticipantStatistics.json";
    private static final String CL_ELEMENT_STATISTICS_JSON =
        "src/test/resources/rest/monitoring/TestClElementStatistics.json";

    private static final Coder CODER = new StandardCoder();

    private static ParticipantStatisticsList inputParticipantStatistics;
    private static ClElementStatisticsList inputClElementStatistics;

    private static ParticipantStatisticsList participantStatisticsList;
    private static  ClElementStatisticsList clElementStatisticsList;

    private static final String CLELEMENT_STATS_ENDPOINT = "monitoring/clelement";
    private static final String PARTICIPANT_STATS_ENDPOINT = "monitoring/participant";
    private static final String PARTICIPANT_STATS_PER_CL_ENDPOINT = "monitoring/participants/controlloop";
    private static final String CLELEMENT_STATS_PER_CL_ENDPOINT = "monitoring/clelements/controlloop";


    /**
     * starts Main.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CommonRestController.setUpBeforeClass("testStatisticsQuery");
        inputParticipantStatistics = CODER.decode(new File(CL_PARTICIPANT_STATISTICS_JSON),
            ParticipantStatisticsList.class);
        inputClElementStatistics = CODER.decode(new File(CL_ELEMENT_STATISTICS_JSON),
            ClElementStatisticsList.class);

        try (MonitoringProvider monitoringProvider = new MonitoringProvider(getParameters())) {
            // Insert Participant statistics to DB
            participantStatisticsList = monitoringProvider.createParticipantStatistics(inputParticipantStatistics
                .getStatisticsList());
            // Insert CL Element statistics to DB
            clElementStatisticsList = monitoringProvider.createClElementStatistics(inputClElementStatistics
                .getClElementStatistics());
        }
    }

    @AfterClass
    public static void teardownAfterClass() {
        CommonRestController.teardownAfterClass();
    }

    @Test
    public void testQuery_Unauthorized_for_ClElementStats() throws Exception {
        assertUnauthorizedGet(CLELEMENT_STATS_ENDPOINT);
    }

    @Test
    public void testQuery_Unauthorized_for_ClParticipantStats() throws Exception {
        assertUnauthorizedGet(PARTICIPANT_STATS_ENDPOINT);
    }

    @Test
    public void testQuery_Unauthorized_for_ParticipantStatsPerCl() throws Exception {
        assertUnauthorizedGet(PARTICIPANT_STATS_PER_CL_ENDPOINT);
    }

    @Test
    public void testQuery_Unauthorized_for_ClElementStatsPerCl() throws Exception {
        assertUnauthorizedGet(CLELEMENT_STATS_PER_CL_ENDPOINT);
    }

    @Test
    public void testSwagger_ClStats() throws Exception {
        super.testSwagger(CLELEMENT_STATS_ENDPOINT);
        super.testSwagger(PARTICIPANT_STATS_ENDPOINT);
        super.testSwagger(CLELEMENT_STATS_PER_CL_ENDPOINT);
        super.testSwagger(PARTICIPANT_STATS_PER_CL_ENDPOINT);
    }

    @Test
    public void testClElementStatisticsEndpoint() throws Exception {
        // Filter statistics only based on participant Id and UUID
        Invocation.Builder invokeRequest1 =
            super.sendRequest(CLELEMENT_STATS_ENDPOINT + "?name=" + clElementStatisticsList
                .getClElementStatistics().get(0).getParticipantId().getName() + "&version=" + clElementStatisticsList
                .getClElementStatistics().get(0).getParticipantId().getVersion() + "&id=" + clElementStatisticsList
                .getClElementStatistics().get(0).getId().toString());
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());

        ClElementStatisticsList result1 = response1.readEntity(ClElementStatisticsList.class);

        assertNotNull(result1);
        assertThat(result1.getClElementStatistics()).hasSize(2);
        assertEquals(result1.getClElementStatistics().get(0), clElementStatisticsList
            .getClElementStatistics().get(0));

        // Filter statistics based on timestamp
        Invocation.Builder invokeRequest2 =
            super.sendRequest(CLELEMENT_STATS_ENDPOINT + "?name=" + clElementStatisticsList
                .getClElementStatistics().get(1).getParticipantId().getName() + "&version=" + clElementStatisticsList
                .getClElementStatistics().get(1).getParticipantId().getVersion() + "&startTime="
                + Instant.parse("2021-01-10T13:00:00.000Z") + "&endTime=" + Instant.parse("2021-01-10T14:00:00.000Z"));
        Response response2 = invokeRequest2.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
        ClElementStatisticsList result2 = response2.readEntity(ClElementStatisticsList.class);

        assertNotNull(result2);
        assertThat(result2.getClElementStatistics()).hasSize(1);
        assertEquals(result1.getClElementStatistics().get(0), clElementStatisticsList
            .getClElementStatistics().get(0));
    }

    @Test
    public void testClElementStats_BadRequest() throws Exception {
        Invocation.Builder invokeRequest1 =
            super.sendRequest(CLELEMENT_STATS_ENDPOINT + "?version=1.0.0");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }

    @Test
    public void testParticipantStatisticsEndpoint() throws Exception {

        // Filter statistics only based on participant Id
        Invocation.Builder invokeRequest1 =
            super.sendRequest(PARTICIPANT_STATS_ENDPOINT + "?name=" + participantStatisticsList
                .getStatisticsList().get(0).getParticipantId().getName() + "&version=" + participantStatisticsList
                .getStatisticsList().get(0).getParticipantId().getVersion());
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        ParticipantStatisticsList result1 = response1.readEntity(ParticipantStatisticsList.class);

        assertNotNull(result1);
        assertThat(result1.getStatisticsList()).hasSize(2);
        assertEquals(result1.getStatisticsList().get(0), participantStatisticsList
            .getStatisticsList().get(0));

        // Filter statistics based on timestamp
        Invocation.Builder invokeRequest2 =
            super.sendRequest(PARTICIPANT_STATS_ENDPOINT + "?name=" + participantStatisticsList
                .getStatisticsList().get(1).getParticipantId().getName() + "&version=" + participantStatisticsList
                .getStatisticsList().get(1).getParticipantId().getVersion() + "&startTime="
                + Instant.parse("2021-01-10T13:00:00.000Z") + "&endTime=" + Instant.parse("2021-01-10T14:00:00.000Z"));
        Response response2 = invokeRequest2.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());
        ParticipantStatisticsList result2 = response2.readEntity(ParticipantStatisticsList.class);

        assertNotNull(result2);
        assertThat(result2.getStatisticsList()).hasSize(1);
        assertEquals(result1.getStatisticsList().get(0), participantStatisticsList
            .getStatisticsList().get(0));
    }

    @Test
    public void testParticipantStats_BadRequest() throws Exception {
        Invocation.Builder invokeRequest1 =
            super.sendRequest(PARTICIPANT_STATS_ENDPOINT + "?version=0.0");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }

    @Test
    public void testParticipantStatsPerClEndpoint() throws Exception {
        Invocation.Builder invokeRequest1 =
            super.sendRequest(PARTICIPANT_STATS_PER_CL_ENDPOINT + "?name=dummyName&version=1.001");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        ParticipantStatisticsList result1 = response1.readEntity(ParticipantStatisticsList.class);
        assertThat(result1.getStatisticsList()).isEmpty();
    }

    @Test
    public void testParticipantStatsPerCl_BadRequest() throws Exception {
        Invocation.Builder invokeRequest1 =
            super.sendRequest(PARTICIPANT_STATS_PER_CL_ENDPOINT);
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }

    @Test
    public void testClElementStatisticsPerClEndpoint() throws Exception {
        Invocation.Builder invokeRequest1 =
            super.sendRequest(CLELEMENT_STATS_PER_CL_ENDPOINT + "?name=dummyName&version=1.001");
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.OK.getStatusCode(), response1.getStatus());
        ClElementStatisticsList result1 = response1.readEntity(ClElementStatisticsList.class);
        assertThat(result1.getClElementStatistics()).isEmpty();
    }

    @Test
    public void testClElementStatsPerCl_BadRequest() throws Exception {
        Invocation.Builder invokeRequest1 =
            super.sendRequest(CLELEMENT_STATS_PER_CL_ENDPOINT);
        Response response1 = invokeRequest1.buildGet().invoke();
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response1.getStatus());
    }
}
