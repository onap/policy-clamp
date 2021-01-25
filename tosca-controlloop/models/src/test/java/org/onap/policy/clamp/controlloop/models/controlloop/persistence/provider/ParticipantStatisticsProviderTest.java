/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
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

package org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatisticsList;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

public class ParticipantStatisticsProviderTest {

    private static final String LIST_IS_NULL = ".*. is marked .*ull but is null";
    private static final Coder CODER = new StandardCoder();
    private static final String PARTICIPANT_STATS_JSON = "src/test/resources/providers/TestParticipantStatistics.json";

    private PolicyModelsProviderParameters parameters;
    private ParticipantStatisticsProvider participantStatisticsProvider;
    private ParticipantStatisticsList inputParticipantStatistics;
    private String originalJson = ResourceUtils.getResourceAsString(PARTICIPANT_STATS_JSON);


    /**
     * Set up test Participant statistics provider.
     */
    @Before
    public void setupDao() throws Exception {

        parameters = new PolicyModelsProviderParameters();
        parameters.setDatabaseDriver("org.h2.Driver");
        parameters.setName("PolicyProviderParameterGroup");
        parameters.setImplementation("org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl");
        parameters.setDatabaseUrl("jdbc:h2:mem:participantStatisticsProviderTestDb");
        parameters.setDatabaseUser("policy");
        parameters.setDatabasePassword("P01icY");
        parameters.setPersistenceUnit("ToscaConceptTest");

        participantStatisticsProvider = new ParticipantStatisticsProvider(parameters);
        inputParticipantStatistics = CODER.decode(originalJson, ParticipantStatisticsList.class);
    }

    @After
    public void teardown() {
        participantStatisticsProvider.close();
    }

    @Test
    public void testParticipantStatisticsCreate() throws Exception {
        assertThatThrownBy(() -> {
            participantStatisticsProvider.createParticipantStatistics(null);
        }).hasMessageMatching(LIST_IS_NULL);

        ParticipantStatisticsList createdStatsList = new ParticipantStatisticsList();
        createdStatsList.setStatisticsList(participantStatisticsProvider
                .createParticipantStatistics(inputParticipantStatistics.getStatisticsList()));

        assertEquals(inputParticipantStatistics.toString().replaceAll("\\s+", ""),
                createdStatsList.toString().replaceAll("\\s+", ""));
    }

    @Test
    public void testGetControlLoops() throws Exception {

        List<ParticipantStatistics> getResponse;

        // Return empty list when no data present in db
        getResponse = participantStatisticsProvider.getParticipantStatistics(null, null, null);
        assertThat(getResponse).isEmpty();

        participantStatisticsProvider.createParticipantStatistics(inputParticipantStatistics.getStatisticsList());
        ToscaConceptIdentifier identifier = inputParticipantStatistics.getStatisticsList().get(0).getParticipantId();
        Date date = inputParticipantStatistics.getStatisticsList().get(0).getTimeStamp();
        assertEquals(1, participantStatisticsProvider
                .getParticipantStatistics(identifier.getName(), identifier.getVersion(), date).size());

        assertEquals(1, participantStatisticsProvider
                .getFilteredParticipantStatistics("name2", "1.0.1", null, null, null, "DESC", 1).size());
    }
}
