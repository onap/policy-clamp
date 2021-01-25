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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.NonNull;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaParticipantStatistics;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.AbstractModelsProvider;

/**
 * This class provides the provision of information on participant statistics in the database to callers.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
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
            final Date timestamp) throws PfModelException {

        if (name != null && version != null && timestamp != null) {
            List<ParticipantStatistics> participantStatistics = new ArrayList<>(1);
            participantStatistics.add(getPfDao()
                    .get(JpaParticipantStatistics.class, new PfTimestampKey(name, version, timestamp)).toAuthorative());
            return participantStatistics;
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
            final Date startTimeStamp, final Date endTimeStamp, Map<String, Object> filterMap, final String sortOrder,
            final int getRecordNum) {

        return asParticipantStatisticsList(getPfDao().getFiltered(JpaParticipantStatistics.class, name, version,
                startTimeStamp, endTimeStamp, filterMap, sortOrder, getRecordNum));
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

        BeanValidationResult validationResult =
                new BeanValidationResult("participant statistics List", participantStatisticsList);

        for (ParticipantStatistics participantStatistics : participantStatisticsList) {
            JpaParticipantStatistics jpaParticipantStatistics = new JpaParticipantStatistics();
            jpaParticipantStatistics.fromAuthorative(participantStatistics);

            validationResult.addResult(jpaParticipantStatistics.validate("participant statistics"));
        }

        if (!validationResult.isValid()) {
            throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
        }

        for (ParticipantStatistics participantStatistics : participantStatisticsList) {
            JpaParticipantStatistics jpaParticipantStatistics = new JpaParticipantStatistics();
            jpaParticipantStatistics.fromAuthorative(participantStatistics);

            getPfDao().create(jpaParticipantStatistics);
        }

        // Return the created participant statistics
        List<ParticipantStatistics> participantStatistics = new ArrayList<>(participantStatisticsList.size());

        for (ParticipantStatistics participantStatisticsItem : participantStatisticsList) {
            JpaParticipantStatistics jpaParticipantStatistics = getPfDao().get(JpaParticipantStatistics.class,
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
