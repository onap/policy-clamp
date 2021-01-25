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
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.concepts.JpaControlLoopStatistics;
import org.onap.policy.common.parameters.BeanValidationResult;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.base.PfTimestampKey;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.provider.impl.AbstractModelsProvider;

/**
 * This class provides the provision of information on Control loop statistics in the database to callers.
 *
 * @author Ramesh Murugan Iyer (ramesh.murugan.iyer@est.tech)
 */
public class ControlLoopStatisticsProvider extends AbstractModelsProvider {

    /**
     * Create a provider for control loops statistics.
     *
     * @param parameters the parameters for database access
     * @throws PfModelException on initiation errors
     */
    public ControlLoopStatisticsProvider(@NonNull PolicyModelsProviderParameters parameters)
        throws PfModelException {
        super(parameters);
        this.init();
    }

    /**
     * Get Control loop statistics.
     *
     * @param name the name of the Control loop statistics to get, null to get all stats
     * @return the Control loop statistics found
     * @throws PfModelException on errors getting Control loop statistics
     */
    public List<ControlLoopStatistics> getControlLoopStatistics(
        final String name, final String version, final Date timestamp)
        throws PfModelException {

        List<ControlLoopStatistics> controlLoopStatistics = new ArrayList<>();
        if (name != null && version != null && timestamp != null) {
            controlLoopStatistics
                .add(getPfDao().get(JpaControlLoopStatistics.class, new PfTimestampKey(name,
                version, timestamp))
                .toAuthorative());
        } else {
            return asClStatisticsList(getPfDao().getAll(JpaControlLoopStatistics.class));
        }
        return controlLoopStatistics;
    }


    /**
     * Get filtered Control loop statistics.
     *
     * @param name the participant name for the control loop statistics to get
     * @param startTimeStamp startTimeStamp to filter statistics
     * @param endTimeStamp endTimeStamp to filter statistics
     * @param sortOrder sortOrder to query database
     * @param getRecordNum Total query count from database
     * @return the control loop statistics found
     * @throws PfModelException on errors getting policies
     */
    public List<ControlLoopStatistics> getFilteredClStatistics(final String name, final String version,
        final Date startTimeStamp, final Date endTimeStamp, Map<String, Object> filterMap,
        final String sortOrder, final int getRecordNum) {

        return asClStatisticsList(getPfDao().getFiltered(JpaControlLoopStatistics.class, name, version,
            startTimeStamp, endTimeStamp, filterMap, sortOrder, getRecordNum));

    }


    /**
     * Creates CL statistics.
     *
     * @param controlLoopStatisticsList a specification of the CL statistics to create
     * @return the CL statistics created
     * @throws PfModelException on errors creating CL statistics
     */
    public List<ControlLoopStatistics> createClStatistics(
        @NonNull final List<ControlLoopStatistics> controlLoopStatisticsList) throws PfModelException {

        for (ControlLoopStatistics controlLoopStatistics : controlLoopStatisticsList) {
            JpaControlLoopStatistics jpaControlLoopStatistics = new JpaControlLoopStatistics();
            jpaControlLoopStatistics.fromAuthorative(controlLoopStatistics);

            BeanValidationResult validationResult = jpaControlLoopStatistics.validate("control loop statistics");
            if (!validationResult.isValid()) {
                throw new PfModelRuntimeException(Response.Status.BAD_REQUEST, validationResult.getResult());
            }

            getPfDao().create(jpaControlLoopStatistics);
        }

        // Return the created CL statistics
        List<ControlLoopStatistics> controlLoopStatistics = new ArrayList<>(controlLoopStatisticsList.size());

        for (ControlLoopStatistics controlLoopStatisticsItem : controlLoopStatisticsList) {
            JpaControlLoopStatistics jpaControlLoopStatistics =
                getPfDao().get(JpaControlLoopStatistics.class,
                    new PfTimestampKey(controlLoopStatisticsItem.getParticipantId().getName(),
                controlLoopStatisticsItem.getParticipantId().getVersion(),
                        controlLoopStatisticsItem.getParticipantTimeStamp()));
            controlLoopStatistics.add(jpaControlLoopStatistics.toAuthorative());
        }

        return controlLoopStatistics;
    }


    /**
     * Convert JPA Control loop statistics list to Control loop statistics list.
     *
     * @param jpaControlLoopStatisticsList the list to convert
     * @return the Control loop statistics list
     */
    private List<ControlLoopStatistics> asClStatisticsList(List<JpaControlLoopStatistics>
                                                              jpaControlLoopStatisticsList) {
        return jpaControlLoopStatisticsList.stream()
          .map(JpaControlLoopStatistics::toAuthorative).collect(Collectors.toList());
    }
}
