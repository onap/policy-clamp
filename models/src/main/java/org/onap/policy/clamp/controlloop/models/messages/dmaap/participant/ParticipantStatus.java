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

package org.onap.policy.clamp.controlloop.models.messages.dmaap.participant;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopElementDefinition;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ControlLoopInfo;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantHealthStatus;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantState;
import org.onap.policy.clamp.controlloop.models.controlloop.concepts.ParticipantStatistics;
import org.onap.policy.models.base.PfUtils;
import org.onap.policy.models.tosca.authorative.concepts.ToscaConceptIdentifier;

/**
 * Class to represent the PARTICIPANT_STATUS message that all the participants send to the control loop runtime.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ParticipantStatus extends ParticipantMessage {

    // State and health status of the participant
    private ParticipantState state;
    private ParticipantHealthStatus healthStatus;

    // Participant statistics
    private ParticipantStatistics participantStatistics;

    // A map with Participant ID as its key, and a map of ControlLoopElements as value.
    // Returned in response to ParticipantStatusReq only
    private Map<ToscaConceptIdentifier, Map<ToscaConceptIdentifier, ControlLoopElementDefinition>>
            participantDefinitionUpdateMap = new LinkedHashMap<>();

    // Map of ControlLoopInfo types indexed by ControlLoopId, one entry for each control loop
    private Map<ToscaConceptIdentifier, ControlLoopInfo> controlLoopInfoMap;

    /**
     * Constructor for instantiating ParticipantStatus class with message name.
     *
     */
    public ParticipantStatus() {
        super(ParticipantMessageType.PARTICIPANT_STATUS);
    }

    /**
     * Constructs the object, making a deep copy.
     *
     * @param source source from which to copy
     */
    public ParticipantStatus(final ParticipantStatus source) {
        super(source);

        this.state = source.state;
        this.healthStatus = source.healthStatus;
        this.participantStatistics = (source.participantStatistics == null ? null : new ParticipantStatistics());
        this.participantDefinitionUpdateMap = PfUtils.mapMap(source.participantDefinitionUpdateMap,
                clElementDefinitionMap -> PfUtils.mapMap(clElementDefinitionMap,
                        ControlLoopElementDefinition::new));
        this.controlLoopInfoMap = PfUtils.mapMap(source.controlLoopInfoMap, ControlLoopInfo::new);
    }
}
