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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.repository.ClElementStatisticsRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class ClElementStatisticsProviderTest {
    private static final String LIST_IS_NULL = ".*. is marked .*ull but is null";
    private static final Coder CODER = new StandardCoder();
    private static final String CL_ELEMENT_STATS_JSON = "src/test/resources/providers/TestClElementStatistics.json";

    private ClElementStatisticsProvider clElementStatisticsProvider;
    private ClElementStatisticsList inputClElementStats;
    private String originalJson = ResourceUtils.getResourceAsString(CL_ELEMENT_STATS_JSON);

    /**
     * Set up test ClElement statistics provider.
     *
     * @throws Exception on errors
     */
    @BeforeEach
    void beforeSetupDao() throws Exception {

        inputClElementStats = CODER.decode(originalJson, ClElementStatisticsList.class);
        var clElementStatisticsRepository = mock(ClElementStatisticsRepository.class);

        var jpaClElementStatisticsList =
                ProviderUtils.getJpaAndValidateList(inputClElementStats.getClElementStatistics(),
                        JpaClElementStatistics::new, "control loop element statistics");

        for (var clElementStat : jpaClElementStatisticsList) {
            when(clElementStatisticsRepository.getById(eq(clElementStat.getKey()))).thenReturn(clElementStat);
            when(clElementStatisticsRepository.findAllById(eq(List.of(clElementStat.getKey()))))
                    .thenReturn(List.of(clElementStat));
        }

        when(clElementStatisticsRepository.saveAll(anyList())).thenReturn(jpaClElementStatisticsList);

        when(clElementStatisticsRepository.getFiltered(eq(JpaClElementStatistics.class), any()))
                .thenReturn(List.of(jpaClElementStatisticsList.get(0)));

        clElementStatisticsProvider = new ClElementStatisticsProvider(clElementStatisticsRepository);
    }

    @Test
    void testClElementStatisticsCreate() throws Exception {
        assertThatThrownBy(() -> {
            clElementStatisticsProvider.createClElementStatistics(null);
        }).hasMessageMatching(LIST_IS_NULL);

        ClElementStatisticsList createdClElementStats = new ClElementStatisticsList();
        createdClElementStats.setClElementStatistics(
                clElementStatisticsProvider.createClElementStatistics(inputClElementStats.getClElementStatistics()));

        assertEquals(inputClElementStats.toString().replaceAll("\\s+", ""),
                createdClElementStats.toString().replaceAll("\\s+", ""));
    }

    @Test
    void testGetClElementStatistics() throws Exception {

        List<ClElementStatistics> getResponse;

        // Return empty list when no data present in db
        getResponse = clElementStatisticsProvider.getClElementStatistics(null, null, null, null);
        assertThat(getResponse).isEmpty();

        clElementStatisticsProvider.createClElementStatistics(inputClElementStats.getClElementStatistics());
        ToscaConceptIdentifier identifier = inputClElementStats.getClElementStatistics().get(0).getParticipantId();
        Instant instant = inputClElementStats.getClElementStatistics().get(0).getTimeStamp();
        String id = inputClElementStats.getClElementStatistics().get(0).getId().toString();
        assertEquals(1, clElementStatisticsProvider
                .getClElementStatistics(identifier.getName(), identifier.getVersion(), id, instant).size());

        assertEquals(1, clElementStatisticsProvider
                .getFilteredClElementStatistics("name2", "1.0.1", null, null, null, "DESC", 1).size());
    }
}
