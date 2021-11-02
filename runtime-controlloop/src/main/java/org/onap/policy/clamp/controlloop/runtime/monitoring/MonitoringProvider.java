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

package org.onap.policy.clamp.controlloop.runtime.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ClElementStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElement;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ClElementStatisticsProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ControlLoopProvider;
import org.onap.policy.clamp.controlloop.models.controlloop.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Component;

/**
 * This class provides information about statistics data of CL elements and CL Participants in database to callers.
 */
@Component
@AllArgsConstructor
public class MonitoringProvider {

    private static final String DESC_ORDER = "DESC";
    private final ParticipantStatisticsProvider participantStatisticsProvider;
    private final ClElementStatisticsProvider clElementStatisticsProvider;
    private final ControlLoopProvider controlLoopProvider;

    /**
     * Create participant statistics.
     *
     * @param participantStatistics the participant statistics
     * @return the result of create operation
     * @throws PfModelException on creation errors
     */
    public ParticipantStatisticsList createParticipantStatistics(List<ParticipantStatistics> participantStatistics)
            throws PfModelException {
        var participantStatisticsList = new ParticipantStatisticsList();
        participantStatisticsList
                .setStatisticsList(participantStatisticsProvider.createParticipantStatistics(participantStatistics));

        return participantStatisticsList;
    }

    /**
     * Create clElement statistics.
     *
     * @param clElementStatisticsList the clElement statistics
     * @return the result of create operation
     * @throws PfModelException on creation errors
     */
    public ClElementStatisticsList createClElementStatistics(List<ClElementStatistics> clElementStatisticsList)
            throws PfModelException {
        var elementStatisticsList = new ClElementStatisticsList();
        elementStatisticsList
                .setClElementStatistics(clElementStatisticsProvider.createClElementStatistics(clElementStatisticsList));

        return elementStatisticsList;
    }

    /**
     * Get participant statistics based on specific filters.
     *
     * @param name the name of the participant statistics to get, null to get all statistics
     * @param version the version of the participant statistics to get, null to get all statistics
     * @param recordCount number of records to be fetched.
     * @param startTime start of the timestamp, from statistics to be filtered
     * @param endTime end of the timestamp up to which statistics to be filtered
     * @return the participant found
     */
    public ParticipantStatisticsList fetchFilteredParticipantStatistics(@NonNull final String name,
            final String version, int recordCount, Instant startTime, Instant endTime) {
        var participantStatisticsList = new ParticipantStatisticsList();

        // Additional parameters can be added in filterMap for filtering data.
        Map<String, Object> filterMap = null;
        participantStatisticsList.setStatisticsList(participantStatisticsProvider.getFilteredParticipantStatistics(name,
                version, startTime, endTime, filterMap, DESC_ORDER, recordCount));

        return participantStatisticsList;
    }

    /**
     * Get all participant statistics records found for a specific control loop. *
     *
     * @param controlLoopName name of the control loop
     * @param controlLoopVersion version of the control loop
     * @return All the participant statistics found
     * @throws PfModelRuntimeException on errors getting participant statistics
     */
    public ParticipantStatisticsList fetchParticipantStatsPerControlLoop(@NonNull final String controlLoopName,
            @NonNull final String controlLoopVersion) {
        var statisticsList = new ParticipantStatisticsList();
        List<ParticipantStatistics> participantStatistics = new ArrayList<>();
        try {
            // Fetch all participantIds for a specific control loop
            List<ToscaConceptIdentifier> participantIds =
                    getAllParticipantIdsPerControlLoop(controlLoopName, controlLoopVersion);
            for (ToscaConceptIdentifier id : participantIds) {
                participantStatistics.addAll(participantStatisticsProvider.getFilteredParticipantStatistics(
                        id.getName(), id.getVersion(), null, null, null, DESC_ORDER, 0));
            }
            statisticsList.setStatisticsList(participantStatistics);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
        return statisticsList;
    }

    /**
     * Get clElement statistics based on specific filters.
     *
     * @param name the name of the clElement statistics to get, null to get all statistics
     * @param version the version of the clElement statistics to get, null to get all statistics
     * @param id UUID of the control loop element
     * @param startTime start of the timestamp, from statistics to be filtered
     * @param endTime end of the timestamp up to which statistics to be filtered
     * @param recordCount number of records to be fetched.
     * @return the participant found
     * @throws PfModelException on errors getting control loop statistics
     */
    public ClElementStatisticsList fetchFilteredClElementStatistics(@NonNull final String name, final String version,
            final String id, Instant startTime, Instant endTime, int recordCount) throws PfModelException {
        var clElementStatisticsList = new ClElementStatisticsList();
        Map<String, Object> filterMap = new HashMap<>();
        // Adding UUID in filter if present
        if (id != null) {
            filterMap.put("localName", id);
        }
        clElementStatisticsList.setClElementStatistics(clElementStatisticsProvider.getFilteredClElementStatistics(name,
                version, startTime, endTime, filterMap, DESC_ORDER, recordCount));

        return clElementStatisticsList;
    }

    /**
     * Get clElement statistics per control loop.
     *
     * @param name the name of the control loop
     * @param version the version of the control loop
     * @return the clElement statistics found
     * @throws PfModelRuntimeException on errors getting control loop statistics
     */
    public ClElementStatisticsList fetchClElementStatsPerControlLoop(@NonNull final String name,
            @NonNull final String version) {
        var clElementStatisticsList = new ClElementStatisticsList();
        List<ClElementStatistics> clElementStats = new ArrayList<>();
        try {
            List<ControlLoopElement> clElements = new ArrayList<>();
            // Fetch all control loop elements for the control loop
            var controlLoop = controlLoopProvider.getControlLoop(new ToscaConceptIdentifier(name, version));
            if (controlLoop != null) {
                clElements.addAll(controlLoop.getElements().values());
                // Collect control loop element statistics for each cl element.
                for (ControlLoopElement clElement : clElements) {
                    clElementStats.addAll(fetchFilteredClElementStatistics(clElement.getParticipantId().getName(),
                            clElement.getParticipantId().getVersion(), clElement.getId().toString(), null, null, 0)
                                    .getClElementStatistics());
                }
            }
            clElementStatisticsList.setClElementStatistics(clElementStats);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
        return clElementStatisticsList;
    }

    /**
     * If required, REST end point can be defined for this method to fetch associated participant Ids
     * for a control loop.
     *
     * @param name the name of the control loop
     * @param version the version of the control loop
     * @return List of participant Id
     * @throws PfModelException on errors
     */
    public List<ToscaConceptIdentifier> getAllParticipantIdsPerControlLoop(String name, String version)
            throws PfModelException {
        List<ToscaConceptIdentifier> participantIds = new ArrayList<>();
        var controlLoop = controlLoopProvider.getControlLoop(new ToscaConceptIdentifier(name, version));
        if (controlLoop != null) {
            for (ControlLoopElement clElement : controlLoop.getElements().values()) {
                participantIds.add(clElement.getParticipantId());
            }
        }
        return participantIds;
    }

    /**
     * If required, REST end point can be defined for this method to fetch associated control loop element Ids
     * for a control loop.
     *
     * @param name the name of the control loop
     * @param version the version of the control loop
     * @return Map of control loop Id and participant details
     * @throws PfModelException on errors
     */
    public Map<String, ToscaConceptIdentifier> getAllClElementsIdPerControlLoop(String name, String version)
            throws PfModelException {
        Map<String, ToscaConceptIdentifier> clElementId = new HashMap<>();
        var controlLoop = controlLoopProvider.getControlLoop(new ToscaConceptIdentifier(name, version));
        if (controlLoop != null) {
            for (ControlLoopElement clElement : controlLoop.getElements().values()) {
                clElementId.put(clElement.getId().toString(), clElement.getParticipantId());
            }
        }
        return clElementId;
    }
}
