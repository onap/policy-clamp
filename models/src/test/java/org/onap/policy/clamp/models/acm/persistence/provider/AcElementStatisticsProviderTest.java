/*-
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021-2022 Nordix Foundation.
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

package org.onap.policy.clamp.models.acm.persistence.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatisticsList;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAcElementStatistics;
import org.onap.policy.clamp.models.acm.persistence.repository.AcElementStatisticsRepository;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.common.utils.resources.ResourceUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

class AcElementStatisticsProviderTest {
    private static final String LIST_IS_NULL = ".*. is marked .*ull but is null";
    private static final Coder CODER = new StandardCoder();
    private static final String AC_ELEMENT_STATS_JSON = "src/test/resources/providers/TestAcElementStatistics.json";

    private AcElementStatisticsProvider acElementStatisticsProvider;
    private AcElementStatisticsList inputAcElementStats;
    private final String originalJson = ResourceUtils.getResourceAsString(AC_ELEMENT_STATS_JSON);

    /**
     * Set up test AcElement statistics provider.
     *
     * @throws Exception on errors
     */
    @BeforeEach
    void beforeSetupDao() throws Exception {

        inputAcElementStats = CODER.decode(originalJson, AcElementStatisticsList.class);
        var acElementStatisticsRepository = mock(AcElementStatisticsRepository.class);

        var jpaAcElementStatisticsList =
            ProviderUtils.getJpaAndValidateList(inputAcElementStats.getAcElementStatistics(),
                JpaAcElementStatistics::new, "automation composition element statistics");

        for (var acElementStat : jpaAcElementStatisticsList) {
            when(acElementStatisticsRepository.getById(eq(acElementStat.getKey()))).thenReturn(acElementStat);
            when(acElementStatisticsRepository.findAllById(eq(List.of(acElementStat.getKey()))))
                .thenReturn(List.of(acElementStat));
        }

        when(acElementStatisticsRepository.saveAll(anyList())).thenReturn(jpaAcElementStatisticsList);

        when(acElementStatisticsRepository.getFiltered(eq(JpaAcElementStatistics.class), any()))
            .thenReturn(List.of(jpaAcElementStatisticsList.get(0)));

        acElementStatisticsProvider = new AcElementStatisticsProvider(acElementStatisticsRepository);
    }

    @Test
    void testAcElementStatisticsCreate() throws Exception {
        assertThatThrownBy(() -> acElementStatisticsProvider.createAcElementStatistics(null))
            .hasMessageMatching(LIST_IS_NULL);

        AcElementStatisticsList createdAcElementStats = new AcElementStatisticsList();
        createdAcElementStats.setAcElementStatistics(
            acElementStatisticsProvider.createAcElementStatistics(inputAcElementStats.getAcElementStatistics()));

        assertEquals(inputAcElementStats.toString().replaceAll("\\s+", ""),
            createdAcElementStats.toString().replaceAll("\\s+", ""));
    }

    @Test
    void testGetAcElementStatistics() throws Exception {

        List<AcElementStatistics> getResponse;

        // Return empty list when no data present in db
        getResponse = acElementStatisticsProvider.getAcElementStatistics(null, null, null, null);
        assertThat(getResponse).isEmpty();

        acElementStatisticsProvider.createAcElementStatistics(inputAcElementStats.getAcElementStatistics());
        ToscaConceptIdentifier identifier = inputAcElementStats.getAcElementStatistics().get(0).getParticipantId();
        Instant instant = inputAcElementStats.getAcElementStatistics().get(0).getTimeStamp();
        String id = inputAcElementStats.getAcElementStatistics().get(0).getId().toString();
        assertEquals(1, acElementStatisticsProvider
            .getAcElementStatistics(identifier.getName(), identifier.getVersion(), id, instant).size());

        assertEquals(1, acElementStatisticsProvider
            .getFilteredAcElementStatistics("name2", "1.0.1", null, null, null, "DESC", 1).size());
    }
}
