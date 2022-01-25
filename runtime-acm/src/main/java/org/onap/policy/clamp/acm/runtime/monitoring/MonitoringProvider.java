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

package org.onap.policy.clamp.acm.runtime.monitoring;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatistics;
import org.onap.policy.clamp.models.acm.concepts.AcElementStatisticsList;
import org.onap.policy.clamp.models.acm.concepts.AutomationCompositionElement;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatistics;
import org.onap.policy.clamp.models.acm.concepts.ParticipantStatisticsList;
import org.onap.policy.clamp.models.acm.persistence.provider.AcElementStatisticsProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.AutomationCompositionProvider;
import org.onap.policy.clamp.models.acm.persistence.provider.ParticipantStatisticsProvider;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides information about statistics data of Automation Composition elements and Participants in database
 * to callers.
 */
@Service
@Transactional
@AllArgsConstructor
public class MonitoringProvider {

    private static final String DESC_ORDER = "DESC";
    private final ParticipantStatisticsProvider participantStatisticsProvider;
    private final AcElementStatisticsProvider acElementStatisticsProvider;
    private final AutomationCompositionProvider automationCompositionProvider;

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
     * Create AcElement statistics.
     *
     * @param acElementStatisticsList the AcElement statistics
     * @return the result of create operation
     * @throws PfModelException on creation errors
     */
    public AcElementStatisticsList createAcElementStatistics(List<AcElementStatistics> acElementStatisticsList)
        throws PfModelException {
        var elementStatisticsList = new AcElementStatisticsList();
        elementStatisticsList
            .setAcElementStatistics(acElementStatisticsProvider.createAcElementStatistics(acElementStatisticsList));

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
    @Transactional(readOnly = true)
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
     * Get all participant statistics records found for a specific automation composition.
     *
     * @param automationCompositionName name of the automation composition
     * @param automationCompositionVersion version of the automation composition
     * @return All the participant statistics found
     * @throws PfModelRuntimeException on errors getting participant statistics
     */
    @Transactional(readOnly = true)
    public ParticipantStatisticsList fetchParticipantStatsPerAutomationComposition(
        @NonNull final String automationCompositionName, @NonNull final String automationCompositionVersion) {
        var statisticsList = new ParticipantStatisticsList();
        List<ParticipantStatistics> participantStatistics = new ArrayList<>();
        try {
            // Fetch all participantIds for a specific automation composition
            List<ToscaConceptIdentifier> participantIds =
                getAllParticipantIdsPerAutomationComposition(automationCompositionName, automationCompositionVersion);
            for (ToscaConceptIdentifier id : participantIds) {
                participantStatistics.addAll(participantStatisticsProvider
                    .getFilteredParticipantStatistics(id.getName(), id.getVersion(), null, null, null, DESC_ORDER, 0));
            }
            statisticsList.setStatisticsList(participantStatistics);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
        return statisticsList;
    }

    /**
     * Get AcElement statistics based on specific filters.
     *
     * @param name the name of the AcElement statistics to get, null to get all statistics
     * @param version the version of the AcElement statistics to get, null to get all statistics
     * @param id UUID of the automation composition element
     * @param startTime start of the timestamp, from statistics to be filtered
     * @param endTime end of the timestamp up to which statistics to be filtered
     * @param recordCount number of records to be fetched.
     * @return the participant found
     * @throws PfModelException on errors getting automation composition statistics
     */
    @Transactional(readOnly = true)
    public AcElementStatisticsList fetchFilteredAcElementStatistics(@NonNull final String name, final String version,
        final String id, Instant startTime, Instant endTime, int recordCount) throws PfModelException {
        var acElementStatisticsList = new AcElementStatisticsList();
        Map<String, Object> filterMap = new HashMap<>();
        // Adding UUID in filter if present
        if (id != null) {
            filterMap.put("localName", id);
        }
        acElementStatisticsList.setAcElementStatistics(acElementStatisticsProvider.getFilteredAcElementStatistics(name,
            version, startTime, endTime, filterMap, DESC_ORDER, recordCount));

        return acElementStatisticsList;
    }

    /**
     * Get AcElement statistics per automation composition.
     *
     * @param name the name of the automation composition
     * @param version the version of the automation composition
     * @return the AcElement statistics found
     * @throws PfModelRuntimeException on errors getting automation composition statistics
     */
    @Transactional(readOnly = true)
    public AcElementStatisticsList fetchAcElementStatsPerAutomationComposition(@NonNull final String name,
        @NonNull final String version) {
        var acElementStatisticsList = new AcElementStatisticsList();
        List<AcElementStatistics> acElementStats = new ArrayList<>();
        try {
            List<AutomationCompositionElement> acElements = new ArrayList<>();
            // Fetch all automation composition elements for the automation composition
            var automationCompositionOpt =
                automationCompositionProvider.findAutomationComposition(new ToscaConceptIdentifier(name, version));
            if (automationCompositionOpt.isPresent()) {
                acElements.addAll(automationCompositionOpt.get().getElements().values());
                // Collect automation composition element statistics for each acElement.
                for (AutomationCompositionElement acElement : acElements) {
                    acElementStats.addAll(fetchFilteredAcElementStatistics(acElement.getParticipantId().getName(),
                        acElement.getParticipantId().getVersion(), acElement.getId().toString(), null, null, 0)
                            .getAcElementStatistics());
                }
            }
            acElementStatisticsList.setAcElementStatistics(acElementStats);
        } catch (PfModelException e) {
            throw new PfModelRuntimeException(e);
        }
        return acElementStatisticsList;
    }

    /**
     * If required, REST end point can be defined for this method to fetch associated participant Ids
     * for a automation composition.
     *
     * @param name the name of the automation composition
     * @param version the version of the automation composition
     * @return List of participant Id
     * @throws PfModelException on errors
     */
    @Transactional(readOnly = true)
    public List<ToscaConceptIdentifier> getAllParticipantIdsPerAutomationComposition(String name, String version)
        throws PfModelException {
        List<ToscaConceptIdentifier> participantIds = new ArrayList<>();
        var automationCompositionOpt =
            automationCompositionProvider.findAutomationComposition(new ToscaConceptIdentifier(name, version));
        if (automationCompositionOpt.isPresent()) {
            for (AutomationCompositionElement acElement : automationCompositionOpt.get().getElements().values()) {
                participantIds.add(acElement.getParticipantId());
            }
        }
        return participantIds;
    }

    /**
     * If required, REST end point can be defined for this method to fetch associated automation composition element Ids
     * for a automation composition.
     *
     * @param name the name of the automation composition
     * @param version the version of the automation composition
     * @return Map of automation composition Id and participant details
     * @throws PfModelException on errors
     */
    @Transactional(readOnly = true)
    public Map<String, ToscaConceptIdentifier> getAllAcElementsIdPerAutomationComposition(String name, String version)
        throws PfModelException {
        Map<String, ToscaConceptIdentifier> acElementId = new HashMap<>();
        var automationCompositionOpt =
            automationCompositionProvider.findAutomationComposition(new ToscaConceptIdentifier(name, version));
        if (automationCompositionOpt.isPresent()) {
            for (AutomationCompositionElement acElement : automationCompositionOpt.get().getElements().values()) {
                acElementId.put(acElement.getId().toString(), acElement.getParticipantId());
            }
        }
        return acElementId;
    }
}
