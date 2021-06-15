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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaParticipantStatistics;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.dao.PfFilterParameters;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.AbstractModelsProvider;

/**
 * This class provides the provision of information on participant statistics in the database to callers.
 */
public class ParticipantStatisticsProvider extends AbstractModelsProvider {

    /**
     * Create a provider for control loops statistics.
     *
     * @param parameters the parameters for database access
     * @throws PfModelException on initiation errors
     */
    public ParticipantStatisticsProvider(@NonNull PolicyModelsProviderParameters parameters) throws PfModelException {
        super(parameters);
        this.init();
    }

    /**
     * Get Participant statistics.
     *
     * @param name the name of the participant statistics to get, null to get all stats
     * @return the participant statistics found
     * @throws PfModelException on errors getting participant statistics
     */
    public List<ParticipantStatistics> getParticipantStatistics(final String name, final String version,
            final Instant timestamp) throws PfModelException {

        if (name != null && version != null && timestamp != null) {
            List<ParticipantStatistics> participantStatistics = new ArrayList<>(1);
            participantStatistics.add(getPfDao()
                    .get(JpaParticipantStatistics.class, new PfTimestampKey(name, version, timestamp)).toAuthorative());
            return participantStatistics;
        } else if (name != null) {
            return getFilteredParticipantStatistics(name, version, timestamp, null, null, "DESC", 0);
        } else {
            return asParticipantStatisticsList(getPfDao().getAll(JpaParticipantStatistics.class));
        }
    }

    /**
     * Get filtered participant statistics.
     *
     * @param name the participant name for the statistics to get
     * @param startTimeStamp startTimeStamp to filter statistics
     * @param endTimeStamp endTimeStamp to filter statistics
     * @param sortOrder sortOrder to query database
     * @param getRecordNum Total query count from database
     * @return the participant statistics found
     * @throws PfModelException on errors getting policies
     */
    public List<ParticipantStatistics> getFilteredParticipantStatistics(final String name, final String version,
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

        return asParticipantStatisticsList(getPfDao().getFiltered(JpaParticipantStatistics.class, filterParams));
    }

    /**
     * Creates Participant statistics.
     *
     * @param participantStatisticsList a specification of the CL statistics to create
     * @return the participant statistics created
     * @throws PfModelException on errors creating participant statistics
     */
    public List<ParticipantStatistics> createParticipantStatistics(
            @NonNull final List<ParticipantStatistics> participantStatisticsList) throws PfModelException {

        List<JpaParticipantStatistics> jpaParticipantStatisticsList = ProviderUtils
                .getJpaAndValidate(participantStatisticsList, JpaParticipantStatistics::new, "Participant Statistics");

        jpaParticipantStatisticsList.forEach(jpaParticipantStatistics -> getPfDao().update(jpaParticipantStatistics));

        // Return the created participant statistics
        List<ParticipantStatistics> participantStatistics = new ArrayList<>(participantStatisticsList.size());

        for (ParticipantStatistics participantStatisticsItem : participantStatisticsList) {
            var jpaParticipantStatistics = getPfDao().get(JpaParticipantStatistics.class,
                    new PfTimestampKey(participantStatisticsItem.getParticipantId().getName(),
                            participantStatisticsItem.getParticipantId().getVersion(),
                            participantStatisticsItem.getTimeStamp()));
            participantStatistics.add(jpaParticipantStatistics.toAuthorative());
        }

        return participantStatistics;
    }

    /**
     * Convert JPA participant statistics list to participant statistics list.
     *
     * @param jpaParticipantStatisticsList the list to convert
     * @return the participant statistics list
     */
    private List<ParticipantStatistics> asParticipantStatisticsList(
            List<JpaParticipantStatistics> jpaParticipantStatisticsList) {

        return jpaParticipantStatisticsList.stream().map(JpaParticipantStatistics::toAuthorative)
                .collect(Collectors.toList());
    }
}
