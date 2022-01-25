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

package org.onap.policy.clamp.models.acm.persistence.provider;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.persistence.concepts.JpaAcElementStatistics;
import org.onap.policy.clamp.models.acm.persistence.repository.AcElementStatisticsRepository;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfReferenceTimestampKey;
import org.onap.policy.models.dao.PfFilterParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides the provision of information on automation composition element statistics in the database to
 * callers.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
@Service
@Transactional
@AllArgsConstructor
public class AcElementStatisticsProvider {

    private AcElementStatisticsRepository acElementStatisticsRepository;

    /**
     * Creates automation composition element statistics.
     *
     * @param acElementStatisticsList a specification of the statistics to create
     * @return the Element statistics created
     * @throws PfModelException on initiation errors
     */
    public List<AcElementStatistics> createAcElementStatistics(
        @NonNull final List<AcElementStatistics> acElementStatisticsList) throws PfModelException {

        try {
            var jpaAcElementStatisticsList = ProviderUtils.getJpaAndValidateList(acElementStatisticsList,
                JpaAcElementStatistics::new, "automation composition element statistics");

            var jpaAcElementStatisticsSaved = acElementStatisticsRepository.saveAll(jpaAcElementStatisticsList);

            // Return the saved automation composition element statistics
            return asAcElementStatisticsList(jpaAcElementStatisticsSaved);
        } catch (IllegalArgumentException e) {
            throw new PfModelException(Status.BAD_REQUEST, "Error in save automation composition element statistics",
                e);
        }
    }

    /**
     * Convert JPA acElement statistics list to acElement statistics list.
     *
     * @param jpaAcElementStatistics the list to convert
     * @return the acElement statistics list
     */
    private List<AcElementStatistics> asAcElementStatisticsList(List<JpaAcElementStatistics> jpaAcElementStatistics) {
        return jpaAcElementStatistics.stream().map(JpaAcElementStatistics::toAuthorative).collect(Collectors.toList());
    }

    /**
     * Get acElement statistics.
     *
     * @param name the name of the participant
     * @param version version of the participant
     * @param id of the automation composition element
     * @param timestamp timestamp of the statistics
     * @return the acElement statistics found
     */
    @Transactional(readOnly = true)
    public List<AcElementStatistics> getAcElementStatistics(final String name, final String version, final String id,
        final Instant timestamp) {
        if (name != null && version != null && timestamp != null && id != null) {
            return asAcElementStatisticsList(acElementStatisticsRepository
                .findAllById(List.of(new PfReferenceTimestampKey(name, version, id, timestamp))));
        } else if (name != null) {
            return getFilteredAcElementStatistics(name, version, null, null, null, "DESC", 0);
        }
        return asAcElementStatisticsList(acElementStatisticsRepository.findAll());
    }

    /**
     * Get filtered acElement statistics.
     *
     * @param name the acElement name for the statistics to get
     * @param version the acElement version for the statistics to get
     * @param startTimeStamp startTimeStamp to filter statistics
     * @param endTimeStamp endTimeStamp to filter statistics
     * @param sortOrder sortOrder to query database
     * @param getRecordNum Total query count from database
     * @param filterMap the filters to apply to the get operation
     * @return the acElement statistics found
     */
    @Transactional(readOnly = true)
    public List<AcElementStatistics> getFilteredAcElementStatistics(final String name, final String version,
        final Instant startTimeStamp, final Instant endTimeStamp, Map<String, Object> filterMap, final String sortOrder,
        final int getRecordNum) {

        // @formatter:off
        PfFilterParameters filterParams = PfFilterParameters
                .builder()
                .name(name)
                .version(version)
                .startTime(startTimeStamp)
                .endTime(endTimeStamp)
                .filterMap(filterMap)
                .sortOrder(sortOrder)
                .recordNum(getRecordNum)
                .build();
        // @formatter:on
        return asAcElementStatisticsList(
            acElementStatisticsRepository.getFiltered(JpaAcElementStatistics.class, filterParams));
    }
}
