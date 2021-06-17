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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaClElementStatistics;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfReferenceTimestampKey;
import org.onap.policy.models.dao.PfFilterParameters;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.AbstractModelsProvider;

/**
 * This class provides the provision of information on control loop element statistics in the database to callers.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
public class ClElementStatisticsProvider extends AbstractModelsProvider {

    /**
     * Create a provider for control loop element statistics.
     *
     * @param parameters the parameters for database access
     * @throws PfModelException on initiation errors
     */
    public ClElementStatisticsProvider(@NonNull PolicyModelsProviderParameters parameters) throws PfModelException {
        super(parameters);
        this.init();
    }

    /**
     * Creates control loop element statistics.
     *
     * @param clElementStatisticsList a specification of the CL element statistics to create
     * @return the clElement statistics created
     * @throws PfModelException on errors creating clElement statistics
     */
    public List<ClElementStatistics> createClElementStatistics(
            @NonNull final List<ClElementStatistics> clElementStatisticsList) throws PfModelException {

        List<JpaClElementStatistics> jpaClElementStatisticsList = ProviderUtils.getJpaAndValidate(
                clElementStatisticsList, JpaClElementStatistics::new, "control loop element statistics");

        jpaClElementStatisticsList.forEach(jpaClElementStatistics -> getPfDao().create(jpaClElementStatistics));

        // Return the created control loop element statistics
        List<ClElementStatistics> elementStatistics = new ArrayList<>(clElementStatisticsList.size());

        for (ClElementStatistics clElementStat : clElementStatisticsList) {
            var jpaClElementStatistics = getPfDao().get(JpaClElementStatistics.class,
                    new PfReferenceTimestampKey(clElementStat.getParticipantId().getName(),
                            clElementStat.getParticipantId().getVersion(), clElementStat.getId().toString(),
                            clElementStat.getTimeStamp()));
            elementStatistics.add(jpaClElementStatistics.toAuthorative());
        }

        return elementStatistics;
    }

    /**
     * Convert JPA clElement statistics list to clElement statistics list.
     *
     * @param jpaClElementStatistics the list to convert
     * @return the clElement statistics list
     */
    private List<ClElementStatistics> asClElementStatisticsList(List<JpaClElementStatistics> jpaClElementStatistics) {
        return jpaClElementStatistics.stream().map(JpaClElementStatistics::toAuthorative).collect(Collectors.toList());
    }

    /**
     * Get clElement statistics.
     *
     * @param name the name of the participant
     * @param version version of the participant
     * @param id of the control loop element
     * @param timestamp timestamp of the statistics
     * @return the clElement statistics found
     * @throws PfModelException on errors getting clElement statistics
     */
    public List<ClElementStatistics> getClElementStatistics(final String name, final String version, final String id,
            final Instant timestamp) throws PfModelException {
        List<ClElementStatistics> clElementStatistics = new ArrayList<>(1);
        if (name != null && version != null && timestamp != null && id != null) {
            clElementStatistics.add(getPfDao()
                    .get(JpaClElementStatistics.class, new PfReferenceTimestampKey(name, version, id, timestamp))
                    .toAuthorative());
            return clElementStatistics;
        } else if (name != null) {
            clElementStatistics.addAll(getFilteredClElementStatistics(name, version, null, null, null, "DESC", 0));
        } else {
            clElementStatistics.addAll(asClElementStatisticsList(getPfDao().getAll(JpaClElementStatistics.class)));
        }
        return clElementStatistics;
    }

    /**
     * Get filtered clElement statistics.
     *
     * @param name the clElement name for the statistics to get
     * @param version the clElement version for the statistics to get
     * @param startTimeStamp startTimeStamp to filter statistics
     * @param endTimeStamp endTimeStamp to filter statistics
     * @param sortOrder sortOrder to query database
     * @param getRecordNum Total query count from database
     * @param filterMap the filters to apply to the get operation
     * @return the clElement statistics found
     * @throws PfModelException on errors getting policies
     */
    public List<ClElementStatistics> getFilteredClElementStatistics(final String name, final String version,
            final Instant startTimeStamp, final Instant endTimeStamp, Map<String, Object> filterMap,
            final String sortOrder, final int getRecordNum) {

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
        return asClElementStatisticsList(getPfDao().getFiltered(JpaClElementStatistics.class, filterParams));
    }
}
